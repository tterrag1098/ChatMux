package com.tterrag.chatmux.discord;

import com.tterrag.chatmux.bridge.ChatChannel;
import com.tterrag.chatmux.bridge.ChatService;
import com.tterrag.chatmux.links.LinkManager;
import com.tterrag.chatmux.links.LinkManager.Link;

import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.util.annotation.NonNull;
import reactor.util.function.Tuple2;

public class DiscordCommandHandler {
            
    @NonNull
    private final DiscordRequestHelper discordHelper;
    
    public DiscordCommandHandler(String token) {
        this.discordHelper = new DiscordRequestHelper(token);
    }
    
    private Mono<ChatChannel<?, ?>> getChannel(String input) {
        String[] data = input.split("/");
        if (data.length != 2) {
            return Mono.error(new IllegalArgumentException("Link must be in the format `service/channel`"));
        }
        ChatService<?, ?> type = ChatService.byName(data[0]);
        if (type == null) {
            return Mono.error(new IllegalArgumentException("Invalid service name"));
        }
        return Mono.just(data[1])
                .flatMap(s -> type.getSource().parseChannel(s))
                .map(name -> new ChatChannel<>(name, type));
    }

    public Mono<?> handle(long channel, long author, String... args) {

        if (args.length >= 2 && (args[0].equals("+link") || args[0].equals("+linkraw"))) {            
            Mono<ChatChannel<?, ?>> from = getChannel(args[1]);
            Mono<ChatChannel<?, ?>> to = args.length >= 3 ? getChannel(args[2]) : Mono.just(new ChatChannel<>(Long.toString(channel), DiscordService.getInstance()));
            
            Mono<Tuple2<ChatChannel<?, ?>, ChatChannel<?, ?>>> sources = Mono.zip(from, to);
            
            final boolean raw = args[0].equals("+linkraw");

            return sources.flatMap(t -> connect(t.getT1(), t.getT2(), raw)
                            .doOnNext(s -> LinkManager.INSTANCE.addLink(t.getT1(), t.getT2(), raw, s))
                            .thenReturn(t))
                   .flatMap(t -> discordHelper.sendMessage(channel, "Link established! " + t.getT1() + " -> " + t.getT2()).thenReturn(t))
                   .doOnError(IllegalArgumentException.class, e -> discordHelper.sendMessage(channel, e.getMessage()))
                   .doOnError(Throwable::printStackTrace);
        } else if (args.length >= 2 && args[0].equals("-link")) {
            Mono<ChatChannel<?, ?>> from = getChannel(args[1]);
            Mono<ChatChannel<?, ?>> to = args.length >= 3 ? getChannel(args[2]) : Mono.just(new ChatChannel<>(Long.toString(channel), DiscordService.getInstance()));
            
            Mono<Tuple2<ChatChannel<?, ?>, ChatChannel<?, ?>>> sources = Mono.zip(from, to);

            return sources.doOnError(IllegalArgumentException.class, e -> discordHelper.sendMessage(channel, e.getMessage()))
                   .doOnError(Throwable::printStackTrace)
                   .flatMap(t -> {
                       if (LinkManager.INSTANCE.removeLink(t.getT1(), t.getT2())) {
                           return discordHelper.sendMessage(channel, "Link broken! " + t.getT1() + " -/> " + t.getT2());
                       } else {
                           return discordHelper.sendMessage(channel, "No such link to remove");
                       }
                   })
                   .onErrorResume(IllegalArgumentException.class, t -> discordHelper.sendMessage(channel, t.toString()));
        } else if (args[0].equals("~links")) {
            StringBuilder msg = new StringBuilder();
            for (Link link : LinkManager.INSTANCE.getLinks()) {
                 msg.append(link).append("\n");
            }
            return discordHelper.sendMessage(channel, msg.toString());
        }
        return Mono.empty();
    }
    
    public static Mono<Disposable> connect(ChatChannel<?, ?> from, ChatChannel<?, ?> to, boolean raw) {
        return Mono.just(to.getType().getSource())
                .map(s -> from.connect().flatMap(m -> s.send(to.getName(), m, raw)).subscribe());
    }
}
