package com.tterrag.chatmux.bridge.mixer.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TokenResponse {
    
    @JsonProperty("access_token")
    public String token;
    
    @JsonProperty("expires_in")
    public long expiresIn;

    @JsonProperty("scope")
    public String scopes;
    
    @JsonProperty("token_type")
    public String type;
}
