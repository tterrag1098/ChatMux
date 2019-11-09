package com.tterrag.chatmux.discord.command;

import java.util.Set;

import com.google.common.collect.Sets;
import com.tterrag.chatmux.api.bridge.ChatMessage;
import com.tterrag.chatmux.api.bridge.ChatService;
import com.tterrag.chatmux.api.command.CommandContext;
import com.tterrag.chatmux.api.command.CommandHandler;
import com.tterrag.chatmux.api.command.CommandListener;
import com.tterrag.chatmux.discord.DiscordMessage;
import com.tterrag.chatmux.discord.DiscordService;
import com.tterrag.chatmux.links.LinkManager;

import discord4j.core.DiscordClient;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.TextChannel;
import discord4j.core.object.entity.User;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.annotation.NonNull;

@RequiredArgsConstructor
@Slf4j
public class DiscordCommandHandler implements CommandHandler {
    
    private final DiscordClient client;
    private final LinkManager manager;
    
    @NonNull
    private final Set<CommandListener> listeners;
    
    public DiscordCommandHandler(DiscordClient client, LinkManager manager) {
        this.client = client;
        this.manager = manager;
        this.listeners = Sets.newConcurrentHashSet(Sets.<CommandListener>newHashSet(new DiscordCommandListener(manager)));
    }
    
    @Override
    public Mono<Void> start() {
        Mono<Void> readyListener = client.getEventDispatcher()
                .on(ReadyEvent.class)
                .doOnNext($ -> manager.readLinks())
                .then();
        
        Mono<Void> commandListener = client.getEventDispatcher()
                .on(MessageCreateEvent.class)
                .flatMap(mc -> Mono.zip(mc.getMessage().getChannel().cast(TextChannel.class), Mono.justOrEmpty(mc.getMessage().getAuthor()))
                        .flatMap(t -> runCommand(t.getT1(), t.getT2(), mc.getMessage().getContent().orElse(""))
                                .doOnError(ex -> log.error("Exception handling discord commands:", ex))
                                .onErrorResume($ -> Mono.empty())))
                .doOnError(t -> log.error("Exception handling message create", t))
                .then();

        return Mono.when(readyListener, commandListener, client.login());
    }

    private Mono<Void> runCommand(TextChannel textChannel, User user, String content) {
        int split = content.indexOf(' ');
        String command = split >= 0 ? content.substring(0, split) : content;
        String args = split >= 0 ? content.substring(split).trim() : "";
        return Flux.fromIterable(listeners)
                .flatMap(l -> l.runCommand(command, new Context(args, textChannel.getId().asString(), user.getId().asString(), textChannel)))
                .then();
    }

    @Override
    public void addListener(CommandListener listener) {
        listeners.add(listener);
    }
    
    @Value
    private static class Context implements CommandContext<DiscordMessage> {
        
        @Getter(onMethod = @__({@Override}))
        String args;
        @Getter(onMethod = @__({@Override}))
        String channelId;
        @Getter(onMethod = @__({@Override}))
        String userId;
        
        TextChannel channel;
        
        @Override
        public DiscordService getService() {
            return DiscordService.getInstance();
        }

        @Override
        public Mono<DiscordMessage> reply(String msg) {
            return channel.createMessage(msg).flatMap(DiscordMessage::create);
        }
    }
}
