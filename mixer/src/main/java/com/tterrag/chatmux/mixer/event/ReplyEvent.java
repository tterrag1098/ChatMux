package com.tterrag.chatmux.mixer.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.ToString;

@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
public class ReplyEvent extends MixerEvent {
    
    public String error;
    
    public JsonNode data;
    
    public int id;

}
