package com.tterrag.chatmux.bridge.discord;

import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteStreams;
import com.tterrag.chatmux.util.RequestHelper;

import discord4j.common.jackson.PossibleModule;
import discord4j.common.json.MessageResponse;
import discord4j.common.json.UserResponse;
import discord4j.rest.RestClient;
import discord4j.rest.http.ExchangeStrategies;
import discord4j.rest.http.client.DiscordWebClient;
import discord4j.rest.json.request.MessageCreateRequest;
import discord4j.rest.json.request.WebhookCreateRequest;
import discord4j.rest.json.response.ChannelResponse;
import discord4j.rest.json.response.WebhookResponse;
import discord4j.rest.request.DefaultRouter;
import discord4j.rest.route.Routes;
import discord4j.rest.util.MultipartRequest;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

public class DiscordRequestHelper extends RequestHelper {
    
    private final String token;
    private final RestClient client;
    
    public DiscordRequestHelper(String token) {
        super(new ObjectMapper()
                .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .registerModules(new PossibleModule(), new Jdk8Module()),
                Routes.BASE_URL);
        
        this.token = token;

        HttpHeaders defaultHeaders = new DefaultHttpHeaders();
        addHeaders(defaultHeaders);
                
        final DiscordWebClient httpClient = new DiscordWebClient(super.client, ExchangeStrategies.jackson(mapper), token);
        this.client = new RestClient(new DefaultRouter(httpClient));
    }
    
    @Override
    protected void addHeaders(HttpHeaders headers) {
        headers.add(HttpHeaderNames.CONTENT_TYPE, "application/json");
        headers.add(HttpHeaderNames.AUTHORIZATION, "Bot " + token);
        headers.add(HttpHeaderNames.USER_AGENT, "DiscordBot(https://tterrag.com 1.0)");
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
    public Mono<WebhookResponse> getWebhook(long channel, String name, InputStream avatar) {
        return client.getWebhookService().getChannelWebhooks(channel)
                .filter(existing -> existing.getName().equals(name))
                .singleOrEmpty() // If there's more than one webhook with the same name, we have big problems...
                .switchIfEmpty(Mono.defer(() -> {
                    try (InputStream in = avatar) {
                        byte[] image = ByteStreams.toByteArray(in);
                        String encoded = Base64.getEncoder().encodeToString(image);
                        return client.getWebhookService().createWebhook(channel, new WebhookCreateRequest(name, "data:image/png;base64," + encoded), null);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }));
    }
    
    public Mono<ChannelResponse> getChannel(long channel) {
        return client.getChannelService().getChannel(channel);
    }
    
    public Mono<UserResponse> getUser(long user) {
        return client.getUserService().getUser(user);
    }

    public Disposable deleteMessage(long channelId, long id) {
        return client.getChannelService().deleteMessage(channelId, id, null).subscribe();
    }

    public Disposable kick(long guildId, long id) {
        return client.getGuildService().removeGuildMember(guildId, id, null).subscribe();
    }
    
    public Disposable ban(long guildId, long id, int daysToDelete, String reason) {
        return client.getGuildService().createGuildBan(guildId, id, ImmutableMap.of("delete-message-days", daysToDelete, "reason", reason), null).subscribe();
    }
    
    public Disposable addReaction(long channelId, long messageId, String id, String name) {
        return client.getChannelService().createReaction(channelId, messageId, (id == null ? name : id + ":" + name)).subscribe();
    }

    public Disposable removeReaction(long channelId, long userId, long messageId, String id, String name) {
        return client.getChannelService().deleteReaction(channelId, messageId, (id == null ? name : id + ":" + name), userId).subscribe();
    }
    
    public Mono<MessageResponse> executeWebhook(WebhookResponse webhook, String payload) {
        return executeWebhook(webhook.getId(), webhook.getToken(), payload);
    }
    
    public Mono<MessageResponse> executeWebhook(long id, String token, String payload) {
        return post("/webhooks/" + id + "/" + token + "?wait=true", payload, MessageResponse.class);
    }

    public Mono<UserResponse> getOurUser() {
        return client.getUserService().getCurrentUser();
    }

    public Disposable sendMessage(long channel, String string) {
        return client.getChannelService().createMessage(channel, new MultipartRequest(new MessageCreateRequest(string, null, false, null))).subscribe();
    }
}
