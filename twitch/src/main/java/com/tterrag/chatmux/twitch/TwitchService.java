package com.tterrag.chatmux.twitch;

import org.pf4j.Extension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tterrag.chatmux.bridge.ChatService;
import com.tterrag.chatmux.bridge.ChatSource;
import com.tterrag.chatmux.config.ServiceConfig;
import com.tterrag.chatmux.config.SimpleServiceConfig;
import com.tterrag.chatmux.twitch.irc.IRCEvent;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Extension
public class TwitchService extends ChatService {

    public TwitchService() {
        super("twitch");
        instance = this;
    }
    
    @Override
    protected ChatSource createSource() {
        TwitchRequestHelper helper = new TwitchRequestHelper(new ObjectMapper(), getData().getToken());
        return new TwitchSource(helper);
    }
    
    @Getter
    @Setter(AccessLevel.PRIVATE)
    private TwitchData data = new TwitchData();
    
    @Override
    public ServiceConfig<?> getConfig() {
        return new SimpleServiceConfig<>(TwitchData::new, this::setData);
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
