package com.tterrag.chatmux.bridge;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tterrag.chatmux.api.bridge.ChatChannel;
import com.tterrag.chatmux.api.bridge.ChatMessage;
import com.tterrag.chatmux.api.bridge.ChatService;

import lombok.Getter;
import lombok.Value;

@Value
public final class ChatChannelImpl<M extends ChatMessage<M>> implements ChatChannel<M> {

    @Getter(onMethod = @__({@Override}))
    String name;
    @Getter(onMethod = @__({@Override}))
    ChatService<M> service;
    
    @JsonCreator
    public ChatChannelImpl(@JsonProperty("name") String name, @JsonProperty("service") AbstractChatService<M, ?> service) {
        this.name = name;
        this.service = service;
    }

    @Override
    public String toString() {
        return service.toString() + "/" + name;
    }
}
