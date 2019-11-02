package com.tterrag.chatmux.mixer;

import com.tterrag.chatmux.bridge.AbstractChatMessage;
import com.tterrag.chatmux.mixer.event.MixerEvent;
import com.tterrag.chatmux.mixer.method.MixerMethod;
import com.tterrag.chatmux.mixer.method.MixerMethod.MethodType;
import com.tterrag.chatmux.websocket.WebSocketClient;

import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;

public class MixerMessage extends AbstractChatMessage<MixerMessage> {
    
    private final MixerRequestHelper helper;
    private final WebSocketClient<?, MixerMethod> client;
    private final MixerEvent.Message message;

    public MixerMessage(MixerRequestHelper helper, WebSocketClient<?, MixerMethod> client, MixerEvent.Message message, String channelName, @Nullable String avatar) {
        super(MixerService.getInstance(), channelName, "" + message.channel, message.username, message.message.rawText(), avatar);
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
