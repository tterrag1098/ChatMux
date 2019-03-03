package com.tterrag.chatmux.bridge.mixer.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.ToString;
import reactor.util.annotation.Nullable;

@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
public class UserResponse {

    public int id;
    
    public String username;
    
    public @Nullable String avatarUrl;

}

