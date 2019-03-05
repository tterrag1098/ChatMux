package com.tterrag.chatmux.bridge;

import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.experimental.NonFinal;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;

@Value
@NonFinal
@RequiredArgsConstructor
public abstract class ChatMessage {
    
    ChatService<?, ?> source;
    String channel;
    String channelId;

    String user;
    String content;
    
    @Nullable String avatar;
    
    protected ChatMessage(ChatService<?, ?> type, String channel, String user, String content, @Nullable String avatar) {
        this(type, channel, channel, user, content, avatar);
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
        return "[" + getSource() + "/" + getChannel() + "] <" + getUser() + "> " + getContent();
    }
}
