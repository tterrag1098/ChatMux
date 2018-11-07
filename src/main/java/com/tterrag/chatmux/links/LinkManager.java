package com.tterrag.chatmux.links;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.tterrag.chatmux.Main;
import com.tterrag.chatmux.bridge.discord.DiscordRequestHelper;
import com.tterrag.chatmux.bridge.mixer.MixerRequestHelper;
import com.tterrag.chatmux.util.ServiceType;

import lombok.Value;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.util.annotation.NonNull;

public enum LinkManager {
    
    INSTANCE;
    
    @Value
    public static class Link {
        
        Channel<?, ?> from, to;
        
        Disposable subscriber;
        
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
    
    public void addLink(Channel<?, ?> from, Channel<?, ?> to, Disposable subscriber) {
        links.computeIfAbsent(from.getType(), type -> HashMultimap.create()).put(from.getName(), new Link(from, to, subscriber));
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
