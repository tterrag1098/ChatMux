package com.tterrag.chatmux.bridge.discord;

import java.io.InputStream;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tterrag.chatmux.Main;
import com.tterrag.chatmux.bridge.mixer.MixerRequestHelper;
import com.tterrag.chatmux.links.Channel;
import com.tterrag.chatmux.links.LinkManager;
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
import reactor.util.function.Tuples;

public class DiscordCommandHandler {
    
    private static final String ADMIN_EMOTE = "\u274C";
    
    private static final Pattern CHANNEL_MENTION = Pattern.compile("<#(\\d+)>");
    
    private final DiscordRequestHelper discordHelper;
    
    private final MixerRequestHelper mixerHelper = new MixerRequestHelper(new ObjectMapper(), Main.cfg.getMixer().getClientId(), Main.cfg.getMixer().getToken());

    public DiscordCommandHandler(String token) {
        this.discordHelper = new DiscordRequestHelper(token);
    }
    
    private Mono<String> getChannelName(long channel, String input, ServiceType<?, ?> type) {
        Mono<String> ret = Mono.just(input);
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
                        return "";
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
        return ret;
    }

    public void handle(long channel, long author, String... args) {

        if (args.length == 3 && args[0].equals("link")) {            
            ServiceType<?, ?> type = ServiceType.byName(args[1]);
 
            Mono<String> channelName = getChannelName(channel, args[2], type);
            Mono<Channel<?, ?>> from = channelName.map(name -> new Channel<>(name, type));
            Channel<?, ?> to = new Channel<>(Long.toString(channel), ServiceType.DISCORD);

            from.zipWhen(chan -> Mono.just(connect(discordHelper, chan, to)))
                .doOnNext(t -> LinkManager.INSTANCE.addLink(t.getT1(), to, t.getT2()))
                .subscribe();
            
            from.switchIfEmpty(Mono.just(to).doOnNext(chan -> discordHelper.sendMessage(channel, "Channel must be a mention or ID"))).subscribe();
        } else if (args.length == 3 && args[0].equals("unlink")) {
            ServiceType<?, ?> type = ServiceType.byName(args[1]);
            Mono<Channel<?, ?>> from = getChannelName(channel, args[2], type).map(chan -> new Channel<>(chan, type));
            Channel<?, ?> to = new Channel<>(Long.toString(channel), ServiceType.DISCORD);
            
            from.subscribe(chan -> {
                LinkManager.INSTANCE.removeLink(chan, to);
                discordHelper.sendMessage(channel, "Link broken! " + chan + " -/> " + to);
            });
            from.switchIfEmpty(Mono.just(to).doOnNext(chan -> discordHelper.sendMessage(channel, "Channel must be a mention or ID"))).subscribe();
        }
    }
    
    public static Disposable connect(DiscordRequestHelper discordHelper, Channel<?, ?> from, Channel<?, ?> to) {

        InputStream in = Main.class.getResourceAsStream("/logo.png");
        if (in == null) {
            throw new RuntimeException("Resource not found: logo.png");
        }
        
        Flux<Message> source = LinkManager.INSTANCE.connect(from);
        WebSocketClient<Dispatch, GatewayPayload<?>> discord = WebSocketFactory.get(ServiceType.DISCORD).getSocket(to.getName());
        
        long channel = Long.parseLong(to.getName());
        Disposable sub = 
          source.flatMap(m -> discordHelper.getWebhook(channel, "ChatMux", in).flatMap(wh -> discordHelper.executeWebhook(wh, "{\"content\":\"" + m + "\"}")).map(r -> Tuples.of(m, r)))
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
        
        return sub;
    }
}
