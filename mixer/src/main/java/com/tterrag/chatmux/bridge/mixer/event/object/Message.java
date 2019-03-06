package com.tterrag.chatmux.bridge.mixer.event.object;

import java.util.Arrays;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Message {
    
    @JsonProperty("message")
    MessageComponent[] components;
    
    public String rawText() {
        return Arrays.stream(components).map(c -> c.text).collect(Collectors.joining());
    }

}
