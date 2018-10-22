package com.tterrag.chatmux.bridge.mixer;

import javax.annotation.ParametersAreNonnullByDefault;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tterrag.chatmux.util.RequestHelper;

import io.netty.handler.codec.http.HttpHeaders;

@ParametersAreNonnullByDefault
public class MixerRequestHelper extends RequestHelper {
    
    private final String id, token;

    public MixerRequestHelper(ObjectMapper mapper, String id, String token) {
        super(mapper);
        this.id = id;
        this.token = token;
    }
    
    @Override
    protected String getBaseUrl() {
        return "https://mixer.com/api/v1";
    }
    
    @Override
    protected void addHeaders(HttpHeaders headers) {
        headers.add("Client-ID", id);
        headers.add("Authorization", "Bearer " + token);
        headers.add("Content-Type", "application/json");
        headers.add("User-Agent", "TwitchBot (https://tropicraft.net, 1.0)");
    }
}
