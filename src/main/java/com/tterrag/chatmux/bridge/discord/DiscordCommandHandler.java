package com.tterrag.chatmux.bridge.discord;

import java.io.InputStream;
import java.time.Duration;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tterrag.chatmux.Main;
import com.tterrag.chatmux.bridge.mixer.MixerRequestHelper;
import com.tterrag.chatmux.bridge.mixer.event.MixerEvent;
import com.tterrag.chatmux.bridge.mixer.method.MixerMethod;
import com.tterrag.chatmux.bridge.mixer.method.MixerMethod.MethodType;
import com.tterrag.chatmux.bridge.twitch.irc.IRCEvent;
import com.tterrag.chatmux.links.Channel;
import com.tterrag.chatmux.links.ChatSource;
import com.tterrag.chatmux.links.LinkManager;
import com.tterrag.chatmux.links.LinkManager.Link;
import com.tterrag.chatmux.links.Message;
import com.tterrag.chatmux.links.WebSocketFactory;
import com.tterrag.chatmux.util.ServiceType;
import com.tterrag.chatmux.websocket.WebSocketClient;

import discord4j.gateway.json.GatewayPayload;
import discord4j.gateway.json.dispatch.Dispatch;
import discord4j.gateway.json.dispatch.MessageReactionAdd;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

public class DiscordCommandHandler {
    
    private static final String ADMIN_EMOTE = "\u274C";
    
    private static final Pattern CHANNEL_MENTION = Pattern.compile("<#(\\d+)>");
    
    private final DiscordRequestHelper discordHelper;
    
    private final MixerRequestHelper mixerHelper = new MixerRequestHelper(new ObjectMapper(), Main.cfg.getMixer().getClientId(), Main.cfg.getMixer().getToken());

    public DiscordCommandHandler(String token) {
        this.discordHelper = new DiscordRequestHelper(token);
    }
    
    private Mono<Channel<?, ?>> getChannel(String input) {
        String[] data = input.split("/");
        if (data.length != 2) {
            return Mono.error(new IllegalArgumentException("Link must be in the format `service/channel`"));
        }
        ServiceType<?, ?> type = ServiceType.byName(data[0]);
        if (type == null) {
            return Mono.error(new IllegalArgumentException("Invalid service name"));
        }
        Mono<String> ret = Mono.just(data[1]);
        if (type == ServiceType.DISCORD) {
            ret = ret.map(name -> {
                try {
                    Long.parseLong(name);
                    return name;
                } catch (NumberFormatException e) {
                    Matcher m = CHANNEL_MENTION.matcher(name);
                    if (m.matches()) {
                        return m.group(1);
                    } else {
                        throw new IllegalArgumentException("Channel must be a mention or ID");
                    }
                }
            }).filter(s -> !s.isEmpty());
        } else if (type == ServiceType.MIXER) {
            ret = ret.flatMap(name -> {
                try {
                    Integer.parseInt(name);
                    return Mono.just(name);
                } catch (NumberFormatException e) {
                    return mixerHelper.getChannel(name).map(c -> c.id).map(c -> Integer.toString(c));
                }
            });
        }
        return ret.map(name -> new Channel<>(name, type));
    }

    public void handle(long channel, long author, String... args) {

        if (args.length >= 2 && (args[0].equals("+link") || args[0].equals("+linkraw"))) {            
            Mono<Channel<?, ?>> from = getChannel(args[1]);
            Mono<Channel<?, ?>> to = args.length >= 3 ? getChannel(args[2]) : Mono.just(new Channel<>(Long.toString(channel), ServiceType.DISCORD));
            
            Mono<Tuple2<Channel<?, ?>, Channel<?, ?>>> sources = Mono.zip(from, to);
            
            final boolean raw = args[0].equals("+linkraw");

            sources.zipWhen(t -> Mono.just(connect(discordHelper, mixerHelper, t.getT1(), t.getT2(), raw)), (t, d) -> Tuples.of(t.getT1(), t.getT2(), d))
                   .doOnNext(t -> LinkManager.INSTANCE.addLink(t.getT1(), t.getT2(), raw, t.getT3()))
                   .doOnError(IllegalArgumentException.class, e -> discordHelper.sendMessage(channel, e.getMessage()))
                   .doOnError(Throwable::printStackTrace)
                   .subscribe();
        } else if (args.length >= 2 && args[0].equals("-link")) {
            Mono<Channel<?, ?>> from = getChannel(args[1]);
            Mono<Channel<?, ?>> to = args.length >= 3 ? getChannel(args[2]) : Mono.just(new Channel<>(Long.toString(channel), ServiceType.DISCORD));
            
            Mono<Tuple2<Channel<?, ?>, Channel<?, ?>>> sources = Mono.zip(from, to);

            sources.doOnError(IllegalArgumentException.class, e -> discordHelper.sendMessage(channel, e.getMessage()))
                   .doOnError(Throwable::printStackTrace)
                   .subscribe(t -> {
                       LinkManager.INSTANCE.removeLink(t.getT1(), t.getT2());
                       discordHelper.sendMessage(channel, "Link broken! " + t.getT1() + " -/> " + t.getT2());
                   });
        } else if (args[0].equals("~links")) {
            StringBuilder msg = new StringBuilder();
            for (Link link : LinkManager.INSTANCE.getLinks()) {
                 msg.append(link).append("\n");
            }
            discordHelper.sendMessage(channel, msg.toString());
        }
    }
    
    public static Disposable connect(DiscordRequestHelper discordHelper, MixerRequestHelper mixerHelper, Channel<?, ?> from, Channel<?, ?> to, boolean raw) {

        InputStream in = Main.class.getResourceAsStream("/logo.png");
        if (in == null) {
            throw new RuntimeException("Resource not found: logo.png");
        }
        
        Flux<Message> source = LinkManager.INSTANCE.connect(from);

        Disposable sub;
        
        if (to.getType() == ServiceType.DISCORD) {        
            WebSocketClient<Dispatch, GatewayPayload<?>> discord = WebSocketFactory.get(ServiceType.DISCORD).getSocket(to.getName());
            long channel = Long.parseLong(to.getName());
            sub = source.flatMap(m -> discordHelper.getWebhook(channel, "ChatMux", in).flatMap(wh -> discordHelper.executeWebhook(wh, new WebhookMessage(m.getContent(), m.getUser() + " (" + m.getSource() + "/" + m.getChannel() + ")", m.getAvatar()).toString())).map(r -> Tuples.of(m, r)))
                        .doOnNext(t -> discordHelper.getChannel(channel).subscribe(c -> LinkManager.INSTANCE.linkMessage(t.getT1(), new DiscordMessage(discordHelper, Long.toString(channel), t.getT2(), c.getGuildId()))))
                        .doOnNext(t -> discordHelper.addReaction(t.getT2().getChannelId(), t.getT2().getId(), null, ADMIN_EMOTE))
                        .map(t -> Tuples.of(t.getT2(), discord.inbound().ofType(MessageReactionAdd.class)
                                .filter(mra -> mra.getUserId() != Main.botUser.getId())
                                .filter(mra -> mra.getMessageId() == t.getT2().getId())
                                .filter(mra -> mra.getEmoji().getName().equals(ADMIN_EMOTE))
                                .doOnNext(mra -> discordHelper.deleteMessage(mra.getChannelId(), mra.getMessageId()))
                                .subscribe(mra -> t.getT1().delete())))
                        .subscribe(tuple -> Mono.just(tuple).delayElement(Duration.ofMinutes(1)).subscribe(t -> {
                            discordHelper.getOurUser().subscribe(u -> discordHelper.removeReaction(t.getT1().getChannelId(), u.getId(), t.getT1().getId(), null, ADMIN_EMOTE));
                            t.getT2().dispose();
                        }));
            discordHelper.sendMessage(channel, "Link established! " + from + " -> " + to);
        } else if (to.getType() == ServiceType.MIXER) {
            WebSocketClient<MixerEvent, MixerMethod> mixer = WebSocketFactory.get(ServiceType.MIXER).getSocket(to.getName());
            new ChatSource.Mixer(mixerHelper).connect(mixer, to.getName());
            sub = source.subscribe(m -> mixer.outbound().next(new MixerMethod(MethodType.MESSAGE, (raw ? m.getContent() : m.toString()))));
        } else if (to.getType() == ServiceType.TWITCH) {
            WebSocketClient<IRCEvent, String> twitch = WebSocketFactory.get(ServiceType.TWITCH).getSocket(to.getName());
            new ChatSource.Twitch().connect(twitch, to.getName());
            sub = source.subscribe(m -> twitch.outbound().next("PRIVMSG #" + to.getName().toLowerCase(Locale.ROOT) + " :" + (raw ? m.getContent() : m)));
        } else {
            throw new IllegalArgumentException("Invalid target service");
        }
        
        return sub;
    }
}
