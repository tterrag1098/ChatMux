package com.tterrag.chatmux.bridge.twitch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tterrag.chatmux.Main;
import com.tterrag.chatmux.bridge.twitch.irc.IRCEvent;
import com.tterrag.chatmux.util.Service;

import lombok.Getter;

public class TwitchService extends Service<IRCEvent, String> {
    
    private final TwitchRequestHelper helper = new TwitchRequestHelper(new ObjectMapper(), Main.cfg.getTwitch().getToken());
    
    @Getter(onMethod = @__({@Override}))
    private final TwitchSource source = new TwitchSource(helper);

    public TwitchService() {
        super("mixer");
    }
}
