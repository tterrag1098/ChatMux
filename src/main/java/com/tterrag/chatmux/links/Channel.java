package com.tterrag.chatmux.links;

import com.tterrag.chatmux.util.ServiceType;

import lombok.Value;

@Value
public class Channel<I, O> {

    String name;
    ServiceType<I, O> type;
    
    @Override
    public String toString() {
        return type.toString() + "/" + name;
    }

}
