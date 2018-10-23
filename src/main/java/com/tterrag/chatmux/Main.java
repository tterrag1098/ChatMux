package com.tterrag.chatmux;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tterrag.chatmux.bridge.discord.DiscordCommandHandler;
import com.tterrag.chatmux.config.ConfigData;
import com.tterrag.chatmux.config.ConfigReader;
import com.tterrag.chatmux.websocket.DecoratedGatewayClient;

import discord4j.gateway.json.dispatch.MessageCreate;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Hooks;

@Slf4j
public class Main {
    
    public static ConfigData cfg = new ConfigData();

    public static void main(String[] args) throws InterruptedException {
        ConfigReader cfgReader = new ConfigReader();
        cfgReader.load();
        cfg = cfgReader.getData();
        
        Hooks.onOperatorDebug();
        
        DecoratedGatewayClient discord = new DecoratedGatewayClient();
        discord.connect().subscribe();
        
        final DiscordCommandHandler commands = new DiscordCommandHandler(discord, cfg.getDiscord().getToken(), new ObjectMapper());

        discord.inbound().ofType(MessageCreate.class)
            .doOnError(e -> e.printStackTrace())
            .subscribe(mc -> commands.handle(mc.getChannelId(), mc.getAuthor().getId(), mc.getContent().split("\\s+")), Throwable::printStackTrace);
        
        Thread.sleep(9999999);
    }
}
