package com.tterrag.chatmux.config;

import com.electronwill.nightconfig.core.conversion.Conversion;
import com.tterrag.chatmux.bridge.AbstractChatService;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@EqualsAndHashCode
@ToString
@Getter
public class AdminData {
    
    private boolean enabled;
    
    @Conversion(AbstractChatService.Conv.class)
    private AbstractChatService<?> main;

}
