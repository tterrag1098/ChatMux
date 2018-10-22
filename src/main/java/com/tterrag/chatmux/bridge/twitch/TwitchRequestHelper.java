package com.tterrag.chatmux.bridge.twitch;

import javax.annotation.ParametersAreNonnullByDefault;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tterrag.chatmux.bridge.twitch.response.TokenResponse;
import com.tterrag.chatmux.util.RequestHelper;

import io.netty.handler.codec.http.HttpHeaders;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@ParametersAreNonnullByDefault
@Slf4j
public class TwitchRequestHelper extends RequestHelper {
    
    private final String id, secret;
    
    private TokenResponse token;
    private long expiry;
    
    public TwitchRequestHelper(ObjectMapper mapper, String id, String secret) {
        super(mapper);
        this.id = id;
        this.secret = secret;
    }
    
    @Override
    protected String getBaseUrl() {
        return "https://id.twitch.tv/";
    }
    
    @Override
    protected void addHeaders(HttpHeaders headers) {
        TokenResponse token = getToken();
        headers.add("Authorization", token.type + " " + token);
        headers.add("Content-Type", "application/json");
        headers.add("User-Agent", "TwitchBot (https://tropicraft.net, 1.0)");
    }
    
    public TokenResponse getToken() {
        if (token == null || expiry <= System.currentTimeMillis()) {
            token = requestToken().block(); // FIXME
        }
        return token;
    }
    
    private Mono<TokenResponse> requestToken() {
        return get("/oauth2/token" + 
                   "?client_id=" + id + 
                   "&client_secret=" + secret + 
                   "&grant_type=client_credentials",
                   TokenResponse.class);
    }
}
