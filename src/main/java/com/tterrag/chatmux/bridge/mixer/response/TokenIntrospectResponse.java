package com.tterrag.chatmux.bridge.mixer.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.ToString;

@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
public class TokenIntrospectResponse {
    
    public boolean active;
    
    public String scopes;
    
    public String client_id;
    
    public String username;
    
    public String token_type;
}
