package com.tterrag.chatmux.twitch;

import org.pf4j.Extension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tterrag.chatmux.Main;
import com.tterrag.chatmux.bridge.ChatService;
import com.tterrag.chatmux.bridge.ChatSource;
import com.tterrag.chatmux.twitch.irc.IRCEvent;

@Extension
public class TwitchService extends ChatService<IRCEvent, String> {

    public TwitchService() {
        super("mixer");
        instance = this;
    }
    
    @Override
    protected ChatSource<IRCEvent, String> createSource() {
        TwitchRequestHelper helper = new TwitchRequestHelper(new ObjectMapper(), Main.cfg.getTwitch().getToken());
        return new TwitchSource(helper);
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
