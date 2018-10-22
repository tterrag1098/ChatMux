package com.tterrag.chatmux.bridge.discord.response;


import com.fasterxml.jackson.annotation.JsonProperty;

import discord4j.common.jackson.UnsignedJson;
import discord4j.common.json.UserResponse;
import lombok.ToString;

@ToString
public class WebhookObject {
    
    @UnsignedJson
    public long id;
    
    @UnsignedJson
    @JsonProperty("guild_id")
    public long guild;

    @UnsignedJson
    @JsonProperty("channel_id")
    public long channel;
    
    public UserResponse user;
    
    public String name;
    public String avatar;
    public String token;
}
