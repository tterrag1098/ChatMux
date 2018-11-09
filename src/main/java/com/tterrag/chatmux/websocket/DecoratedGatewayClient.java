package com.tterrag.chatmux.websocket;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.time.Duration;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.tterrag.chatmux.Main;

import discord4j.common.jackson.PossibleModule;
import discord4j.common.jackson.UnknownPropertyHandler;
import discord4j.gateway.GatewayClient;
import discord4j.gateway.IdentifyOptions;
import discord4j.gateway.TokenBucket;
import discord4j.gateway.json.GatewayPayload;
import discord4j.gateway.json.dispatch.Dispatch;
import discord4j.gateway.payload.JacksonPayloadReader;
import discord4j.gateway.payload.JacksonPayloadWriter;
import discord4j.gateway.payload.PayloadReader;
import discord4j.gateway.payload.PayloadWriter;
import discord4j.gateway.retry.RetryOptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

public class DecoratedGatewayClient implements WebSocketClient<Dispatch, GatewayPayload<?>> {
    
    private final GatewayClient wrapped;
    private final String endpoint;
    
    public DecoratedGatewayClient() {
        try {
            // build the value of the Authorization header        
            String token = Main.cfg.getDiscord().getToken();
            String authorization = "Bot " + token;
            
            // TODO reactify
            HttpURLConnection con = (HttpURLConnection) URI.create("https://discordapp.com/api/gateway/bot").toURL().openConnection();
            
            // add the header to your request
            // For HttpUrlConnection this looks like:
            con.setRequestProperty("Authorization", authorization);
            con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            con.setRequestProperty("User-Agent", "DiscordBot (https://tterrag.com, 1.0)");

            ObjectMapper mapper = new ObjectMapper()
                    .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
                    .addHandler(new UnknownPropertyHandler(true))
                    .registerModules(new PossibleModule(), new Jdk8Module());

            JsonNode gateway = mapper.readTree(new InputStreamReader(con.getInputStream()));

            if (gateway.isObject()) {
                
                PayloadReader reader = new JacksonPayloadReader(mapper);
                PayloadWriter writer = new JacksonPayloadWriter(mapper);
                RetryOptions retryOptions = new RetryOptions(Duration.ofSeconds(5), Duration.ofSeconds(120), Integer.MAX_VALUE);

                wrapped = new GatewayClient(reader, writer, retryOptions, token, new IdentifyOptions(0, 1, null), null, new TokenBucket(10, Duration.ofSeconds(5)));
                endpoint = gateway.get("url").textValue();
                if (endpoint == null) {
                    throw new IllegalStateException("Invalid endpoint url: " + gateway.get("url"));
                }
            } else {
                throw new IllegalStateException("Invalid response from gateway endpoint: " + gateway);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    @Override
    @Deprecated
    public Mono<Void> connect(String string, FrameParser<Dispatch, GatewayPayload<?>> frameParser) {
        return connect();
    }

    public Mono<Void> connect() {
        return wrapped.execute(endpoint + "/?v=6&encoding=json&compress=zlib-stream");
    }

    @Override
    public Flux<Dispatch> inbound() {
        return wrapped.dispatch();
    }

    @Override
    public FluxSink<GatewayPayload<?>> outbound() {
        return wrapped.sender();
    }
}
