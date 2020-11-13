package com.tterrag.chatmux.twitch.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.ToString;

@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
public class UserResponse {

    @JsonProperty("_id")
    public String id;
    @JsonProperty("name")
    public String login;
    @JsonProperty("display_name")
    public String displayName;
    @JsonProperty("logo")
    public String avatarUrl;
}
