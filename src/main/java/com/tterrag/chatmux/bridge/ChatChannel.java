package com.tterrag.chatmux.links;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tterrag.chatmux.util.Service;

import lombok.Value;

@Value
public class Channel<I, O> {

    String name;
    Service<I, O> type;
    
    @JsonCreator
    public Channel(@JsonProperty("name") String name, @JsonProperty("type") Service<I, O> type) {
        this.name = name;
        this.type = type;
    }
    
    @Override
    public String toString() {
        return type.toString() + "/" + name;
    }

}
