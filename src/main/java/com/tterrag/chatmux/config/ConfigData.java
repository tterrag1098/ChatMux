package com.tterrag.chatmux.config;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@EqualsAndHashCode
@ToString
@Getter
public class ConfigData {
    
    private AdminData admin;
    
    private ServiceData discord;
    
    private ServiceData.TwitchData twitch;
    
    private ServiceData.MixerData mixer;
    
    private PermissionData permissions;

}
