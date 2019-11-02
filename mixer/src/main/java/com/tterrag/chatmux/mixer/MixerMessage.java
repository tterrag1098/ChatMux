package com.tterrag.chatmux.mixer;

import java.util.UUID;

import com.tterrag.chatmux.bridge.AbstractChatMessage;
import com.tterrag.chatmux.mixer.event.MixerEvent;
import com.tterrag.chatmux.mixer.event.reply.MessageReply;
import com.tterrag.chatmux.mixer.method.MixerMethod;
import com.tterrag.chatmux.mixer.method.MixerMethod.MethodType;
import com.tterrag.chatmux.websocket.WebSocketClient;

import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;

public class MixerMessage extends AbstractChatMessage<MixerMessage> {
    
    private final MixerRequestHelper helper;
    private final WebSocketClient<?, MixerMethod> client;
    private final UUID id;
    private final int userId;

    public MixerMessage(MixerRequestHelper helper, WebSocketClient<?, MixerMethod> client, MixerEvent.Message message, String channelName, @Nullable String avatar) {
        this(helper, client, message.id, message.userId, channelName, message.channel, message.username, message.message.rawText(), avatar);
    }
    
    public MixerMessage(MixerRequestHelper helper, WebSocketClient<?, MixerMethod> client, MessageReply message, String channelName) {
        this(helper, client, message.id, message.userId, channelName, message.channel, message.username, message.message.rawText(), message.userAvatar);
    }
    
    public MixerMessage(MixerRequestHelper helper, WebSocketClient<?, MixerMethod> client, UUID messageId, int userId, String channelName, int channelId, String username, String content, @Nullable String avatar) {
        super(MixerService.getInstance(), channelName, "" + channelId, username, content, avatar);
        this.helper = helper;
        this.client = client;
        this.id = messageId;
        this.userId = userId;
    }

    @Override
    public Mono<Void> delete() {
        client.outbound().next(new MixerMethod(MethodType.DELETE_MESSAGE, id.toString()));
        return Mono.empty();
    }

    @Override
    public Mono<Void> kick() {
        client.outbound().next(new MixerMethod(MethodType.PURGE, getUser()));
        return Mono.empty();
    }

    @Override
    public Mono<Void> ban() {
        return helper.ban(Integer.parseInt(getChannelId()), userId);
    }
}
