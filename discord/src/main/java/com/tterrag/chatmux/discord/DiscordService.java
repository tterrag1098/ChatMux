package com.tterrag.chatmux.discord;

import java.util.regex.Matcher;

import org.pf4j.Extension;

import com.tterrag.chatmux.api.config.ServiceConfig;
import com.tterrag.chatmux.bridge.AbstractChatService;
import com.tterrag.chatmux.config.SimpleServiceConfig;
import com.tterrag.chatmux.links.LinkManager;

import discord4j.core.DiscordClient;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.GuildChannel;
import discord4j.core.object.entity.TextChannel;
import discord4j.core.object.entity.User;
import discord4j.core.object.util.Snowflake;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Extension
@Slf4j
public class DiscordService extends AbstractChatService<DiscordMessage, DiscordSource> {
    
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
    protected DiscordSource createSource() {
        return new DiscordSource(getData().getToken());
    }
    
    @Override
    public Mono<Void> runInterface(LinkManager manager) {
        DiscordClient discord = ((DiscordSource)getSource()).getClient();
        
        final DiscordCommandHandler commands = new DiscordCommandHandler(manager);
        
        botUser = discord.getEventDispatcher()
                .on(ReadyEvent.class)
                .publishOn(Schedulers.elastic())
                .doOnNext(r -> manager.readLinks())
                .map(e -> e.getSelf())
                .next()
                .cache();
        
        Mono<Void> commandListener = discord.getEventDispatcher()
                .on(MessageCreateEvent.class)
                .flatMap(mc -> Mono.zip(mc.getMessage().getChannel().cast(TextChannel.class), Mono.justOrEmpty(mc.getMessage().getAuthor()))
                        .flatMap(t -> commands.handle(t.getT1(), t.getT2(), mc.getMessage().getContent().orElse("").split("\\s+"))
                        					  .doOnError(ex -> log.error("Exception handling discord commands:", ex))
                        					  .onErrorResume($ -> Mono.empty())))
                .doOnError(t -> log.error("Exception handling message create", t))
                .then();
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> discord.logout().block()));
        
        return Mono.when(botUser, commandListener, discord.login());
    }
    
    @Override
    public Mono<String> parseChannel(String channel) {
        return Mono.fromSupplier(() -> Long.parseLong(channel))
                .thenReturn(channel)
                .onErrorResume(NumberFormatException.class, t -> Mono.just(DiscordMessage.CHANNEL_MENTION.matcher(channel))
                        .filter(Matcher::matches)
                        .map(m -> m.group(1))
                        .switchIfEmpty(Mono.error(() -> new IllegalArgumentException("ChatChannelImpl must be a mention or ID"))));
    }
    
    @Override
    public Mono<String> prettifyChannel(String channel) {
        return getSource().getClient().getChannelById(Snowflake.of(channel))
                .cast(GuildChannel.class)
                .map(c -> '#' + c.getName());
    }
}
