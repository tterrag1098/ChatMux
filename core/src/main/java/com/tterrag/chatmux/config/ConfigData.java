package com.tterrag.chatmux.config;

import java.util.ArrayList;
import java.util.List;

import com.electronwill.nightconfig.core.conversion.Conversion;
import com.electronwill.nightconfig.core.conversion.PreserveNotNull;
import com.tterrag.chatmux.bridge.AbstractChatService;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@EqualsAndHashCode
@ToString
@Getter
public class ConfigData {
    
    @Conversion(AbstractChatService.Conv.class)
    private AbstractChatService<?> main;
    
    @PreserveNotNull
    private List<PermissionEntry> admins = new ArrayList<>();
    
    @PreserveNotNull
    private List<PermissionEntry> moderators = new ArrayList<>();

}
