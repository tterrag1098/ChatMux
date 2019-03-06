package com.tterrag.chatmux.bridge.discord;

import com.austinv11.servicer.WireService;
import com.tterrag.chatmux.Main;
import com.tterrag.chatmux.bridge.ChatService;

import discord4j.gateway.json.GatewayPayload;
import discord4j.gateway.json.dispatch.Dispatch;
import lombok.Getter;

@WireService(ChatService.class)
public class DiscordService extends ChatService<Dispatch, GatewayPayload<?>> {
    
    private final DiscordRequestHelper helper = new DiscordRequestHelper(Main.cfg.getDiscord().getToken());
    
    @Getter(onMethod = @__({@Override}))
    private final DiscordSource source = new DiscordSource(helper);

    public DiscordService() {
        super("discord");
    }
}
