package com.tterrag.chatmux.config;

import com.electronwill.nightconfig.core.conversion.ObjectConverter;
import com.electronwill.nightconfig.core.file.FileConfig;

import lombok.Getter;

public class ConfigReader {
    
    private final FileConfig config;
    
    @Getter
    private ConfigData data;
    
    public ConfigReader() {
        config = FileConfig.builder("chatmux.toml").concurrent().defaultResource("/default_config.toml").build();
    }
    
    public void save() {
        if (data != null) {
            new ObjectConverter().toConfig(data, config);
            config.save();
        }
    }

    public void load() {
        config.load();
        data = new ObjectConverter().toObject(config, ConfigData::new);
    }
}
