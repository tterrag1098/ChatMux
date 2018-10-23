package com.tterrag.chatmux.config;

import com.electronwill.nightconfig.core.conversion.Path;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@EqualsAndHashCode
@ToString
@Getter
public class ServiceData {
    
    @EqualsAndHashCode
    @ToString
    @Getter
    public static class Discord {
        
        private String token;
        
    }
    
    @EqualsAndHashCode
    @ToString
    @Getter
    public static class Twitch {
        
        private String token;
        
        private String nick;
        
    }

    @EqualsAndHashCode
    @ToString
    @Getter
    public static class Mixer {
        
        private String token;
        
        @Path("user_id")
        private int userId;
        
        @Path("client_id")
        private String clientId;
    }
}
