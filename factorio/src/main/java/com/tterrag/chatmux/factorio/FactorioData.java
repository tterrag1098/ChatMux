package com.tterrag.chatmux.factorio;

import com.tterrag.chatmux.config.ServiceData;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@EqualsAndHashCode
@ToString
@Getter
public class FactorioData implements ServiceData {

    private String input = "server/server.out";
    
    private String output = "server/server.fifo";
}
