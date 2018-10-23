package com.tterrag.chatmux.links;

import com.tterrag.chatmux.util.ServiceType;

import lombok.Value;

@Value
public class Message {
    
    ServiceType source;
    String channel;

    String user;
    String content;
    
    @Override
    public String toString() {
        return "[" + source + "/" + channel + "] <" + user + "> " + content;
    }
}
