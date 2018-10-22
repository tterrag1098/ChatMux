package com.tterrag.chatmux.config;

import java.util.Locale;

import com.electronwill.nightconfig.core.conversion.Converter;

public enum ServiceType {
    
    DISCORD,
    TWITCH,
    MIXER,
    ;

    public static class Conv implements Converter<ServiceType, String> {

        @Override
        public ServiceType convertToField(String value) {
            return ServiceType.valueOf(value.toUpperCase(Locale.ROOT));
        }

        @Override
        public String convertFromField(ServiceType value) {
            return value.name().toLowerCase(Locale.ROOT);
        }
    }
}
