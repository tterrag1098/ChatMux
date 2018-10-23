package com.tterrag.chatmux.config;

import com.electronwill.nightconfig.core.conversion.Conversion;
import com.tterrag.chatmux.util.ServiceType;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@EqualsAndHashCode
@ToString
@Getter
public class AdminData {
    
    private boolean enabled;
    
    @Conversion(ServiceType.Conv.class)
    private ServiceType main;

}
