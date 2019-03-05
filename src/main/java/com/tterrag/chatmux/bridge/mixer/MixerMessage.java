package com.tterrag.chatmux.bridge.mixer;

import com.tterrag.chatmux.bridge.mixer.event.MixerEvent;
import com.tterrag.chatmux.bridge.mixer.method.MixerMethod;
import com.tterrag.chatmux.bridge.mixer.method.MixerMethod.MethodType;
import com.tterrag.chatmux.links.Message;
import com.tterrag.chatmux.util.Service;
import com.tterrag.chatmux.websocket.WebSocketClient;

import reactor.core.publisher.Mono;

public class MixerMessage extends Message {
    
    private final MixerRequestHelper helper;
    private final WebSocketClient<?, MixerMethod> client;
    private final MixerEvent.Message message;

    public MixerMessage(MixerRequestHelper helper, WebSocketClient<?, MixerMethod> client, MixerEvent.Message message, String avatar) {
        super(Service.MIXER, "" + message.channel, message.username, message.message.rawText(), avatar);
        this.helper = helper;
        this.client = client;
        this.message = message;
    }

    @Override
    public Mono<Void> delete() {
        client.outbound().next(new MixerMethod(MethodType.DELETE_MESSAGE, message.id.toString()));
        return Mono.empty();
    }

    @Override
    public Mono<Void> kick() {
        client.outbound().next(new MixerMethod(MethodType.PURGE, message.username));
        return Mono.empty();
    }

    @Override
    public Mono<Void> ban() {
        return helper.ban(message.channel, message.userId);
    }
}
