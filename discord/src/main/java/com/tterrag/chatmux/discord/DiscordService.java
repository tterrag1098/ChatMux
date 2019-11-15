package com.tterrag.chatmux.discord;

import java.util.regex.Matcher;

import org.pf4j.Extension;

import com.tterrag.chatmux.api.bot.BotInterface;
import com.tterrag.chatmux.api.config.ServiceConfig;
import com.tterrag.chatmux.bridge.AbstractChatService;
import com.tterrag.chatmux.config.SimpleServiceConfig;

import discord4j.core.object.entity.GuildChannel;
import discord4j.core.object.util.Snowflake;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Extension
@Slf4j
public class DiscordService extends AbstractChatService<DiscordMessage, DiscordSource> {

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
    public Mono<BotInterface> getInterface() {
        return Mono.just(new DiscordInterface(getSource(), getData()));
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
