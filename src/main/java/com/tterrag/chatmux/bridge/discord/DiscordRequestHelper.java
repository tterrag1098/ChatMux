package com.tterrag.chatmux.bridge.discord;

import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

import javax.annotation.ParametersAreNonnullByDefault;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.ByteStreams;
import com.tterrag.chatmux.bridge.discord.response.WebhookObject;
import com.tterrag.chatmux.util.RequestHelper;

import io.netty.handler.codec.http.HttpHeaders;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@ParametersAreNonnullByDefault
public class DiscordRequestHelper extends RequestHelper {
    
    private final String token;
    
    public DiscordRequestHelper(ObjectMapper mapper, String token) {
        super(mapper);
        this.token = token;
    }
    
    @Override
    protected String getBaseUrl() {
        return "https://discordapp.com/api";
    }
    
    @Override
    protected void addHeaders(HttpHeaders headers) {
        headers.add("Authorization", "Bot " + token);
        headers.add("Content-Type", "application/json");
        headers.add("User-Agent", "DiscordBot (https://tropicraft.net, 1.0)");
    }

    /**
     * Creates a new webhook, or returns an existing one by the same name.
     * 
     * @param channel
     *            The channel ID to find the webhook in
     * @param name
     *            The name of the webhook
     * @param avatar
     *            An {@link InputStream} pointing to a .png resource
     * @return A {@link WebhookObject} representing the created/found webhook.
     */
    public Mono<WebhookObject> getWebhook(long channel, String name, InputStream avatar) {
        return get("/channels/" + channel + "/webhooks", WebhookObject[].class)
                .flatMapMany(Flux::fromArray)
                .filter(existing -> existing.name.equals(name))
                .singleOrEmpty() // If there's more than one webhook with the same name, we have big problems...
                .switchIfEmpty(Mono.defer(() -> {
                    try (InputStream in = avatar) {
                        byte[] image = ByteStreams.toByteArray(in);
                        String template = "{\"name\":\"%s\", \"avatar\":\"data:image/png;base64,%s\"}";
                        return post("/channels/" + channel + "/webhooks", String.format(template, name, new String(Base64.getEncoder().encodeToString(image))), WebhookObject.class);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }));
    }
    
    public void executeWebhook(WebhookObject webhook, String payload) {
        executeWebhook(webhook.id, webhook.token, payload);
    }
    
    public void executeWebhook(long id, String token, String payload) {
        postVoid("/webhooks/" + id + "/" + token, payload);
    }
}
