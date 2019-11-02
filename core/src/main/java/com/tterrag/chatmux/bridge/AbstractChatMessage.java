package com.tterrag.chatmux.bridge;

import com.tterrag.chatmux.api.bridge.ChatMessage;
import com.tterrag.chatmux.api.bridge.ChatService;

import lombok.Getter;
import lombok.Value;
import lombok.experimental.NonFinal;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;

@Value
@NonFinal
public abstract class AbstractChatMessage<M extends ChatMessage<M>> implements ChatMessage<M> {
    
    @Getter(onMethod = @__({@Override}))
    ChatService<M> service;
    @Getter(onMethod = @__({@Override}))
    String channel;
    @Getter(onMethod = @__({@Override}))
    String channelId;

    @Getter(onMethod = @__({@Override}))
    String user;
    @Getter(onMethod = @__({@Override}))
    String content;
    
    @Nullable String avatar;
    
    protected AbstractChatMessage(ChatService<M> type, String channel, String user, String content, @Nullable String avatar) {
        this(type, channel, channel, user, content, avatar);
    }
    
    protected AbstractChatMessage(ChatService<M> type, String channel, String channelId, String user, String content, @Nullable String avatar) {
        this.service = type;
        this.channel = channel;
        this.channelId = channelId;
        this.user = user;
        this.content = content;
        this.avatar = avatar;
    }

    /**
     * Deletes the current message, exact behavior is up to the specific service.
     */
    public abstract Mono<Void> delete();
    
    /**
     * Kicks the user. Exact behavior may vary, for instance on twitch this equates to a "purge".
     */
    public abstract Mono<Void> kick();
    
    /**
     * Ban the author of this message
     */
    public abstract Mono<Void> ban();
    
    @Override
    public String toString() {
        return "[" + getService() + "/" + getChannel() + "] <" + getUser() + "> " + getContent();
    }
}
