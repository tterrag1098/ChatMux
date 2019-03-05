package com.tterrag.chatmux.bridge.mixer;

import com.tterrag.chatmux.bridge.mixer.event.MixerEvent;
import com.tterrag.chatmux.bridge.mixer.method.MixerMethod;
import com.tterrag.chatmux.links.ChatSource;
import com.tterrag.chatmux.links.Message;
import com.tterrag.chatmux.util.Service;
import com.tterrag.chatmux.websocket.WebSocketClient;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

@RequiredArgsConstructor
public
class MixerSource implements ChatSource<MixerEvent, MixerMethod> {
    
    private final MixerRequestHelper helper;

    @Override
    public Service<MixerEvent, MixerMethod> getType() {
        return Service.MIXER;
    }
    
    @Override
    public Flux<Message> connect(WebSocketClient<MixerEvent, MixerMethod> client, String channel) {
        return client.inbound()
            .ofType(MixerEvent.Message.class)
            .flatMap(e -> helper.getUser(e.userId)
                                .map(u -> new MixerMessage(helper, client, e, u.avatarUrl)));
    }
}