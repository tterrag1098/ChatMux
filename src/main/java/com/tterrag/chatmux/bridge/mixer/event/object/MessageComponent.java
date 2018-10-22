package com.tterrag.chatmux.bridge.mixer.event.object;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageComponent {
    
    public enum MessageType {
        text,
        link,
        emoticon,
        tag, // ?
        ;
    }
    
    public MessageType type;
    
    public String text;

}
