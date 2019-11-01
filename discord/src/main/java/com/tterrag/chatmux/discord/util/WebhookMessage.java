package com.tterrag.chatmux.discord.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Value
@Slf4j
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
            log.error("Exception writing webhook message to JSON", e);
            return "Error";
        }
    }
}
