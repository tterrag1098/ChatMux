package com.tterrag.chatmux.bridge.discord;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Value;

@Value
public class WebhookMessage {
    
    private static final ObjectMapper MAPPER = new ObjectMapper();
    
    String content;
    String username;
    String avatar_url;
    
    @Override
    public String toString() {
        try {
            return MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return "Error";
        }
    }
}
