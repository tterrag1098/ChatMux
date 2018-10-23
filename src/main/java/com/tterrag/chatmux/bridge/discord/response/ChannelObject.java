package com.tterrag.chatmux.bridge.discord.response;

import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import discord4j.common.jackson.UnsignedJson;
import lombok.ToString;

@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChannelObject {
    
    @UnsignedJson
    public long id;
    
    @Nullable
    public String name;

}
