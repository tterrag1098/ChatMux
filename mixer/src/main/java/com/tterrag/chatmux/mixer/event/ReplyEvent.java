package com.tterrag.chatmux.mixer.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tterrag.chatmux.mixer.event.reply.ReplyData;

import lombok.ToString;

@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
public class ReplyEvent extends MixerEvent {
    
    public String error;
    
    public JsonNode data;
    
    public int id;

    public <T extends ReplyData> T getData(ObjectMapper mapper, Class<? extends T> cls) {
        return mapper.convertValue(data, cls);
    }
}
