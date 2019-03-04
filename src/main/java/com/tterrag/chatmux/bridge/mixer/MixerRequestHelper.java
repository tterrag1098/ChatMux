package com.tterrag.chatmux.bridge.mixer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tterrag.chatmux.bridge.mixer.method.MixerRole;
import com.tterrag.chatmux.bridge.mixer.response.ChannelResponse;
import com.tterrag.chatmux.bridge.mixer.response.UserResponse;
import com.tterrag.chatmux.util.RequestHelper;

import io.netty.handler.codec.http.HttpHeaders;
import reactor.core.publisher.Mono;

public class MixerRequestHelper extends RequestHelper {
    
    private final String id, token;

    public MixerRequestHelper(ObjectMapper mapper, String id, String token) {
        super(mapper, "https://mixer.com/api/v1");
        this.id = id;
        this.token = token;
    }
    
    @Override
    protected void addHeaders(HttpHeaders headers) {
        headers.add("Client-ID", id);
        headers.add("Authorization", "Bearer " + token);
        headers.add("Content-Type", "application/json");
        headers.add("User-Agent", "TwitchBot (https://tropicraft.net, 1.0)");
    }
    
    public Mono<Void> addRoles(int channel, int userId, MixerRole... roles) {
        return patch("/channels/" + channel + "/users/" + userId, roles);
    }

    public Mono<Void> ban(int channel, int userId) {
        return addRoles(channel, userId, MixerRole.BANNED);
    }
    
    public Mono<ChannelResponse> getChannel(int id) {
        return getChannel(Integer.toString(id));
    }
    
    public Mono<ChannelResponse> getChannel(String tokenOrID) {
        return get("/channels/" + tokenOrID, ChannelResponse.class);
    }

    public Mono<UserResponse> getUser(int id) {
        return get("/users/" + id, UserResponse.class);
    }
}
