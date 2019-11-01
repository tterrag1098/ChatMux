package com.tterrag.chatmux.discord;

import com.tterrag.chatmux.bridge.ChatChannel;
import com.tterrag.chatmux.bridge.ChatService;
import com.tterrag.chatmux.links.LinkManager;
import com.tterrag.chatmux.links.LinkManager.Link;

import discord4j.core.object.entity.TextChannel;
import discord4j.core.object.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

@RequiredArgsConstructor
@Slf4j
public class DiscordCommandHandler {
    
    private Mono<ChatChannel<?, ?>> getChannel(String input) {
        String[] data = input.split("/");
        if (data.length != 2) {
            return Mono.error(new IllegalArgumentException("Link must be in the format `service/channel`"));
        }
        ChatService type = ChatService.byName(data[0]);
        if (type == null) {
            return Mono.error(new IllegalArgumentException("Invalid service name"));
        }
        return Mono.just(data[1])
                .flatMap(s -> type.getSource().parseChannel(s))
                .map(name -> new ChatChannel<>(name, type));
    }

    public Mono<?> handle(TextChannel channel, User author, String... args) {

        if (args.length >= 2 && (args[0].equals("+link") || args[0].equals("+linkraw"))) {            
            Mono<ChatChannel<?, ?>> from = getChannel(args[1]);
            Mono<ChatChannel<?, ?>> to = args.length >= 3 ? getChannel(args[2]) : Mono.just(new ChatChannel<>(channel.getId().asString(), DiscordService.getInstance()));
            
            Mono<Tuple2<ChatChannel<?, ?>, ChatChannel<?, ?>>> sources = Mono.zip(from, to);
            
            final boolean raw = args[0].equals("+linkraw");

            return sources.flatMap(t -> connect(t.getT1(), t.getT2(), raw)
                            .doOnNext(s -> LinkManager.INSTANCE.addLink(t.getT1(), t.getT2(), raw, s))
                            .thenReturn(t))
                   .flatMap(t -> channel.createMessage("Link established! " + t.getT1() + " -> " + t.getT2()))
                   .doOnError(t -> log.error("Exception establishing link", t))
                   .onErrorResume(IllegalArgumentException.class, e -> channel.createMessage(e.getMessage()))
                   .doOnError(t -> log.error("Secondary exception notifying error", t));
        } else if (args.length >= 2 && args[0].equals("-link")) {
            Mono<ChatChannel<?, ?>> from = getChannel(args[1]);
            Mono<ChatChannel<?, ?>> to = args.length >= 3 ? getChannel(args[2]) : Mono.just(new ChatChannel<>(channel.getId().asString(), DiscordService.getInstance()));
            
            Mono<Tuple2<ChatChannel<?, ?>, ChatChannel<?, ?>>> sources = Mono.zip(from, to);

            return sources.flatMap(t -> {
                       if (LinkManager.INSTANCE.removeLink(t.getT1(), t.getT2())) {
                           return channel.createMessage("Link broken! " + t.getT1() + " -/> " + t.getT2());
                       } else {
                           return channel.createMessage("No such link to remove");
                       }
                   })
                   .doOnError(t -> log.error("Exception removing link", t))
                   .onErrorResume(IllegalArgumentException.class, m -> channel.createMessage(m.toString()));
        } else if (args[0].equals("~links")) {
            StringBuilder msg = new StringBuilder();
            for (Link link : LinkManager.INSTANCE.getLinks()) {
                 msg.append(link).append("\n");
            }
            return channel.createMessage(msg.length() == 0 ? "No links!" : msg.toString());
        }
        return Mono.empty();
    }
    
    public static Mono<Disposable> connect(ChatChannel<?, ?> from, ChatChannel<?, ?> to, boolean raw) {
        return Mono.just(to.getType().getSource())
                .map(s -> from.connect().flatMap(m -> s.send(to.getName(), m, raw)).subscribe());
    }
}
