package com.tterrag.chatmux.twitch;

import org.pf4j.Extension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tterrag.chatmux.api.config.ServiceConfig;
import com.tterrag.chatmux.bridge.AbstractChatService;
import com.tterrag.chatmux.config.SimpleServiceConfig;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Extension
public class TwitchService extends AbstractChatService<TwitchMessage> {

    public TwitchService() {
        super("twitch");
        instance = this;
    }
    
    @Override
    protected TwitchSource createSource() {
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
