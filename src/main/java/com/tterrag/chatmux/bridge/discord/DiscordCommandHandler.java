package com.tterrag.chatmux.bridge.discord;

import java.io.InputStream;
import java.util.Locale;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tterrag.chatmux.Main;
import com.tterrag.chatmux.bridge.discord.response.WebhookObject;

public class DiscordCommandHandler {
    
    private final DiscordRequestHelper helper;

    public DiscordCommandHandler(String token, ObjectMapper mapper) {
        this.helper = new DiscordRequestHelper(mapper, token);
    }

    public void handle(long channel, long author, String... args) {

        if (args.length == 3 && args[0].equals("link")) {

            InputStream in = Main.class.getResourceAsStream("/lovetropicsdiscord.png");
            if (in == null) {
                throw new RuntimeException("Resource not found: avatar.png");
            }
            helper.getWebhook(channel, "TropiBridge", in).subscribe(wh -> {
                Main.theWebhook = wh;
                helper.executeWebhook(Main.theWebhook, "{\"content\":\"Hello world!\"}");
            });
            
            switch (args[1].toLowerCase(Locale.ROOT)) {
                case "twitch": 
                    // Link to twitch channel
                    break;
                case "mixer":
                    // Link to mixer channel
                    break;
                default:
                    break;
            }
            
        }
    }
}
