package com.tterrag.chatmux.config;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@EqualsAndHashCode
@ToString
@Getter
public class PermissionData {

    private Long discord;
    
    private Integer mixer;
    
    private String twitch;

}
