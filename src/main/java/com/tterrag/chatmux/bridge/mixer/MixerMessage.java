package com.tterrag.chatmux.bridge.mixer;

import com.tterrag.chatmux.bridge.mixer.event.MixerEvent;
import com.tterrag.chatmux.bridge.mixer.method.MixerMethod;
import com.tterrag.chatmux.bridge.mixer.method.MixerMethod.MethodType;
import com.tterrag.chatmux.links.Message;
import com.tterrag.chatmux.util.ServiceType;
import com.tterrag.chatmux.websocket.WebSocketClient;

public class MixerMessage extends Message {
    
    private final MixerRequestHelper helper;
    private final WebSocketClient<?, MixerMethod> client;
    private final MixerEvent.Message message;

    public MixerMessage(MixerRequestHelper helper, WebSocketClient<?, MixerMethod> client, MixerEvent.Message message, String avatar) {
        super(ServiceType.MIXER, "" + message.channel, message.username, message.message.rawText(), avatar);
        this.helper = helper;
        this.client = client;
        this.message = message;
    }

    @Override
    public void delete() {
        client.outbound().next(new MixerMethod(MethodType.DELETE_MESSAGE, message.id.toString()));
    }

    @Override
    public void kick() {
        client.outbound().next(new MixerMethod(MethodType.PURGE, message.username));
    }

    @Override
    public void ban() {
        helper.ban(message.channel, message.userId);
    }
}
