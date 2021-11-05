package com.tterrag.chatmux.discord;

import java.util.Set;
import java.util.stream.Collectors;

import com.tterrag.chatmux.api.bot.BotInterface;
import com.tterrag.chatmux.api.bridge.ChatService;
import com.tterrag.chatmux.api.command.CommandHandler;
import com.tterrag.chatmux.api.link.LinkManager;

import discord4j.common.util.Snowflake;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class DiscordInterface implements BotInterface {
    
    private final DiscordSource source;
    private final DiscordData config;

    @Override
    public Mono<CommandHandler> getCommandHandler(LinkManager manager) {
        return Mono.just(source.getClient())
                .doOnNext(client -> Runtime.getRuntime().addShutdownHook(new Thread(() -> client.logout().block())))
                .thenReturn(source.createCommandHandler(manager));
    }

    @Override
    public ChatService<?> getService() {
        return DiscordService.getInstance();
    }

    @Override
    public Set<String> getAdmins() {
        return config.getAdmins().stream().map(Snowflake::of).map(Snowflake::asString).collect(Collectors.toSet());
    }
}
