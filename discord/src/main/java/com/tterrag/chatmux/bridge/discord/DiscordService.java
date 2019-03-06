package com.tterrag.chatmux.bridge.discord;

import com.austinv11.servicer.WireService;
import com.tterrag.chatmux.Main;
import com.tterrag.chatmux.bridge.ChatService;
import com.tterrag.chatmux.links.LinkManager;
import com.tterrag.chatmux.websocket.DecoratedGatewayClient;

import discord4j.common.json.UserResponse;
import discord4j.gateway.json.GatewayPayload;
import discord4j.gateway.json.dispatch.Dispatch;
import discord4j.gateway.json.dispatch.MessageCreate;
import discord4j.gateway.json.dispatch.Ready;
import lombok.Getter;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@WireService(ChatService.class)
public class DiscordService extends ChatService<Dispatch, GatewayPayload<?>> {
    
    private final DiscordRequestHelper helper = new DiscordRequestHelper(Main.cfg.getDiscord().getToken());
    
    @Getter(onMethod = @__({@Override}))
    private final DiscordSource source = new DiscordSource(helper);
    
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
    public Mono<Void> runInterface() {
        DecoratedGatewayClient discord = getSource().getClient();
        
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
