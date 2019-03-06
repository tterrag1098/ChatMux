package com.tterrag.chatmux.bridge.discord;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tterrag.chatmux.Main;
import com.tterrag.chatmux.bridge.ChatChannel;
import com.tterrag.chatmux.bridge.ChatService;
import com.tterrag.chatmux.bridge.mixer.MixerRequestHelper;
import com.tterrag.chatmux.bridge.twitch.TwitchRequestHelper;
import com.tterrag.chatmux.links.LinkManager;
import com.tterrag.chatmux.links.LinkManager.Link;

import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.util.annotation.NonNull;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

public class DiscordCommandHandler {
        
    private static final Pattern CHANNEL_MENTION = Pattern.compile("<#(\\d+)>");
    
    @NonNull
    private final DiscordRequestHelper discordHelper;
    @NonNull
    private final MixerRequestHelper mixerHelper = new MixerRequestHelper(new ObjectMapper(), Main.cfg.getMixer().getClientId(), Main.cfg.getMixer().getToken());
    @NonNull
    private final TwitchRequestHelper twitchHelper = new TwitchRequestHelper(new ObjectMapper(), Main.cfg.getTwitch().getToken());

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
        Mono<String> ret = Mono.just(data[1]);
        if (type == ChatService.DISCORD) {
            ret = ret.map(name -> {
                try {
                    Long.parseLong(name);
                    return name;
                } catch (NumberFormatException e) {
                    Matcher m = CHANNEL_MENTION.matcher(name);
                    if (m.matches()) {
                        return m.group(1);
                    } else {
                        throw new IllegalArgumentException("ChatChannel must be a mention or ID");
                    }
                }
            }).filter(s -> !s.isEmpty());
        } else if (type == ChatService.MIXER) {
            ret = ret.flatMap(name -> {
                try {
                    Integer.parseInt(name);
                    return Mono.just(name);
                } catch (NumberFormatException e) {
                    return mixerHelper.getChannel(name).map(c -> c.id).map(c -> Integer.toString(c));
                }
            });
        }
        return ret.map(name -> new ChatChannel<>(name, type));
    }

    public Mono<?> handle(long channel, long author, String... args) {

        if (args.length >= 2 && (args[0].equals("+link") || args[0].equals("+linkraw"))) {            
            Mono<ChatChannel<?, ?>> from = getChannel(args[1]);
            Mono<ChatChannel<?, ?>> to = args.length >= 3 ? getChannel(args[2]) : Mono.just(new ChatChannel<>(Long.toString(channel), ChatService.DISCORD));
            
            Mono<Tuple2<ChatChannel<?, ?>, ChatChannel<?, ?>>> sources = Mono.zip(from, to);
            
            final boolean raw = args[0].equals("+linkraw");

            return sources.zipWhen(t -> connect(discordHelper, mixerHelper, twitchHelper, t.getT1(), t.getT2(), raw), (t, d) -> Tuples.of(t.getT1(), t.getT2(), d))
                   .doOnNext(t -> LinkManager.INSTANCE.addLink(t.getT1(), t.getT2(), raw, t.getT3()))
                   .flatMap(t -> discordHelper.sendMessage(channel, "Link established! " + t.getT1() + " -> " + t.getT2()).thenReturn(t))
                   .doOnError(IllegalArgumentException.class, e -> discordHelper.sendMessage(channel, e.getMessage()))
                   .doOnError(Throwable::printStackTrace);
        } else if (args.length >= 2 && args[0].equals("-link")) {
            Mono<ChatChannel<?, ?>> from = getChannel(args[1]);
            Mono<ChatChannel<?, ?>> to = args.length >= 3 ? getChannel(args[2]) : Mono.just(new ChatChannel<>(Long.toString(channel), ChatService.DISCORD));
            
            Mono<Tuple2<ChatChannel<?, ?>, ChatChannel<?, ?>>> sources = Mono.zip(from, to);

            return sources.doOnError(IllegalArgumentException.class, e -> discordHelper.sendMessage(channel, e.getMessage()))
                   .doOnError(Throwable::printStackTrace)
                   .flatMap(t -> {
                       if (LinkManager.INSTANCE.removeLink(t.getT1(), t.getT2())) {
                           return discordHelper.sendMessage(channel, "Link broken! " + t.getT1() + " -/> " + t.getT2());
                       } else {
                           return discordHelper.sendMessage(channel, "No such link to remove");
                       }
                   });
        } else if (args[0].equals("~links")) {
            StringBuilder msg = new StringBuilder();
            for (Link link : LinkManager.INSTANCE.getLinks()) {
                 msg.append(link).append("\n");
            }
            return discordHelper.sendMessage(channel, msg.toString());
        }
        return Mono.empty();
    }
    
    public static Mono<Disposable> connect(DiscordRequestHelper discordHelper, MixerRequestHelper mixerHelper, TwitchRequestHelper twitchHelper, ChatChannel<?, ?> from, ChatChannel<?, ?> to, boolean raw) {
        return Mono.just(to.getType().getSource())
                .map(s -> from.connect().flatMap(m -> s.send(to.getName(), m, raw)).subscribe());
    }
}
