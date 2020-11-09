package com.tterrag.chatmux.links;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tterrag.chatmux.api.bridge.ChatChannel;
import com.tterrag.chatmux.api.bridge.ChatService;
import com.tterrag.chatmux.api.link.Link;
import com.tterrag.chatmux.bridge.ChatChannelImpl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;

@Value
@RequiredArgsConstructor
public class SimpleLink implements Link {
  
    @Getter(onMethod = @__({@Override}))
    ChatChannel<?> from, to;

    @Getter(onMethod = @__({@Override}))
    boolean raw;

    @JsonIgnore
    @Nullable
    @Getter(onMethod = @__({@Override}))
    Disposable subscription;
            
    @JsonCreator
    SimpleLink(@JsonProperty("from") ChatChannelImpl<?> from, @JsonProperty("to") ChatChannelImpl<?> to, @JsonProperty("raw") boolean raw) {
        this((ChatChannel<?>) from, to, raw);
    }
    
    SimpleLink(ChatChannel<?> from, ChatChannel<?> to, boolean raw) {
        this(from, to, raw, null);
    }
    
    @Override
    public String toString() {
        return from + " -> " + to + (raw ? " (raw)" : "");
    }
    
    @Override
    public Mono<String> prettyPrint(ChatService<?> target) {
        return from.getService().prettifyChannel(target, from)
                .zipWith(to.getService().prettifyChannel(target, to),
                        (fromName, toName) -> fromName + " -> " + toName + (raw ? " (raw)" : ""));
    }
}