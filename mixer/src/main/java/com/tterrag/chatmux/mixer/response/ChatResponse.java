package com.tterrag.chatmux.mixer.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.ToString;

@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
public class ChatResponse {

    @JsonProperty("authkey")
    public String authKey;

    public String[] endpoints;

    public String[] permissions;

}
