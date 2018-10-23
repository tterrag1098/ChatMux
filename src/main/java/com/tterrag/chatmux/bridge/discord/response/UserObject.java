package com.tterrag.chatmux.bridge.discord.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import discord4j.common.jackson.UnsignedJson;
import lombok.ToString;

@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserObject {
    
    @UnsignedJson
    public long id;
    
    public String username;
    
    public String discriminator;

    public boolean bot;
}
