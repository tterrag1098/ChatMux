package com.tterrag.chatmux.mixer;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tterrag.chatmux.Main;
import com.tterrag.chatmux.bridge.ChatMessage;
import com.tterrag.chatmux.bridge.ChatService;
import com.tterrag.chatmux.bridge.ChatSource;
import com.tterrag.chatmux.mixer.event.MixerEvent;
import com.tterrag.chatmux.mixer.method.MixerMethod;
import com.tterrag.chatmux.mixer.method.MixerMethod.MethodType;
import com.tterrag.chatmux.mixer.response.ChatResponse;
import com.tterrag.chatmux.websocket.FrameParser;
import com.tterrag.chatmux.websocket.SimpleWebSocketClient;
import com.tterrag.chatmux.websocket.WebSocketClient;

import lombok.RequiredArgsConstructor;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.annotation.NonNull;

@RequiredArgsConstructor
public
class MixerSource implements ChatSource<MixerEvent, MixerMethod> {
    
    @NonNull
    private final MixerRequestHelper helper;
    
    @NonNull
    private final Map<Integer, WebSocketClient<MixerEvent, MixerMethod>> mixer = new HashMap<>();
    @NonNull
    private final Map<Integer, Disposable> connections = new HashMap<>();

    @Override
    public ChatService<MixerEvent, MixerMethod> getType() {
        return MixerService.getInstance();
    }
    
    @Override
    public Mono<String> parseChannel(String channel) {
        try {
            Integer.parseInt(channel);
            return Mono.just(channel);
        } catch (NumberFormatException e) {
            return helper.getChannel(channel).map(c -> c.id).map(c -> Integer.toString(c));
        }
    }
    
    @Override
    public Flux<ChatMessage> connect(String channel) {
        return getClient(channel).inbound()
            .ofType(MixerEvent.Message.class)
            .flatMap(e -> helper.getUser(e.userId)
                                .map(u -> new MixerMessage(helper, getClient(channel), e, u.avatarUrl)));
    }
    
    @Override
    public Mono<Void> send(String channel, ChatMessage message, boolean raw) {
        return Mono.fromSupplier(() -> getClient(channel).outbound())
                .doOnNext(sink -> sink.next(new MixerMethod(MethodType.MESSAGE, (raw ? message.getContent() : message.toString()))))
                .then();
    }
    
    private WebSocketClient<MixerEvent, MixerMethod> getClient(String channel) {
        int chan = Integer.parseInt(channel);
        WebSocketClient<MixerEvent, MixerMethod> ws = mixer.computeIfAbsent(chan, c -> {
            final WebSocketClient<MixerEvent, MixerMethod> socket = new SimpleWebSocketClient<>();
            MixerRequestHelper mrh = new MixerRequestHelper(new ObjectMapper(), Main.cfg.getMixer().getClientId(), Main.cfg.getMixer().getToken());
            mrh.get("/chats/" + chan, ChatResponse.class)
                .doOnNext(chat -> connections.put(chan, socket.connect(chat.endpoints[0], new FrameParser<>(MixerEvent::parse, new ObjectMapper())).subscribe()))
                .subscribe(chat -> socket.outbound().next(new MixerMethod(MethodType.AUTH, chan, Main.cfg.getMixer().getUserId(), chat.authKey)));
            return socket;
        });
        return ws;
    }

    @Override
    public void disconnect(String channel) {
        Integer chan = Integer.parseInt(channel);
        WebSocketClient<MixerEvent, MixerMethod> ws = mixer.get(chan);
        if (ws != null) {
            connections.remove(chan).dispose();
            mixer.remove(chan);
        }
    }
}