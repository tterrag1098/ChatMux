package com.tterrag.chatmux.discord;

import com.tterrag.chatmux.config.ServiceData;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;


@EqualsAndHashCode
@ToString
@Getter
public class DiscordData implements ServiceData {
 
    private String token = "YOUR_TOKEN_HERE";
    
}
