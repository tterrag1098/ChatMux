package com.tterrag.chatmux.discord;

import java.util.ArrayList;
import java.util.List;

import com.electronwill.nightconfig.core.conversion.Path;
import com.tterrag.chatmux.api.config.ServiceData;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;


@EqualsAndHashCode
@ToString
@Getter
public class DiscordData implements ServiceData {
 
    private String token = "YOUR_TOKEN_HERE";
    
    @Path("moderation_channels")
    private List<Long> moderationChannels = new ArrayList<>();
    
    private List<Long> admins = new ArrayList<>();
}
