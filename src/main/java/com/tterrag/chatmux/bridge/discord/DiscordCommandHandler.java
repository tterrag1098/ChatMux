package com.tterrag.chatmux.bridge.discord;

import java.io.InputStream;
import java.time.Duration;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tterrag.chatmux.Main;
import com.tterrag.chatmux.bridge.ChatService;
import com.tterrag.chatmux.bridge.ChatChannel;
import com.tterrag.chatmux.bridge.ChatMessage;
import com.tterrag.chatmux.bridge.factorio.FactorioMessage;
import com.tterrag.chatmux.bridge.mixer.MixerRequestHelper;
import com.tterrag.chatmux.bridge.mixer.event.MixerEvent;
import com.tterrag.chatmux.bridge.mixer.method.MixerMethod;
import com.tterrag.chatmux.bridge.mixer.method.MixerMethod.MethodType;
import com.tterrag.chatmux.bridge.twitch.TwitchRequestHelper;
import com.tterrag.chatmux.bridge.twitch.irc.IRCEvent;
import com.tterrag.chatmux.links.LinkManager;
import com.tterrag.chatmux.links.LinkManager.Link;
import com.tterrag.chatmux.util.WebhookMessage;
import com.tterrag.chatmux.links.WebSocketFactory;
import com.tterrag.chatmux.websocket.WebSocketClient;

import discord4j.common.json.UserResponse;
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
                   .doOnError(IllegalArgumentException.class, e -> discordHelper.sendMessage(channel, e.getMessage()))
                   .doOnError(Throwable::printStackTrace);
        } else if (args.length >= 2 && args[0].equals("-link")) {
            Mono<ChatChannel<?, ?>> from = getChannel(args[1]);
            Mono<ChatChannel<?, ?>> to = args.length >= 3 ? getChannel(args[2]) : Mono.just(new ChatChannel<>(Long.toString(channel), ChatService.DISCORD));
            
            Mono<Tuple2<ChatChannel<?, ?>, ChatChannel<?, ?>>> sources = Mono.zip(from, to);

            return sources.doOnError(IllegalArgumentException.class, e -> discordHelper.sendMessage(channel, e.getMessage()))
                   .doOnError(Throwable::printStackTrace)
                   .doOnNext(t -> {
                       if (LinkManager.INSTANCE.removeLink(t.getT1(), t.getT2())) {
                           discordHelper.sendMessage(channel, "Link broken! " + t.getT1() + " -/> " + t.getT2());
                       } else {
                           discordHelper.sendMessage(channel, "No such link to remove");
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

        InputStream in = Main.class.getResourceAsStream("/logo.png");
        if (in == null) {
            throw new RuntimeException("Resource not found: logo.png");
        }
        
        Flux<? extends ChatMessage> source = LinkManager.INSTANCE.connect(from);

        Mono<Disposable> ret;
        
        if (to.getType() == ChatService.DISCORD) {        
            WebSocketClient<Dispatch, GatewayPayload<?>> discord = WebSocketFactory.get(ChatService.DISCORD).getSocket(to.getName());
            long channel = Long.parseLong(to.getName());
            Disposable sub = 
                  source.flatMap(m -> discordHelper.getWebhook(channel, "ChatMux", in).flatMap(wh -> discordHelper.executeWebhook(wh, new WebhookMessage(m.getContent(), m.getUser() + " (" + m.getSource() + "/" + m.getChannel() + ")", m.getAvatar()).toString())).map(r -> Tuples.of(m, r)))
                        .flatMap(t -> discordHelper.getChannel(channel).doOnNext(c -> LinkManager.INSTANCE.linkMessage(t.getT1(), new DiscordMessage(discordHelper, Long.toString(channel), t.getT2(), c.getGuildId()))).thenReturn(t))
                        .filter(t -> !Main.cfg.getModerators().isEmpty() || !Main.cfg.getAdmins().isEmpty())
                        .flatMap(t -> discordHelper.addReaction(t.getT2().getChannelId(), t.getT2().getId(), null, ADMIN_EMOTE).thenReturn(t))
                        .flatMap(t -> discord.inbound().ofType(MessageReactionAdd.class)
                                .take(Duration.ofSeconds(5))
                                .filterWhen(mra -> discordHelper.getOurUser().map(UserResponse::getId).map(id -> id != mra.getUserId()))
                                .filter(mra -> mra.getMessageId() == t.getT2().getId())
                                .filter(mra -> mra.getEmoji().getName().equals(ADMIN_EMOTE))
                                .next()
                                .flatMap(mra -> discordHelper.deleteMessage(mra.getChannelId(), mra.getMessageId()).and(t.getT1().delete()).thenReturn(t.getT2()))
                                .switchIfEmpty(discordHelper.getOurUser().flatMap(u -> discordHelper.removeReaction(t.getT2().getChannelId(), u.getId(), t.getT2().getId(), null, ADMIN_EMOTE)).thenReturn(t.getT2())))
                        .subscribe();
            ret = discordHelper.sendMessage(channel, "Link established! " + from + " -> " + to).thenReturn(sub);
        } else if (to.getType() == ChatService.MIXER) {
            WebSocketClient<MixerEvent, MixerMethod> mixer = WebSocketFactory.get(ChatService.MIXER).getSocket(to.getName());
            ret = Mono.just(source.subscribe(m -> mixer.outbound().next(new MixerMethod(MethodType.MESSAGE, (raw ? m.getContent() : m.toString())))));
        } else if (to.getType() == ChatService.TWITCH) {
            WebSocketClient<IRCEvent, String> twitch = WebSocketFactory.get(ChatService.TWITCH).getSocket(to.getName());
            ret = Mono.just(source.subscribe(m -> twitch.outbound().next("PRIVMSG #" + to.getName().toLowerCase(Locale.ROOT) + " :" + (raw ? m.getContent() : m))));
        } else if (to.getType() == ChatService.FACTORIO) {
            WebSocketClient<FactorioMessage, String> factorio = WebSocketFactory.get(ChatService.FACTORIO).getSocket(to.getName());
            ret = Mono.just(source.subscribe(m -> factorio.outbound().next(raw ? m.getContent() : m.toString())));
        } else {
            throw new IllegalArgumentException("Invalid target service");
        }
        
        return ret;
    }
}
