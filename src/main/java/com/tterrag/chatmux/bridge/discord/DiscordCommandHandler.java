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
import com.tterrag.chatmux.links.ChatSource.MixerSource;
import com.tterrag.chatmux.links.ChatSource.TwitchSource;
import com.tterrag.chatmux.links.Message;
import com.tterrag.chatmux.websocket.DecoratedGatewayClient;
import com.tterrag.chatmux.websocket.FrameParser;
import com.tterrag.chatmux.websocket.SimpleWebSocketClient;
import com.tterrag.chatmux.websocket.WebSocketClient;

import reactor.core.publisher.Flux;

public class DiscordCommandHandler {
    
    private final DiscordRequestHelper helper;
    private final MixerRequestHelper mixerHelper = new MixerRequestHelper(new ObjectMapper(), Main.cfg.getMixer().getClientId(), Main.cfg.getMixer().getToken());

    
    // FIXME TEMP
    private final DecoratedGatewayClient discord;
    
    private final Map<Integer, WebSocketClient<MixerEvent, MixerMethod>> mixer = new HashMap<>();
    private final WebSocketClient<IRCEvent, String> twitch;

    public DiscordCommandHandler(DecoratedGatewayClient client, String token, ObjectMapper mapper) {
        this.discord = client;
        this.helper = new DiscordRequestHelper(mapper, token);
        
//        System.out.println(mrh.post("/oauth/token/introspect", "{\"token\":\"" + config.getMixer().getToken() + "\"}", TokenIntrospectResponse.class).block());
        
        twitch = new SimpleWebSocketClient<>();
        twitch.connect("wss://irc-ws.chat.twitch.tv:443", new FrameParser<>(IRCEvent::parse, Function.identity()))
            .subscribe();
        
        twitch.outbound()
            .next("PASS oauth:" + Main.cfg.getTwitch().getToken())
            .next("NICK " + Main.cfg.getTwitch().getNick());
    }

    public void handle(long channel, long author, String... args) {

        if (args.length == 3 && args[0].equals("link")) {

            InputStream in = Main.class.getResourceAsStream("/logo.png");
            if (in == null) {
                throw new RuntimeException("Resource not found: logo.png");
            }
            
            Flux<Message> source;

            switch (args[1].toLowerCase(Locale.ROOT)) {
                case "twitch": 
                    source = new TwitchSource().connect(twitch, args[2]);
                    break;
                case "mixer":
                    int chan = Integer.parseInt(args[2]);
                    WebSocketClient<MixerEvent, MixerMethod> ws = new SimpleWebSocketClient<>();
                    mixer.put(chan, ws);
                    
                    MixerRequestHelper mrh = new MixerRequestHelper(new ObjectMapper(), Main.cfg.getMixer().getClientId(), Main.cfg.getMixer().getToken());
                    source = mrh.get("/chats/" + chan, ChatResponse.class)
                                .map(MixerSource::new)
                                .flatMapMany(ms -> ms.connect(ws, args[2]));
                    break;
                default:
                    throw new RuntimeException("Invalid service");
            }
            
            source.subscribe(m -> helper.getWebhook(channel, "ChatMux", in).subscribe(wh -> helper.executeWebhook(wh, "{\"content\":\"" + m + "\"}")));
        }
    }
}
