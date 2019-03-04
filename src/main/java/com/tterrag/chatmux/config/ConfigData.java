package com.tterrag.chatmux.config;

import java.util.ArrayList;
import java.util.List;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@EqualsAndHashCode
@ToString
@Getter
public class ConfigData {
    
//    private AdminData admin;
    
    private ServiceData.Discord discord;
    
    private ServiceData.Factorio factorio;
    
    private ServiceData.Twitch twitch;
    
    private ServiceData.Mixer mixer;
    
    private List<PermissionEntry> admins = new ArrayList<>();
    
    private List<PermissionEntry> moderators = new ArrayList<>();

}
