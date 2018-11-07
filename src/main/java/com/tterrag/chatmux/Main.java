package com.tterrag.chatmux;

import com.tterrag.chatmux.bridge.discord.DiscordCommandHandler;
import com.tterrag.chatmux.config.ConfigData;
import com.tterrag.chatmux.config.ConfigReader;
import com.tterrag.chatmux.links.LinkManager;
import com.tterrag.chatmux.links.WebSocketFactory;
import com.tterrag.chatmux.util.ServiceType;
import com.tterrag.chatmux.websocket.DecoratedGatewayClient;

import discord4j.common.json.UserResponse;
import discord4j.gateway.json.dispatch.GuildCreate.Presence.User;
import discord4j.gateway.json.dispatch.MessageCreate;
import discord4j.gateway.json.dispatch.Ready;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;

@Slf4j
public class Main {
    
    public static ConfigData cfg = new ConfigData();
    
    public static UserResponse botUser = new UserResponse();

    public static void main(String[] args) throws InterruptedException {
        ConfigReader cfgReader = new ConfigReader();
        cfgReader.load();
        cfg = cfgReader.getData();
        
        Hooks.onOperatorDebug();
        
        DecoratedGatewayClient discord = (DecoratedGatewayClient) WebSocketFactory.get(ServiceType.DISCORD).getSocket(null);
        
        final DiscordCommandHandler commands = new DiscordCommandHandler(cfg.getDiscord().getToken());
        
        Flux<UserResponse> loginWait = discord.inbound()
                .ofType(Ready.class)
                .doOnNext(r -> LinkManager.INSTANCE.readLinks())
                .map(e -> e.getUser());
        
        discord.connect().subscribe();
        
        botUser = loginWait.blockFirst(); // TODO can this be better?

        discord.inbound().ofType(MessageCreate.class)
            .doOnError(e -> e.printStackTrace())
            .subscribe(mc -> commands.handle(mc.getChannelId(), mc.getAuthor().getId(), mc.getContent().split("\\s+")), Throwable::printStackTrace);
        
        Thread.sleep(9999999);
    }
}
