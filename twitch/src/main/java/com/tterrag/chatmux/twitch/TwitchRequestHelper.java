package com.tterrag.chatmux.twitch;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tterrag.chatmux.twitch.response.UserResponse;
import com.tterrag.chatmux.util.http.RequestHelper;

import io.netty.handler.codec.http.HttpHeaders;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
public class TwitchRequestHelper extends RequestHelper {
    
    private final String token;
    
    public TwitchRequestHelper(ObjectMapper mapper, String token) {
        super(mapper, "https://api.twitch.tv/helix");
        this.token = token;
    }

    @Override
    protected void addHeaders(HttpHeaders headers) {
        headers.add("Client-ID", "q6batx0epp608isickayubi39itsckt");
        headers.add("Content-Type", "application/json");
        headers.add("User-Agent", "TwitchBot (https://tropicraft.net, 1.0)");
        headers.add("Accept", "application/vnd.twitchtv.v5+json");
        headers.add("Authorization", "Bearer " + TwitchService.getInstance().getData().getTokenReceive());
    }
    
    public Mono<UserResponse> getUser(int id) {
        return get("/users/" + id, UserResponse.class);
    }

    public Mono<UserResponse> getUser(String login) {
        return getUsers(login).next();
    }
    
    public Flux<UserResponse> getUsers(String... logins) {
        String args = "?" + Arrays.stream(logins).map(s -> "login=" + s)
                  .collect(Collectors.joining("&"));
        
        return get("/users" + args, JsonNode.class)
                .map(node -> node.get("data"))
                .map(node -> {
                    try {
                        return mapper.readValue(node.toString(), UserResponse[].class);
                    } catch (IOException e) {
                        throw new IllegalArgumentException("Error reading JSON", e);
                    }
                })
                .flatMapMany(Flux::fromArray);
    }
    
//    public TokenResponse getToken() {
//        if (token == null || expiry <= System.currentTimeMillis()) {
//            token = requestToken().block(); // FIXME
//        }
//        return token;
//    }
//    
//    private Mono<TokenResponse> requestToken() {
//        return get("/oauth2/token" + 
//                   "?client_id=" + id + 
//                   "&client_secret=" + secret + 
//                   "&grant_type=client_credentials",
//                   TokenResponse.class);
//    }
}
