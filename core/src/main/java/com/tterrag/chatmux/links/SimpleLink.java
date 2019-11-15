package com.tterrag.chatmux.links;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tterrag.chatmux.api.bridge.ChatChannel;
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
    public Mono<String> prettyPrint() {
        return from.getService().prettifyChannel(from.getName())
                .zipWith(to.getService().prettifyChannel(to.getName()),
                        (fromName, toName) -> from.getService().getName() + "/" + fromName + " -> " + to.getService().getName() + "/" + toName + (raw ? " (raw)" : ""));
    }
}