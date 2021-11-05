package com.tterrag.chatmux.discord;

import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.common.io.ByteStreams;
import com.tterrag.chatmux.util.http.RequestHelper;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.Webhook;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.discordjson.json.MessageData;
import discord4j.discordjson.possible.PossibleModule;
import discord4j.rest.route.Routes;
import discord4j.rest.util.Image;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import reactor.core.publisher.Mono;
import reactor.util.annotation.NonNull;

public class DiscordRequestHelper extends RequestHelper {
    
    private final GatewayDiscordClient client;
    private final String token;
    
    public DiscordRequestHelper(GatewayDiscordClient client, String token) {
        super(new ObjectMapper()
                .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .registerModules(new PossibleModule(), new Jdk8Module()),
                Routes.BASE_URL);
        
        this.client = client;
        this.token = token;
    }
    
    @Override
    protected void addHeaders(@NonNull HttpHeaders headers) {
        headers.add(HttpHeaderNames.CONTENT_TYPE, "application/json");
        headers.add(HttpHeaderNames.AUTHORIZATION, "Bot " + token);
        headers.add(HttpHeaderNames.USER_AGENT, "DiscordBot(https://tterrag.com 1.0)");
    }

    /**
     * Creates a new webhook, or returns an existing one by the same name.
     * 
     * @param channelId
     *            The channel ID to find the webhook in
     * @param name
     *            The name of the webhook
     * @param avatar
     *            An {@link InputStream} pointing to a .png resource
     * @return A {@link WebhookObject} representing the created/found webhook.
     */
    public Mono<Webhook> getWebhook(Snowflake channelId, String name, InputStream avatar) {
        final Mono<TextChannel> channel = client.getChannelById(channelId).ofType(TextChannel.class).cache();
        return channel.flatMapMany(c -> c.getWebhooks())
                .filter(existing -> existing.getName().filter(s -> s.equals(name)).isPresent())
                .singleOrEmpty() // If there's more than one webhook with the same name, we have big problems...
                .switchIfEmpty(Mono.defer(() -> {
                    try (InputStream in = avatar) {
                        byte[] image = ByteStreams.toByteArray(in);
                        return channel.flatMap(c -> c.createWebhook(spec -> spec.setName(name).setAvatar(Image.ofRaw(image, Image.Format.PNG))));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }));
    }
    
    public Mono<Message> executeWebhook(Webhook webhook, String payload) {
        return executeWebhook(webhook.getId(), webhook.getToken(), payload);
    }
    
    public Mono<Message> executeWebhook(Snowflake snowflake, String token, String payload) {
        return post("/webhooks/" + snowflake.asString() + "/" + token + "?wait=true", payload, MessageData.class)
                .map(r -> new Message(client, r));
    }
}