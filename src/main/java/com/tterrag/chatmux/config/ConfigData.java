package com.tterrag.chatmux.config;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@EqualsAndHashCode
@ToString
@Getter
public class ConfigData {
    
    private AdminData admin;
    
    private ServiceData.Discord discord;
    
    private ServiceData.Twitch twitch;
    
    private ServiceData.Mixer mixer;
    
    private PermissionData permissions;

}
