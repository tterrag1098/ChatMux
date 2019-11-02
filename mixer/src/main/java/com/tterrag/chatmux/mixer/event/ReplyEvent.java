package com.tterrag.chatmux.mixer.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.ToString;

@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
public class ReplyEvent extends MixerEvent {
    
    public String error;
    
    public Object data;
    
    public int id;

}
