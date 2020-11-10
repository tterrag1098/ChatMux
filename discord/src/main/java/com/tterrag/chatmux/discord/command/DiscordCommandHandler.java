package com.tterrag.chatmux.discord.command;

import java.util.Objects;
import java.util.Set;

import com.google.common.collect.Sets;
import com.tterrag.chatmux.api.bridge.ChatMessage;
import com.tterrag.chatmux.api.bridge.ChatService;
import com.tterrag.chatmux.api.bridge.Connectable;
import com.tterrag.chatmux.api.command.CommandContext;
import com.tterrag.chatmux.api.command.CommandHandler;
import com.tterrag.chatmux.api.command.CommandListener;
import com.tterrag.chatmux.api.link.LinkManager;
import com.tterrag.chatmux.bridge.AbstractChatService;
import com.tterrag.chatmux.discord.DiscordMessage;
import com.tterrag.chatmux.discord.DiscordService;

import discord4j.core.DiscordClient;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.GuildMessageChannel;
import discord4j.core.object.entity.TextChannel;
import discord4j.core.object.entity.User;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.annotation.NonNull;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

@RequiredArgsConstructor
@Slf4j
public class DiscordCommandHandler implements CommandHandler, Connectable {
    
    private final DiscordClient client;
    private final LinkManager manager;
    private final DiscordCommandListener baseListener;
    
    @NonNull
    private final Set<CommandListener> listeners = Sets.newConcurrentHashSet();
    
    public DiscordCommandHandler(DiscordClient client, LinkManager manager) {
        this.client = client;
        this.manager = manager;
        this.baseListener = new DiscordCommandListener(manager);
        addListener(this.baseListener);
    }
    
    @Override
    public Mono<Void> start() {
        Mono<Void> readyListener = client.getEventDispatcher()
                .on(ReadyEvent.class)
                .flatMap(e -> manager.readLinks().thenReturn(e))
                .flatMapIterable($ -> this.listeners)
                .flatMap(l -> l.onServiceAvailable(this))
                .then();
        
        Mono<Void> commandListener = client.getEventDispatcher()
                .on(MessageCreateEvent.class)
                .flatMap(mc -> Mono.zip(mc.getMessage().getChannel().cast(GuildMessageChannel.class), Mono.justOrEmpty(mc.getMessage().getAuthor()))
                        .flatMap(t -> runCommand(t.getT1(), t.getT2(), mc.getMessage().getContent().orElse(""))
                                .doOnError(ex -> log.error("Exception handling discord commands:", ex))
                                .onErrorResume($ -> Mono.empty())))
                .doOnError(t -> log.error("Exception handling message create", t))
                .then();

        return Mono.when(readyListener, commandListener, client.login());
    }
    
    private Tuple2<String, String> splitInput(String input) {
        int split = input.indexOf(' ');
        String command = split >= 0 ? input.substring(0, split) : input;
        String args = split >= 0 ? input.substring(split).trim() : "";
        return Tuples.of(command, args);
    }

    private Mono<Void> runCommand(GuildMessageChannel textChannel, User user, String content) {
        Tuple2<String, String> split = splitInput(content);
        return Flux.fromIterable(listeners)
                .flatMap(l -> l.runCommand(split.getT1(), new Context(split.getT2(), textChannel.getId().asString(), user.getId().asString(), textChannel)))
                .then();
    }
    
    public Mono<Boolean> canHandle(ChatService<?> service, String content) {
        Tuple2<String, String> split = splitInput(content);
        return Flux.fromIterable(listeners)
                .filterWhen(l -> l.canHandle(service, split.getT1(), split.getT2()))
                .hasElements();
    }

    @Override
    public void addListener(CommandListener listener) {
        listeners.add(listener);
    }
    
    @Override
    public Flux<? extends ChatMessage<?>> connect(String input) {
        return baseListener.getChannel(input)
                .flatMapMany(this::connect);
    }
    
    @Value
    private class Context implements CommandContext<DiscordMessage> {
        
        @Getter(onMethod = @__({@Override}))
        String args;
        @Getter(onMethod = @__({@Override}))
        String channelId;
        @Getter(onMethod = @__({@Override}))
        String userId;
        
        GuildMessageChannel channel;
        
        @Override
        public DiscordService getService() {
            return DiscordService.getInstance();
        }

        @Override
        public Mono<DiscordMessage> reply(String msg) {
            return channel.createMessage(msg).flatMap(DiscordMessage::create);
        }
        
        @Override
        public ChatService<?> getService(String name) {
            return Objects.requireNonNull(AbstractChatService.byName(name), "Invalid service name");
        }
        
        @Override
        public Flux<? extends ChatMessage<?>> connect(String input) {
            return DiscordCommandHandler.this.connect(input);
        }
    }
}
