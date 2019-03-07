package com.tterrag.chatmux.discord;

import org.pf4j.Extension;

import com.tterrag.chatmux.Main;
import com.tterrag.chatmux.bridge.ChatService;
import com.tterrag.chatmux.bridge.ChatSource;
import com.tterrag.chatmux.discord.util.DecoratedGatewayClient;
import com.tterrag.chatmux.links.LinkManager;

import discord4j.common.json.UserResponse;
import discord4j.gateway.json.GatewayPayload;
import discord4j.gateway.json.dispatch.Dispatch;
import discord4j.gateway.json.dispatch.MessageCreate;
import discord4j.gateway.json.dispatch.Ready;
import lombok.Getter;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Extension
public class DiscordService extends ChatService<Dispatch, GatewayPayload<?>> {
    
    @Getter
    public static Mono<UserResponse> botUser = Mono.empty();

    public DiscordService() {
        super("discord");
        instance = this;
    }
    
    private static DiscordService instance;

    public static DiscordService getInstance() {
        DiscordService inst = instance;
        if (inst == null) {
            throw new IllegalStateException("Discord service not initialized");
        }
        return inst;
    }
    
    @Override
    protected ChatSource<Dispatch, GatewayPayload<?>> createSource() {
        DiscordRequestHelper helper = new DiscordRequestHelper(Main.cfg.getDiscord().getToken());
        return new DiscordSource(helper);
    }
    
    @Override
    public Mono<Void> runInterface() {
        DecoratedGatewayClient discord = ((DiscordSource)getSource()).getClient();
        
        final DiscordCommandHandler commands = new DiscordCommandHandler(Main.cfg.getDiscord().getToken());
        
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
        
        return Mono.when(botUser, commandListener, discord.connect());
    }
}
