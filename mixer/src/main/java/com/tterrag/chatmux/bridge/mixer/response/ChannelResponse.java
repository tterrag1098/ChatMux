package com.tterrag.chatmux.bridge.mixer.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.ToString;

@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
public class ChannelResponse {

    public int id;
    
    public int userId;
    
    public String token;
    
    public String name;

}
