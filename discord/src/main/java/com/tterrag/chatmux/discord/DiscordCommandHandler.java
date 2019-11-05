package com.tterrag.chatmux.discord;

import java.util.stream.Collectors;

import com.tterrag.chatmux.api.bridge.ChatChannel;
import com.tterrag.chatmux.bridge.AbstractChatService;
import com.tterrag.chatmux.bridge.ChatChannelImpl;
import com.tterrag.chatmux.links.LinkManager;
import com.tterrag.chatmux.links.LinkManager.Link;

import discord4j.core.object.entity.TextChannel;
import discord4j.core.object.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

@RequiredArgsConstructor
@Slf4j
public class DiscordCommandHandler {
    
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

    public Mono<?> handle(TextChannel channel, User author, String... args) {

        if (args.length >= 2 && (args[0].equals("+link") || args[0].equals("+linkraw"))) {            
            Mono<ChatChannelImpl<?>> from = getChannel(args[1]);
            Mono<ChatChannelImpl<?>> to = args.length >= 3 ? getChannel(args[2]) : Mono.just(new ChatChannelImpl<>(channel.getId().asString(), DiscordService.getInstance()));
            
            Mono<Tuple2<ChatChannelImpl<?>, ChatChannelImpl<?>>> sources = Mono.zip(from, to);
            
            final boolean raw = args[0].equals("+linkraw");

            return sources
                   .doOnNext(t -> manager.addLink(t.getT1(), t.getT2(), raw, manager.connect(t.getT1(), t.getT2(), raw)))
                   .flatMap(t -> channel.createMessage("Link established! " + t.getT1() + " -> " + t.getT2()))
                   .doOnError(t -> log.error("Exception establishing link", t))
                   .onErrorResume(IllegalArgumentException.class, e -> channel.createMessage(e.getMessage()))
                   .doOnError(t -> log.error("Secondary exception notifying error", t));
        } else if (args.length >= 2 && args[0].equals("-link")) {
            Mono<ChatChannelImpl<?>> from = getChannel(args[1]);
            Mono<ChatChannelImpl<?>> to = args.length >= 3 ? getChannel(args[2]) : Mono.just(new ChatChannelImpl<>(channel.getId().asString(), DiscordService.getInstance()));
            
            Mono<Tuple2<ChatChannel<?>, ChatChannel<?>>> sources = Mono.zip(from, to);

            return sources.flatMap(t -> {
                       if (manager.removeLink(t.getT1(), t.getT2())) {
                           return channel.createMessage("Link broken! " + t.getT1() + " -/> " + t.getT2());
                       } else {
                           return channel.createMessage("No such link to remove");
                       }
                   })
                   .doOnError(t -> log.error("Exception removing link", t))
                   .onErrorResume(IllegalArgumentException.class, m -> channel.createMessage(m.toString()));
        } else if (args[0].equals("~links")) {
            return Flux.fromIterable(manager.getLinks())
                    .flatMap(Link::prettyPrint)
                    .collect(Collectors.joining("\n"))
                    .flatMap(msg -> channel.createMessage(msg.length() == 0 ? "No links!" : msg.toString()));
        }
        return Mono.empty();
    }
}
