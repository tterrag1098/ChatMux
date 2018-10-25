package com.tterrag.chatmux.bridge.discord;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tterrag.chatmux.Main;
import com.tterrag.chatmux.bridge.mixer.MixerRequestHelper;
import com.tterrag.chatmux.bridge.mixer.event.MixerEvent;
import com.tterrag.chatmux.bridge.mixer.method.MixerMethod;
import com.tterrag.chatmux.bridge.mixer.response.ChatResponse;
import com.tterrag.chatmux.bridge.twitch.irc.IRCEvent;
import com.tterrag.chatmux.links.ChatSource;
import com.tterrag.chatmux.links.Message;
import com.tterrag.chatmux.websocket.DecoratedGatewayClient;
import com.tterrag.chatmux.websocket.FrameParser;
import com.tterrag.chatmux.websocket.SimpleWebSocketClient;
import com.tterrag.chatmux.websocket.WebSocketClient;

import discord4j.gateway.json.dispatch.MessageReactionAdd;
import reactor.core.publisher.Flux;
import reactor.util.function.Tuples;

public class DiscordCommandHandler {
    
    private static final String ADMIN_EMOTE = "\u2757";
    
    private final DiscordRequestHelper discordHelper;
    private final MixerRequestHelper mixerHelper = new MixerRequestHelper(new ObjectMapper(), Main.cfg.getMixer().getClientId(), Main.cfg.getMixer().getToken());

    
    // FIXME TEMP
    private final DecoratedGatewayClient discord;
    
    private final Map<Integer, WebSocketClient<MixerEvent, MixerMethod>> mixer = new HashMap<>();
    private final WebSocketClient<IRCEvent, String> twitch;

    public DiscordCommandHandler(DecoratedGatewayClient client, String token) {
        this.discord = client;
        
        discord.inbound().ofType(MessageReactionAdd.class)
                .filter(p -> p.getUserId() != Main.botUser.getId())
                .filter(p -> Main.cfg.getAdmins().stream().anyMatch(perm -> perm.getDiscord() != null && perm.getDiscord() == p.getUserId()))
                .subscribe(p -> System.out.println(p.getEmoji()));
        
        this.discordHelper = new DiscordRequestHelper(token);
        
//        System.out.println(mrh.post("/oauth/token/introspect", "{\"token\":\"" + config.getMixer().getToken() + "\"}", TokenIntrospectResponse.class).block());
        
        twitch = new SimpleWebSocketClient<>();
        twitch.connect("wss://irc-ws.chat.twitch.tv:443", new FrameParser<>(IRCEvent::parse, Function.identity()))
            .subscribe();
        
        twitch.outbound()
            .next("PASS oauth:" + Main.cfg.getTwitch().getToken())
            .next("NICK " + Main.cfg.getTwitch().getNick())
            .next("CAP REQ :twitch.tv/tags");
    }

    public void handle(long channel, long author, String... args) {

        if (args.length == 3 && args[0].equals("link")) {

            InputStream in = Main.class.getResourceAsStream("/logo.png");
            if (in == null) {
                throw new RuntimeException("Resource not found: logo.png");
            }
            
            Flux<Message> source;

            switch (args[1].toLowerCase(Locale.ROOT)) {
                case "discord":
                    source = new ChatSource.Discord(discordHelper).connect(discord, args[2]);
                    break;
                case "twitch": 
                    source = new ChatSource.Twitch().connect(twitch, args[2]);
                    break;
                case "mixer":
                    int chan = Integer.parseInt(args[2]);
                    WebSocketClient<MixerEvent, MixerMethod> ws = mixer.get(chan);
                    if (ws == null) {
                        final WebSocketClient<MixerEvent, MixerMethod> socket = new SimpleWebSocketClient<>();
                        mixer.put(chan, socket);
                        MixerRequestHelper mrh = new MixerRequestHelper(new ObjectMapper(), Main.cfg.getMixer().getClientId(), Main.cfg.getMixer().getToken());
                        source = mrh.get("/chats/" + chan, ChatResponse.class)
                                .doOnNext(chat -> socket.connect(chat.endpoints[0], new FrameParser<>(MixerEvent::parse, new ObjectMapper())).subscribe())
                                .map(chat -> new ChatSource.Mixer(mixerHelper, chat))
                                .flatMapMany(ms -> ms.connect(socket, args[2]));
                    } else {
                        source = new ChatSource.Mixer(mixerHelper, null).connect(ws, args[2]);
                    }
                    break;
                default:
                    throw new RuntimeException("Invalid service");
            }
            
            source.flatMap(m -> discordHelper.getWebhook(channel, "ChatMux", in).flatMap(wh -> discordHelper.executeWebhook(wh, "{\"content\":\"" + m + "\"}")).map(r -> Tuples.of(m, r)))
                  .doOnNext(t -> discordHelper.addReaction(t.getT2().getChannelId(), t.getT2().getId(), null, ADMIN_EMOTE))
                  .subscribe(t -> discord.inbound().ofType(MessageReactionAdd.class)
                          .filter(mra -> mra.getUserId() != Main.botUser.getId())
                          .filter(mra -> mra.getMessageId() == t.getT2().getId())
                          .filter(mra -> mra.getEmoji().getName().equals(ADMIN_EMOTE))
                          .doOnNext(mra -> discordHelper.deleteMessage(mra.getChannelId(), mra.getMessageId()))
                          .subscribe(mra -> t.getT1().delete()));
        }
    }
}
