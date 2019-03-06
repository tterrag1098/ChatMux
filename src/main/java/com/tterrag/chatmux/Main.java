package com.tterrag.chatmux;

import com.tterrag.chatmux.bridge.ChatService;
import com.tterrag.chatmux.bridge.discord.DiscordCommandHandler;
import com.tterrag.chatmux.config.ConfigData;
import com.tterrag.chatmux.config.ConfigReader;
import com.tterrag.chatmux.links.LinkManager;
import com.tterrag.chatmux.websocket.DecoratedGatewayClient;

import discord4j.common.json.UserResponse;
import discord4j.gateway.json.dispatch.MessageCreate;
import discord4j.gateway.json.dispatch.Ready;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
public class Main {
    
    public static ConfigData cfg = new ConfigData();
    
    public static Mono<UserResponse> botUser = Mono.empty();

    public static void main(String[] args) throws InterruptedException {        
        ConfigReader cfgReader = new ConfigReader();
        cfgReader.load();
        cfg = cfgReader.getData();
        
        Hooks.onOperatorDebug();
        
        DecoratedGatewayClient discord = ChatService.DISCORD.getSource().getClient();
        
        final DiscordCommandHandler commands = new DiscordCommandHandler(cfg.getDiscord().getToken());
        
        botUser = discord.inbound()
                .ofType(Ready.class)
                .publishOn(Schedulers.elastic())
                .doOnNext(r -> LinkManager.INSTANCE.readLinks())
                .map(e -> e.getUser())
                .next()
                .cache();
        
        Mono<Void> commandListener = discord.inbound()
                .ofType(MessageCreate.class)
                .flatMap(mc -> commands.handle(mc.getChannelId(), mc.getAuthor().getId(), mc.getContent().split("\\s+")))
                .doOnError(Throwable::printStackTrace)
                .then();
        
        Mono.when(botUser, commandListener, discord.connect()).block();
    }
}
