package com.tterrag.chatmux.bridge.mixer.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ReplyEvent extends MixerEvent {
    
    public String error;
    
    public Object data;
    
    public int id;

}
