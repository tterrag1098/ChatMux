package com.tterrag.chatmux.mixer.event;

import java.io.IOException;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MixerEvent {

    public static MixerEvent parse(String raw) {
        ObjectMapper om = new ObjectMapper();
        try {
            JsonNode data = om.readTree(raw);
            String type = data.get("type").asText();
            if (type.equals("reply")) {
                return om.readValue(raw, ReplyEvent.class);
            } else if (type.equals("event")) {
                String event = data.get("event").asText();
                if (event.equals("AbstractChatMessage")) {
                    return om.readValue(om.writeValueAsString(data.get("data")), Message.class);
                } else if (event.equals("WelcomeEvent")) {
                    return om.readValue(om.writeValueAsString(data.get("data")), Welcome.class);
                }
            }
        } catch (IOException e) {
            log.error("Exception parsing MixerEvent", e);
            throw new RuntimeException(e);
        }
        return new Unknown();
    }
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    @ToString
    public static class Message extends MixerEvent {
        
        public int channel;
        
        public UUID id;
        
        @JsonProperty("user_name")
        public String username;

        @JsonProperty("user_id")
        public int userId;
        
        public com.tterrag.chatmux.mixer.event.object.Message message;
    }
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    @ToString
    public static class Welcome extends MixerEvent {
        
        public UUID server;
    }
    
    @ToString
    public static class Unknown extends MixerEvent {}
}
