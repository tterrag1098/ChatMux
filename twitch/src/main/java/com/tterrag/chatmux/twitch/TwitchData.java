package com.tterrag.chatmux.twitch;

import com.tterrag.chatmux.config.ServiceData;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@EqualsAndHashCode
@ToString
@Getter
public class TwitchData implements ServiceData {
    
    private String token = "YOUR_TOKEN_HERE";
    
    private String nick = "ChatMux";
    
}