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
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.Converter;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.tterrag.chatmux.bridge.ChatChannel;
import com.tterrag.chatmux.bridge.ChatMessage;
import com.tterrag.chatmux.bridge.ChatService;

import lombok.RequiredArgsConstructor;
import lombok.Value;
import reactor.core.Disposable;
import reactor.util.annotation.Nullable;

public enum LinkManager {
    
    INSTANCE;
    
    @Value
    @JsonDeserialize(converter = LinkConverter.class)
    @RequiredArgsConstructor
    public static class Link {
        
        ChatChannel<?, ?> from, to;

        boolean raw;

        @JsonIgnore
        Disposable subscriber;
                
        @JsonCreator
        Link(@JsonProperty("from") ChatChannel<?, ?> from, @JsonProperty("to") ChatChannel<?, ?> to, @JsonProperty("raw") boolean raw) {
            this(from, to, raw, null);
        }
        
        @Override
        public String toString() {
            return from + " -> " + to + (raw ? " (raw)" : "");
        }
    }
    
    private static class LinkConverter implements Converter<Link, Link> {

        @Override
        public Link convert(@SuppressWarnings("null") Link value) {
            Disposable sub = value.getFrom().connect().doOnNext(m -> value.getTo().getType().getSource().send(value.getTo().getName(), m, value.isRaw())).subscribe();
            if (sub == null) {
                throw new IllegalArgumentException("Connecting to saved link failed");
            }
            return new Link(value.getFrom(), value.getTo(), value.isRaw(), sub);
        }

        @SuppressWarnings("null")
        @Override
        public JavaType getInputType(TypeFactory typeFactory) {
            return TypeFactory.defaultInstance().constructType(Link.class);
        }

        @SuppressWarnings("null")
        @Override
        public JavaType getOutputType(TypeFactory typeFactory) {
            return TypeFactory.defaultInstance().constructType(Link.class);
        }
    }
    
    @Value
    private static class MessageKey {
        
        ChatService<?, ?> type;
        
        String id;
    }
    
    private final Map<ChatService<?, ?>, Multimap<String, Link>> links = new HashMap<>();
        
    private final LoadingCache<MessageKey, List<ChatMessage>> messageCache = CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.MINUTES).recordStats().build(new CacheLoader<MessageKey, List<ChatMessage>>() {
        @Override
        public List<ChatMessage> load(@Nullable MessageKey key) throws Exception {
            return new ArrayList<>();
        }
    });

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
                allLinks.forEach(this::addLink);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public void addLink(ChatChannel<?, ?> from, ChatChannel<?, ?> to, boolean raw, Disposable subscriber) {
        addLink(new Link(from, to, raw, subscriber));
    }
    
    private void addLink(Link link) {
        links.computeIfAbsent(link.getFrom().getType(), type -> HashMultimap.create()).put(link.getFrom().getName(), link);
        saveLinks();
    }
    
    public boolean removeLink(ChatChannel<?, ?> from, ChatChannel<?, ?> to) {
        Multimap<String, Link> typeLinks = links.get(from.getType());
        Collection<Link> channelLinks = typeLinks.get(from.getName());
        List<Link> toRemove = channelLinks.stream().filter(c -> c.getTo().equals(to)).collect(Collectors.toList());
        if (toRemove.isEmpty()) {
            return false;
        }
        toRemove.forEach(l -> l.getSubscriber().dispose());
        channelLinks.removeAll(toRemove);
        if (channelLinks.isEmpty()) {
            from.getType().getSource().disconnect(from.getName());
        }
        saveLinks();
        return true;
    }
    
    public List<Link> getLinks() {
        return links.values().stream().flatMap(m -> m.values().stream()).collect(Collectors.toList());
    }
    
    public void linkMessage(ChatMessage source, ChatMessage linked) {
        try {
            messageCache.get(new MessageKey(source.getSource(), source.getChannelId())).add(linked);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
    
    public List<ChatMessage> getLinkedMessages(ChatService<?, ?> type, String id) {
        try {
            return messageCache.get(new MessageKey(type, id));
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}
