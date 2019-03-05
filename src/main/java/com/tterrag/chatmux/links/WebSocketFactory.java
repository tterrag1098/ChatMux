package com.tterrag.chatmux.links;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.tterrag.chatmux.Main;
import com.tterrag.chatmux.bridge.ChatService;
import com.tterrag.chatmux.bridge.factorio.FactorioClient;
import com.tterrag.chatmux.bridge.factorio.FactorioMessage;
import com.tterrag.chatmux.bridge.mixer.MixerRequestHelper;
import com.tterrag.chatmux.bridge.mixer.event.MixerEvent;
import com.tterrag.chatmux.bridge.mixer.method.MixerMethod;
import com.tterrag.chatmux.bridge.mixer.method.MixerMethod.MethodType;
import com.tterrag.chatmux.bridge.mixer.response.ChatResponse;
import com.tterrag.chatmux.bridge.twitch.irc.IRCEvent;
import com.tterrag.chatmux.websocket.DecoratedGatewayClient;
import com.tterrag.chatmux.websocket.FrameParser;
import com.tterrag.chatmux.websocket.SimpleWebSocketClient;
import com.tterrag.chatmux.websocket.WebSocketClient;

import discord4j.gateway.json.GatewayPayload;
import discord4j.gateway.json.dispatch.Dispatch;
import reactor.core.Disposable;

public abstract class WebSocketFactory<I, O> {
    
    private static final Map<ChatService<?, ?>, WebSocketFactory<?, ?>> factoriesByType = ImmutableMap.<ChatService<?, ?>, WebSocketFactory<?, ?>>builder()
            .put(ChatService.DISCORD, new Discord())
            .put(ChatService.TWITCH, new Twitch())
            .put(ChatService.MIXER, new Mixer())
            .put(ChatService.FACTORIO, new Factorio())
            .build();
    
    public abstract WebSocketClient<I, O> getSocket(String channel);
    
    public abstract void disposeSocket(String channel);
    
    @SuppressWarnings("unchecked")
    public static <I, O> WebSocketFactory<I, O> get(ChatService<I, O> type) {
        WebSocketFactory<I, O> ret = (WebSocketFactory<I, O>) factoriesByType.get(type);
        if (ret == null) {
            throw new IllegalArgumentException("Unknown service type");
        }
        return ret;
    }
    
    private static class Discord extends WebSocketFactory<Dispatch, GatewayPayload<?>> {
        
        private final DecoratedGatewayClient discord = new DecoratedGatewayClient();

        @Override
        public WebSocketClient<Dispatch, GatewayPayload<?>> getSocket(String channel) {
            return discord;
        }

        @Override
        public void disposeSocket(String channel) {}
    }
    
    private static class Mixer extends WebSocketFactory<MixerEvent, MixerMethod> {
        private final Map<Integer, WebSocketClient<MixerEvent, MixerMethod>> mixer = new HashMap<>();
        private final Map<Integer, Disposable> connections = new HashMap<>();

        @Override
        public WebSocketClient<MixerEvent, MixerMethod> getSocket(String channel) {
            int chan = Integer.parseInt(channel);
            WebSocketClient<MixerEvent, MixerMethod> ws = mixer.get(chan);
            if (ws == null) {
                final WebSocketClient<MixerEvent, MixerMethod> socket = new SimpleWebSocketClient<>();
                ws = socket;
                mixer.put(chan, ws);
                MixerRequestHelper mrh = new MixerRequestHelper(new ObjectMapper(), Main.cfg.getMixer().getClientId(), Main.cfg.getMixer().getToken());
                mrh.get("/chats/" + chan, ChatResponse.class)
                    .doOnNext(chat -> connections.put(chan, socket.connect(chat.endpoints[0], new FrameParser<>(MixerEvent::parse, new ObjectMapper())).subscribe()))
                    .subscribe(chat -> socket.outbound().next(new MixerMethod(MethodType.AUTH, Integer.parseInt(channel), Main.cfg.getMixer().getUserId(), chat.authKey)));
            }
            return ws;
        }

        @Override
        public void disposeSocket(String channel) {
            Integer chan = Integer.parseInt(channel);
            WebSocketClient<MixerEvent, MixerMethod> ws = mixer.get(chan);
            if (ws != null) {
                connections.remove(chan).dispose();
                mixer.remove(chan);
            }
        }
    }
    
    private static class Twitch extends WebSocketFactory<IRCEvent, String> {
        
        private boolean connected;

        private final WebSocketClient<IRCEvent, String> twitch = new SimpleWebSocketClient<>();

        @Override
        public WebSocketClient<IRCEvent, String> getSocket(String channel) {
            if (!connected) {
                twitch.connect("wss://irc-ws.chat.twitch.tv:443", new FrameParser<>(IRCEvent::parse, Function.identity()))
                    .subscribe();
                
                twitch.outbound()
                    .next("PASS oauth:" + Main.cfg.getTwitch().getToken())
                    .next("NICK " + Main.cfg.getTwitch().getNick())
                    .next("CAP REQ :twitch.tv/tags")
                    .next("CAP REQ :twitch.tv/commands");
                connected = true;
            }
            return twitch;
        }

        @Override
        public void disposeSocket(String channel) {
            twitch.outbound().next("PART #" + channel);
        }
    }
    
    private static class Factorio extends WebSocketFactory<FactorioMessage, String> {

        private boolean connected;

        private final FactorioClient factorio = new FactorioClient(new File(Main.cfg.getFactorio().getInput()), new File(Main.cfg.getFactorio().getOutput()));

        @Override
        public WebSocketClient<FactorioMessage, String> getSocket(String channel) {
            if (!connected) {
                factorio.connect().subscribe();
                connected = true;
            }
            return factorio;
        }

        @Override
        public void disposeSocket(String channel) {
            
        }
    }
}
