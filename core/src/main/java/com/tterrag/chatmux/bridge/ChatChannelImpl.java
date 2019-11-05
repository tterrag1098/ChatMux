package com.tterrag.chatmux.bridge;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tterrag.chatmux.api.bridge.ChatChannel;
import com.tterrag.chatmux.api.bridge.ChatMessage;

import lombok.Getter;
import lombok.Value;
import reactor.core.publisher.Flux;

@Value
public final class ChatChannelImpl<M extends ChatMessage<M>> implements ChatChannel<M> {

    @Getter(onMethod = @__({@Override}))
    String name;
    @Getter(onMethod = @__({@Override}))
    AbstractChatService<M, ?> service;
    
    @JsonCreator
    public ChatChannelImpl(@JsonProperty("name") String name, @JsonProperty("type") AbstractChatService<M, ?> service) {
        this.name = name;
        this.service = service;
    }
    
    @Override
    public Flux<M> connect() {
        return getService().getSource().connect(getName());
    }
    
    @Override
    public String toString() {
        return service.toString() + "/" + name;
    }
}
