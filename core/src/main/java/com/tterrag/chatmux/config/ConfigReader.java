package com.tterrag.chatmux.config;

import java.util.function.Supplier;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.conversion.ObjectConverter;
import com.electronwill.nightconfig.core.file.FileConfig;

import lombok.Getter;

public class ConfigReader {
    
    private final FileConfig config;
    
    private final ObjectConverter converter = new ObjectConverter();
    
    @Getter
    private ConfigData data;
    
    public ConfigReader() {
        config = FileConfig.builder("chatmux.toml").concurrent().defaultResource("/default_config.toml").build();
    }
    
    public void save() {
        if (data != null) {
            converter.toConfig(data, config);
            config.save();
        }
    }

    public void load() {
        config.load();
        data = converter.toObject(config, ConfigData::new);
    }
    
    public <T> T get(String path, Supplier<T> obj) {
        Config cfg = config.get(path);
        if (cfg == null) {
            cfg = Config.inMemory();
            converter.toConfig(obj.get(), cfg);
            config.set(path, cfg);
            save();
        }
        return converter.toObject(cfg, obj);
    }
}
