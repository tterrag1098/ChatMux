package com.tterrag.chatmux.bridge.twitch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tterrag.chatmux.Main;
import com.tterrag.chatmux.bridge.ChatService;
import com.tterrag.chatmux.bridge.twitch.irc.IRCEvent;

import lombok.Getter;

public class TwitchService extends ChatService<IRCEvent, String> {
    
    private final TwitchRequestHelper helper = new TwitchRequestHelper(new ObjectMapper(), Main.cfg.getTwitch().getToken());
    
    @Getter(onMethod = @__({@Override}))
    private final TwitchSource source = new TwitchSource(helper);

    public TwitchService() {
        super("mixer");
        instance = this;
    }
    
    private static TwitchService instance;

    public static TwitchService getInstance() {
        TwitchService inst = instance;
        if (inst == null) {
            throw new IllegalStateException("Factorio service not initialized");
        }
        return inst;
    }
}
