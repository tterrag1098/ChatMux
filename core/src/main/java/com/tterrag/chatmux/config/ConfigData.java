package com.tterrag.chatmux.config;

import java.util.ArrayList;
import java.util.List;

import com.electronwill.nightconfig.core.conversion.Conversion;
import com.electronwill.nightconfig.core.conversion.PreserveNotNull;
import com.tterrag.chatmux.bridge.ChatService;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@EqualsAndHashCode
@ToString
@Getter
public class ConfigData {
    
    @Conversion(ChatService.Conv.class)
    private ChatService<?, ?> main;
    
    private ServiceData.Discord discord;
    
    private ServiceData.Factorio factorio;
    
    private ServiceData.Twitch twitch;
    
    private ServiceData.Mixer mixer;
    
    @PreserveNotNull
    private List<PermissionEntry> admins = new ArrayList<>();
    
    @PreserveNotNull
    private List<PermissionEntry> moderators = new ArrayList<>();

}
