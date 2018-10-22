package com.tterrag.chatmux.config;

import com.electronwill.nightconfig.core.conversion.Path;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@EqualsAndHashCode
@ToString
@Getter
public class ServiceData {
    
    String token;
    
    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    @Getter
    public static class TwitchData extends ServiceData {
        
        private String nick;
        
    }

    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    @Getter
    public static class MixerData extends ServiceData {
        
        @Path("client_id")
        private String clientId;
    }
}
