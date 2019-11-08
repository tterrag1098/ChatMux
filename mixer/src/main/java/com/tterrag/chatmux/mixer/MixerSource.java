package com.tterrag.chatmux.mixer;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import com.tterrag.chatmux.api.bridge.ChatMessage;
import com.tterrag.chatmux.api.bridge.ChatSource;
import com.tterrag.chatmux.api.websocket.WebSocketClient;
import com.tterrag.chatmux.mixer.event.MixerEvent;
import com.tterrag.chatmux.mixer.event.ReplyEvent;
import com.tterrag.chatmux.mixer.event.reply.MessageReply;
import com.tterrag.chatmux.mixer.method.MixerMethod;
import com.tterrag.chatmux.mixer.method.MixerMethod.MethodType;
import com.tterrag.chatmux.mixer.response.ChatResponse;
import com.tterrag.chatmux.util.reactor.Monos;
import com.tterrag.chatmux.websocket.SimpleFrameParser;
import com.tterrag.chatmux.websocket.SimpleWebSocketClient;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.annotation.NonNull;

@RequiredArgsConstructor
@Slf4j
public class MixerSource implements ChatSource<MixerMessage> {
    
    @NonNull
    @Getter(AccessLevel.PACKAGE)
    private final MixerRequestHelper helper;
    
    @NonNull
    private final Map<Integer, Mono<WebSocketClient<MixerEvent, MixerMethod>>> mixer = new ConcurrentHashMap<>();
    @NonNull
    private final Map<Integer, Disposable> connections = new ConcurrentHashMap<>();
    @NonNull // Mixer API is super clever and doesn't send the method name back in the reply, so we have to track it ourselves
    private final Map<Integer, MethodType> sentMethods = new ConcurrentHashMap<>();
    @NonNull // It also sends us message events for messages WE sent (unlike twitch), so, more special handling
    private final Set<UUID> sentMessages = Sets.newConcurrentHashSet();
    
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public MixerService getType() {
        return MixerService.getInstance();
    }
    
    @Override
    public Flux<MixerMessage> connect(String channel) {
        return getClient(channel)
            .flatMapMany(client -> client.inbound()
                    .log(log.getName())
                    .ofType(MixerEvent.Message.class)
                    .filter(m -> !sentMessages.remove(m.id))
                    .flatMap(e -> helper.getUser(e.userId)
                            .zipWith(helper.getChannel(e.channel).flatMap(c -> helper.getUser(c.userId)),
                                    (sender, channelOwner) -> new MixerMessage(helper, client, e, channelOwner.username, sender.avatarUrl))))
            .doOnError(t -> log.error("Failed to receive from mixer", t))
            .doOnComplete(() -> log.error("Mixer connection completed"));
    }
    
    @Override
    public Mono<MixerMessage> send(String channel, ChatMessage<?> message, boolean raw) {
        return getClient(channel)
                .doOnNext(client -> client.outbound().next(new MixerMethod(MethodType.MESSAGE, (raw ? message.getContent() : message.toString()))
                        .saveId(sentMethods::put)))
                .flatMapMany(WebSocketClient::inbound)
                .ofType(ReplyEvent.class)
                .filter(re -> getMethodType(re.id) == MethodType.MESSAGE)
                .next()
                .map(re -> re.getData(mapper, MessageReply.class))
                .doOnNext(m -> sentMessages.add(m.id))
                .doOnError(t -> log.error("Failed to send to mixer", t))
                .flatMap(m -> getClient(channel).zipWith(helper.getChannel(m.userId).flatMap(c -> helper.getUser(c.userId)), 
                        (client, c) -> new MixerMessage(helper, client, m, c.username)));
    }
    
    private Mono<WebSocketClient<MixerEvent, MixerMethod>> getClient(String channel) {
        int chan = Integer.parseInt(channel);
        return mixer.computeIfAbsent(chan, c -> {
            final WebSocketClient<MixerEvent, MixerMethod> socket = new SimpleWebSocketClient<>();
            MixerRequestHelper mrh = new MixerRequestHelper(new ObjectMapper(), MixerService.getInstance().getData().getClientId(), MixerService.getInstance().getData().getToken());
            return mrh.get("/chats/" + chan, ChatResponse.class)
                .doOnNext(chat -> connections.put(chan, socket.connect(chat.endpoints[0], new SimpleFrameParser<>(MixerEvent::parse, new ObjectMapper()))
                        .subscribe($ -> {}, t -> log.error("Exception handling mixer chat", t), () -> log.error("Mixer chat handler completed"))))
                .doOnNext(chat -> socket.outbound().next(new MixerMethod(MethodType.AUTH, chan, MixerService.getInstance().getData().getUserId(), chat.authKey)
                        .saveId(sentMethods::put)))
                .thenReturn(socket)
                // Make sure auth response completes before anything else
                .flatMap(ws -> ws.inbound().ofType(ReplyEvent.class).filter(re -> getMethodType(re.id) == MethodType.AUTH)
                        .next()
                        .transform(Monos.precondition(re -> re.error == null, re -> new IllegalStateException("Failed to authenticate mixer: " + re.error)))
                        .transform(Monos.precondition(re -> re.data.get("authenticated").asBoolean(false), "Mixer returned not authenticated!"))
                        .thenReturn(ws))
                .cache();
        });
    }

    @Override
    public void disconnect(String channel) {
        Integer chan = Integer.parseInt(channel);
        Mono<WebSocketClient<MixerEvent, MixerMethod>> ws = mixer.get(chan);
        if (ws != null) {
            connections.remove(chan).dispose();
            mixer.remove(chan);
        }
    }
    
    private MethodType getMethodType(int id) {
        MethodType res = sentMethods.remove(id);
        if (res == null) {
            throw new IllegalStateException("Received reply with unknown ID: " + id);
        }
        return res;
    }
}