package com.tterrag.chatmux.bridge.mixer.response;

import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.ToString;

@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
public class UserResponse {

    public int id;
    
    public String username;
    
    public @Nullable String avatarUrl;

}

