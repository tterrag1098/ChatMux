package com.tterrag.chatmux.mixer;

import com.electronwill.nightconfig.core.conversion.Path;
import com.tterrag.chatmux.api.config.ServiceData;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;


@EqualsAndHashCode
@ToString
@Getter
public class MixerData implements ServiceData {
    
    private String token = "YOUR_TOKEN_HERE";
    
    @Path("user_id")
    private int userId;
    
    @Path("client_id")
    private String clientId = "YOUR_CLIENT_ID_HERE";
}
