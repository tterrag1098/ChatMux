package com.tterrag.chatmux.discord.command;

import java.util.stream.Collectors;

import com.tterrag.chatmux.api.bridge.ChatChannel;
import com.tterrag.chatmux.api.bridge.ChatMessage;
import com.tterrag.chatmux.api.command.CommandContext;
import com.tterrag.chatmux.api.command.CommandListener;
import com.tterrag.chatmux.bridge.AbstractChatService;
import com.tterrag.chatmux.bridge.ChatChannelImpl;
import com.tterrag.chatmux.discord.DiscordService;
import com.tterrag.chatmux.links.LinkManager;
import com.tterrag.chatmux.links.LinkManager.Link;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

@RequiredArgsConstructor
@Slf4j
public class DiscordCommandListener implements CommandListener {
    
    private final LinkManager manager;
    
    private Mono<ChatChannelImpl<?>> getChannel(String input) {
        String[] data = input.split("/");
        if (data.length != 2) {
            return Mono.error(new IllegalArgumentException("Link must be in the format `service/channel`"));
        }
        AbstractChatService<?, ?> type = AbstractChatService.byName(data[0]);
        if (type == null) {
            return Mono.error(new IllegalArgumentException("Invalid service name"));
        }
        return Mono.just(data[1])
                .flatMap(s -> type.parseChannel(s))
                .map(name -> new ChatChannelImpl<>(name, type));
    }

    @Override
    public <M extends ChatMessage<M>> Mono<?> runCommand(String command, CommandContext<M> ctx) {
        String[] args = ctx.getSplitArgs();
        if (args.length >= 1 && (command.equals("+link") || command.equals("+linkraw"))) {            
            Mono<ChatChannelImpl<?>> from = getChannel(args[0]);
            Mono<ChatChannelImpl<?>> to = args.length >= 2 ? getChannel(args[1]) : Mono.just(new ChatChannelImpl<>(ctx.getChannelId(), DiscordService.getInstance()));
            
            Mono<Tuple2<ChatChannelImpl<?>, ChatChannelImpl<?>>> sources = Mono.zip(from, to);
            
            final boolean raw = args[0].equals("+linkraw");

            return sources
                   .map(t -> manager.addLink(t.getT1(), t.getT2(), raw, manager.connect(t.getT1(), t.getT2(), raw)))
                   .flatMap(Link::prettyPrint)
                   .flatMap(link -> ctx.reply("Link established! " + link))
                   .doOnError(t -> log.error("Exception establishing link", t))
                   .onErrorResume(IllegalArgumentException.class, e -> ctx.reply(e.getMessage()))
                   .doOnError(t -> log.error("Secondary exception notifying error", t));
        } else if (args.length >= 1 && command.equals("-link")) {
            Mono<ChatChannelImpl<?>> from = getChannel(args[0]);
            Mono<ChatChannelImpl<?>> to = args.length >= 2 ? getChannel(args[1]) : Mono.just(new ChatChannelImpl<>(ctx.getChannelId(), DiscordService.getInstance()));
            
            Mono<Tuple2<ChatChannel<?>, ChatChannel<?>>> sources = Mono.zip(from, to);

            return sources.flatMapIterable(t -> manager.removeLink(t.getT1(), t.getT2()))
                    .flatMap(Link::prettyPrint)
                    .flatMap(link -> ctx.reply("Link broken! " + link))
                    .switchIfEmpty(ctx.reply("No such link to remove"))
                    .doOnError(t -> log.error("Exception removing link", t))
                    .onErrorResume(IllegalArgumentException.class, m -> ctx.reply(m.toString()))
                    .then();
        } else if (command.equals("~links")) {
            return Flux.fromIterable(manager.getLinks())
                    .flatMap(Link::prettyPrint)
                    .collect(Collectors.joining("\n"))
                    .flatMap(msg -> ctx.reply(msg.length() == 0 ? "No links!" : msg.toString()));
        }
        return Mono.empty();
    }
}
