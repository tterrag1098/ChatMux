package com.tterrag.chatmux.links;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.tterrag.chatmux.api.bridge.ChatChannel;
import com.tterrag.chatmux.api.bridge.ChatMessage;
import com.tterrag.chatmux.api.bridge.ChatService;
import com.tterrag.chatmux.api.wiretap.WiretapPlugin;
import com.tterrag.chatmux.bridge.ChatChannelImpl;

import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.annotation.NonNull;
import reactor.util.annotation.Nullable;

@Slf4j
public class LinkManager {
    
    @Value
    @RequiredArgsConstructor
    public static class Link {
        
        ChatChannelImpl<?> from, to;

        boolean raw;

        @JsonIgnore
        @Nullable
        Disposable subscriber;
                
        @JsonCreator
        Link(@JsonProperty("from") ChatChannelImpl<?> from, @JsonProperty("to") ChatChannelImpl<?> to, @JsonProperty("raw") boolean raw) {
            this(from, to, raw, null);
        }
        
        @Override
        public String toString() {
            return from + " -> " + to + (raw ? " (raw)" : "");
        }
        
        public Mono<String> prettyPrint() {
            return from.getService().prettifyChannel(from.getName())
                    .zipWith(to.getService().prettifyChannel(to.getName()),
                            (fromName, toName) -> from.getService().getName() + "/" + fromName + " -> " + to.getService().getName() + "/" + toName + (raw ? " (raw)" : ""));
        }
    }
    
    @Value
    private static class MessageKey {
        
        ChatService<?> type;
        
        String id;
    }
    
    private final Map<ChatService<?>, Multimap<String, Link>> links = new HashMap<>();
    
    @NonNull
    private final List<WiretapPlugin> callbacks;
        
    private final LoadingCache<MessageKey, List<ChatMessage<?>>> messageCache = CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.MINUTES).recordStats().build(new CacheLoader<MessageKey, List<ChatMessage<?>>>() {
        @Override
        public List<ChatMessage<?>> load(@Nullable MessageKey key) throws Exception {
            return new ArrayList<>();
        }
    });
    
    public LinkManager(Collection<WiretapPlugin> callbacks) {
        this.callbacks = ImmutableList.copyOf(callbacks);
    }

    private void saveLinks() {
        List<Link> allLinks = getLinks();
        try {
            new ObjectMapper().writeValue(new File("links.json"), allLinks);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void readLinks() {
        try {
            File file = new File("links.json");
            if (file.exists()) {
                List<Link> allLinks = new ObjectMapper().readValue(new File("links.json"), new TypeReference<List<Link>>() {});
                allLinks.stream()
                    .map(this::connect)
                    .forEach(this::addLink);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    private Link connect(Link unconnected) {
        return new Link(unconnected.getFrom(), unconnected.getTo(), unconnected.isRaw(),
                connect(unconnected.getFrom(), unconnected.getTo(), unconnected.isRaw())); 
    }
    
    public <M extends ChatMessage<M>> Disposable connect(ChatChannel<M> from, ChatChannel<?> to, boolean raw) {
        return from.connect()
                .flatMap(m -> Flux.fromIterable(callbacks).flatMap(c -> c.onMessage(m, from, to)).then().thenReturn(m))
                .flatMap(m -> to.getService().getSource().send(to.getName(), m, raw))
                .doOnError(t -> log.error("Exception processing message", t))
                .subscribe();
    }
    
    public Link addLink(ChatChannelImpl<?> from, ChatChannelImpl<?> to, boolean raw, Disposable subscriber) {
        Link ret = new Link(from, to, raw, subscriber);
        addLink(ret);
        return ret;
    }
    
    private void addLink(Link link) {
        links.computeIfAbsent(link.getFrom().getService(), type -> HashMultimap.create()).put(link.getFrom().getName(), link);
        saveLinks();
    }
    
    public List<Link> removeLink(ChatChannel<?> from, ChatChannel<?> to) {
        Multimap<String, Link> typeLinks = links.get(from.getService());
        Collection<Link> channelLinks = typeLinks.get(from.getName());
        List<Link> toRemove = channelLinks.stream().filter(c -> c.getTo().equals(to)).collect(Collectors.toList());
        if (toRemove.isEmpty()) {
            return toRemove;
        }
        toRemove.forEach(l -> l.getSubscriber().dispose());
        channelLinks.removeAll(toRemove);
        if (channelLinks.isEmpty()) {
            from.getService().getSource().disconnect(from.getName());
        }
        saveLinks();
        return toRemove;
    }
    
    public List<Link> getLinks() {
        return links.values().stream().flatMap(m -> m.values().stream()).collect(Collectors.toList());
    }
    
    public void linkMessage(ChatMessage<?> source, ChatMessage<?> linked) {
        try {
            messageCache.get(new MessageKey(source.getService(), source.getChannelId())).add(linked);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <M extends ChatMessage<M>> List<ChatMessage<M>> getLinkedMessages(ChatService<M> type, String id) {
        try {
            return (List) messageCache.get(new MessageKey(type, id));
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}
