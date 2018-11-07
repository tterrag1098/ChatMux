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
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.type.SimpleType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.Converter;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.tterrag.chatmux.Main;
import com.tterrag.chatmux.bridge.discord.DiscordCommandHandler;
import com.tterrag.chatmux.bridge.discord.DiscordRequestHelper;
import com.tterrag.chatmux.bridge.mixer.MixerRequestHelper;
import com.tterrag.chatmux.util.ServiceType;

import lombok.RequiredArgsConstructor;
import lombok.Value;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.util.annotation.NonNull;

public enum LinkManager {
    
    INSTANCE;
    
    @Value
    @JsonDeserialize(converter = LinkConverter.class)
    @RequiredArgsConstructor
    public static class Link {
        
        Channel<?, ?> from, to;
        
        @JsonIgnore
        Disposable subscriber;
        
        @JsonCreator
        Link(@JsonProperty("from") Channel<?, ?> from, @JsonProperty("to") Channel<?, ?> to) {
            this(from, to, null);
        }
    }
    
    private static class LinkConverter implements Converter<Link, Link> {

        @Override
        public Link convert(Link value) {
            Disposable sub = DiscordCommandHandler.connect(INSTANCE.discordHelper, value.getFrom(), value.getTo());
            return new Link(value.getFrom(), value.getTo(), sub);
        }

        @Override
        public JavaType getInputType(TypeFactory typeFactory) {
            return TypeFactory.defaultInstance().constructType(Link.class);
        }

        @Override
        public JavaType getOutputType(TypeFactory typeFactory) {
            return TypeFactory.defaultInstance().constructType(Link.class);
        }
    }
    
    @Value
    private static class MessageKey {
        
        ServiceType<?, ?> type;
        
        String id;
    }
    
    private final Map<ServiceType<?, ?>, Multimap<String, Link>> links = new HashMap<>();
        
    private final LoadingCache<MessageKey, List<Message>> messageCache = CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.MINUTES).recordStats().build(new CacheLoader<MessageKey, List<Message>>() {
        @Override
        public List<Message> load(@NonNull MessageKey key) throws Exception {
            return new ArrayList<>();
        }
    });
    
    private final DiscordRequestHelper discordHelper = new DiscordRequestHelper(Main.cfg.getDiscord().getToken());
    private final MixerRequestHelper mixerHelper = new MixerRequestHelper(new ObjectMapper(), Main.cfg.getMixer().getClientId(), Main.cfg.getMixer().getToken());
    
    public <I, O> Flux<Message> connect(Channel<I, O> channel) {
        ServiceType<I, O> type = channel.getType();
        if (type == ServiceType.DISCORD) {
            return new ChatSource.Discord(discordHelper).connect(WebSocketFactory.get(ServiceType.DISCORD).getSocket(channel.getName()), channel.getName());
        } else if (type == ServiceType.TWITCH) {
            return new ChatSource.Twitch().connect(WebSocketFactory.get(ServiceType.TWITCH).getSocket(channel.getName()), channel.getName());
        } else if (type == ServiceType.MIXER) {
            return new ChatSource.Mixer(mixerHelper).connect(WebSocketFactory.get(ServiceType.MIXER).getSocket(channel.getName()), channel.getName());
        }
        throw new IllegalArgumentException("Unknown service type");
    }
    
    private void saveLinks() {
        List<Link> allLinks = links.values().stream().flatMap(m -> m.values().stream()).collect(Collectors.toList());
        try {
            new ObjectMapper().writeValue(new File("links.json"), allLinks);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void readLinks() {
        try {
            List<Link> allLinks = new ObjectMapper().readValue(new File("links.json"), new TypeReference<List<Link>>() {});
            allLinks.forEach(this::addLink);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public void addLink(Channel<?, ?> from, Channel<?, ?> to, Disposable subscriber) {
        addLink(new Link(from, to, subscriber));
    }
    
    private void addLink(Link link) {
        links.computeIfAbsent(link.getFrom().getType(), type -> HashMultimap.create()).put(link.getFrom().getName(), link);
        saveLinks();
    }
    
    public void removeLink(Channel<?, ?> from, Channel<?, ?> to) {
        Multimap<String, Link> typeLinks = links.get(from.getType());
        Collection<Link> channelLinks = typeLinks.get(from.getName());
        List<Link> toRemove = channelLinks.stream().filter(c -> c.getTo().equals(to)).collect(Collectors.toList());
        toRemove.forEach(l -> l.getSubscriber().dispose());
        channelLinks.removeAll(toRemove);
        if (channelLinks.isEmpty()) {
            WebSocketFactory.get(from.getType()).disposeSocket(from.getName());
        }
        saveLinks();
    }
    
    public void linkMessage(Message source, Message linked) {
        try {
            messageCache.get(new MessageKey(source.getSource(), source.getChannelId())).add(linked);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
    
    public List<Message> getLinkedMessages(ServiceType<?, ?> type, String id) {
        try {
            return messageCache.get(new MessageKey(type, id));
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}
