package com.tterrag.chatmux.bridge;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Value;
import reactor.core.publisher.Flux;

@Value
public class ChatChannel<I, O> {

    String name;
    ChatService<I, O> type;
    
    @JsonCreator
    public ChatChannel(@JsonProperty("name") String name, @JsonProperty("type") ChatService<I, O> type) {
        this.name = name;
        this.type = type;
    }
    
    public Flux<? extends ChatMessage> connect() {
        return getType().getSource().connect(getName());
    }
    
    @Override
    public String toString() {
        return type.toString() + "/" + name;
    }
}
