package com.tterrag.chatmux.bridge.twitch.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.ToString;

@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
public class UserResponse {

    public String id;
    public String login;
    @JsonProperty("display_name")
    public String displayName;
    @JsonProperty("profile_image_url")
    public String avatarUrl;
}
