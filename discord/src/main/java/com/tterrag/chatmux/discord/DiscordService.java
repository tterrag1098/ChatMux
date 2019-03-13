package com.tterrag.chatmux.discord;

import org.pf4j.Extension;

import com.tterrag.chatmux.bridge.ChatService;
import com.tterrag.chatmux.bridge.ChatSource;
import com.tterrag.chatmux.config.ServiceConfig;
import com.tterrag.chatmux.config.SimpleServiceConfig;
import com.tterrag.chatmux.links.LinkManager;

import discord4j.core.DiscordClient;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.TextChannel;
import discord4j.core.object.entity.User;
import discord4j.gateway.json.GatewayPayload;
import discord4j.gateway.json.dispatch.Dispatch;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Extension
public class DiscordService extends ChatService<Dispatch, GatewayPayload<?>> {
    
    @Getter
    public static Mono<User> botUser = Mono.empty();

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
    
    @Getter
    @Setter(AccessLevel.PRIVATE)
    private DiscordData data = new DiscordData();
    
    @Override
    public ServiceConfig<?> getConfig() {
        return new SimpleServiceConfig<>(DiscordData::new, this::setData);
    }
    
    @Override
    protected ChatSource<Dispatch, GatewayPayload<?>> createSource() {
        return new DiscordSource(getData().getToken());
    }
    
    @Override
    public Mono<Void> runInterface() {
        DiscordClient discord = ((DiscordSource)getSource()).getClient();
        
        final DiscordCommandHandler commands = new DiscordCommandHandler();
        
        botUser = discord.getEventDispatcher()
                .on(ReadyEvent.class)
                .publishOn(Schedulers.elastic())
                .doOnNext(r -> LinkManager.INSTANCE.readLinks())
                .map(e -> e.getSelf())
                .next()
                .cache();
        
        Mono<Void> commandListener = discord.getEventDispatcher()
                .on(MessageCreateEvent.class)
                .flatMap(mc -> Mono.zip(mc.getMessage().getChannel().cast(TextChannel.class), Mono.justOrEmpty(mc.getMessage().getAuthor()))
                        .flatMap(t -> commands.handle(t.getT1(), t.getT2(), mc.getMessage().getContent().orElse("").split("\\s+"))))
                .doOnError(Throwable::printStackTrace)
                .then();
        
        return Mono.when(botUser, commandListener, discord.login());
    }
}
