package com.tterrag.chatmux.config;

import com.electronwill.nightconfig.core.conversion.Conversion;
import com.tterrag.chatmux.util.Service;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@EqualsAndHashCode
@ToString
@Getter
public class AdminData {
    
    private boolean enabled;
    
    @Conversion(Service.Conv.class)
    private Service main;

}
