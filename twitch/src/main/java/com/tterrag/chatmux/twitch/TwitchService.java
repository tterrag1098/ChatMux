package com.tterrag.chatmux.twitch;

import java.util.Locale;

import org.pf4j.Extension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tterrag.chatmux.api.bridge.ChatChannel;
import com.tterrag.chatmux.api.bridge.ChatService;
import com.tterrag.chatmux.api.config.ServiceConfig;
import com.tterrag.chatmux.bridge.AbstractChatService;
import com.tterrag.chatmux.config.SimpleServiceConfig;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import reactor.core.publisher.Mono;

@Extension
public class TwitchService extends AbstractChatService<TwitchMessage, TwitchSource> {

    public TwitchService() {
        super("twitch");
        instance = this;
    }
    
    @Override
    protected TwitchSource createSource() {
        TwitchRequestHelper helper = new TwitchRequestHelper(new ObjectMapper(), getData().getTokenReceive());
        return new TwitchSource(helper);
    }
    
    @Getter
    @Setter(AccessLevel.PRIVATE)
    private TwitchData data = new TwitchData();
    
    @Override
    public ServiceConfig<?> getConfig() {
        return new SimpleServiceConfig<>(TwitchData::new, this::setData);
    }
    
    @Override
    public Mono<String> parseChannel(String channel) {
        return super.parseChannel(channel.toLowerCase(Locale.ROOT));
    }
    
    @Override
    public Mono<String> prettifyChannel(ChatService<?> target, ChatChannel<?> channel) {
        return getSource().getHelper()
                .getUser(channel.getName())
                .map(ur -> channel.getService().getName() + "/" + ur.displayName);
    }
    
    private static TwitchService instance;

    public static TwitchService getInstance() {
        TwitchService inst = instance;
        if (inst == null) {
            throw new IllegalStateException("Twitch service not initialized");
        }
        return inst;
    }
}
