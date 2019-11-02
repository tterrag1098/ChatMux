package com.tterrag.chatmux.twitch;

import com.tterrag.chatmux.api.config.ServiceData;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@EqualsAndHashCode
@ToString
@Getter
public class TwitchData implements ServiceData {
    
    private String tokenSend = "YOUR_TOKEN_HERE";
    
    private String nickSend = "ChatMux";
    
    private String tokenReceive = "YOUR_TOKEN_HERE";
    
    private String nickReceive = "Broadcaster";
    
}