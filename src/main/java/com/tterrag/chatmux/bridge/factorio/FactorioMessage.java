package com.tterrag.chatmux.bridge.factorio;

import com.tterrag.chatmux.links.Message;
import com.tterrag.chatmux.util.ServiceType;

public class FactorioMessage extends Message {
    boolean action;
    
    public FactorioMessage(String username, String message, boolean action) {
        super(ServiceType.FACTORIO, "", "", username, message, null);
        this.action = action;
    }
    
    @Override
    public String getContent() {
        String content = super.getContent();
        if (action) {
            content = "*" + content + "*";
        }
        return content;
    }
    
    @Override
    public void delete() {} // Impossible
    
    @Override
    public void kick() {
        // TODO
    }
    
    @Override
    public void ban() {
        // TODO
    }

    @Override
    public String toString() {
        return "[" + getSource() + "] <" + getUser() + "> " + getContent();
    }
}