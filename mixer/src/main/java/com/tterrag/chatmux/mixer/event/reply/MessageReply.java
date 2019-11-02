package com.tterrag.chatmux.mixer.event.reply;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tterrag.chatmux.mixer.event.object.Message;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageReply implements ReplyData {
    
    public int channel;
    
    public UUID id;
    
    @JsonProperty("user_name")
    public String username;
    @JsonProperty("user_id")
    public int userId;
    @JsonProperty("user_avatar")
    public String userAvatar;
    
    public Message message;
}
