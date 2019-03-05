package com.tterrag.chatmux.bridge;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Value;

@Value
public class ChatChannel<I, O> {

    String name;
    ChatService<I, O> type;
    
    @JsonCreator
    public ChatChannel(@JsonProperty("name") String name, @JsonProperty("type") ChatService<I, O> type) {
        this.name = name;
        this.type = type;
    }
    
    @Override
    public String toString() {
        return type.toString() + "/" + name;
    }

}
