package com.tterrag.chatmux;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URI;
import java.time.Duration;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.tterrag.chatmux.bridge.discord.DiscordCommandHandler;
import com.tterrag.chatmux.bridge.discord.DiscordRequestHelper;
import com.tterrag.chatmux.bridge.discord.response.WebhookObject;
import com.tterrag.chatmux.bridge.mixer.MixerRequestHelper;
import com.tterrag.chatmux.bridge.mixer.event.MixerEvent;
import com.tterrag.chatmux.bridge.mixer.event.ReplyEvent;
import com.tterrag.chatmux.bridge.mixer.method.MixerMethod;
import com.tterrag.chatmux.bridge.mixer.method.MixerMethod.MethodType;
import com.tterrag.chatmux.bridge.mixer.response.ChatResponse;
import com.tterrag.chatmux.bridge.twitch.irc.IRCEvent;
import com.tterrag.chatmux.config.ConfigData;
import com.tterrag.chatmux.config.ConfigReader;
import com.tterrag.chatmux.websocket.FrameParser;
import com.tterrag.chatmux.websocket.WebSocketClient;

import discord4j.common.jackson.PossibleModule;
import discord4j.common.jackson.UnknownPropertyHandler;
import discord4j.gateway.GatewayClient;
import discord4j.gateway.IdentifyOptions;
import discord4j.gateway.json.dispatch.MessageCreate;
import discord4j.gateway.json.dispatch.Ready;
import discord4j.gateway.payload.JacksonPayloadReader;
import discord4j.gateway.payload.JacksonPayloadWriter;
import discord4j.gateway.payload.PayloadReader;
import discord4j.gateway.payload.PayloadWriter;
import discord4j.gateway.retry.RetryOptions;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Hooks;

@Slf4j
public class Main {
    
    public static volatile WebhookObject theWebhook; // FIXME TEMPORARY

    public static void main(String[] args) throws MalformedURLException, IOException, ProtocolException, InterruptedException {
        ConfigReader cfgReader = new ConfigReader();
        cfgReader.load();
        ConfigData config = cfgReader.getData();
        
        Hooks.onOperatorDebug();

        WebSocketClient<MixerEvent, MixerMethod> mixer = new WebSocketClient<>();
        
        MixerRequestHelper mrh = new MixerRequestHelper(new ObjectMapper(), config.getMixer().getClientId(), config.getMixer().getToken());
        ChatResponse chat = mrh.get("/chats/148199", ChatResponse.class).block();
        
//        System.out.println(mrh.post("/oauth/token/introspect", "{\"token\":\"" + config.getMixer().getToken() + "\"}", TokenIntrospectResponse.class).block());
        
        mixer.connect(chat.endpoints[0], new FrameParser<>(MixerEvent::parse, new ObjectMapper())).subscribe();
        
        System.out.println(new ObjectMapper().writeValueAsString(new MixerMethod(MethodType.AUTH, 148199, 206522, chat.authKey)));
                
        mixer.outbound().next(new MixerMethod(MethodType.AUTH, 148199, 206522, chat.authKey));
        
        mixer.inbound().ofType(ReplyEvent.class).blockFirst();
        
        mixer.outbound().next(new MixerMethod(MethodType.MESSAGE, "Hello World!"));
        
        // build the value of the Authorization header        
        String token = config.getDiscord().getToken();
        String authorization = "Bot " + token;

        HttpURLConnection con = (HttpURLConnection) URI.create("https://discordapp.com/api/gateway/bot").toURL().openConnection();
        
        // add the header to your request
        // For HttpUrlConnection this looks like:
        con.setRequestProperty("Authorization", authorization);
        con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        con.setRequestProperty("User-Agent", "DiscordBot (https://tropicraft.net, 1.0)");

        try {
            ObjectMapper mapper = new ObjectMapper()
                    .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
                    .addHandler(new UnknownPropertyHandler(true))
                    .registerModules(new PossibleModule(), new Jdk8Module());

            JsonNode gateway = mapper.readTree(new InputStreamReader(con.getInputStream()));

            if (gateway.isObject()) {
                
                PayloadReader reader = new JacksonPayloadReader(mapper);
                PayloadWriter writer = new JacksonPayloadWriter(mapper);
                RetryOptions retryOptions = new RetryOptions(Duration.ofSeconds(5), Duration.ofSeconds(120), Integer.MAX_VALUE);

                GatewayClient gatewayClient = new GatewayClient(reader, writer, retryOptions, token, new IdentifyOptions(0, 1, null));
                gatewayClient.execute(gateway.get("url").asText() + "/?v=6&encoding=json&compress=zlib-stream").subscribe();
                
                gatewayClient.dispatch().ofType(Ready.class).log().subscribe();
                
                final DiscordCommandHandler commands = new DiscordCommandHandler(token, mapper);

                gatewayClient.dispatch().ofType(MessageCreate.class)
                    .doOnError(e -> e.printStackTrace())
                    .subscribe(mc -> commands.handle(mc.getChannelId(), mc.getAuthor().getId(), mc.getContent().split("\\s+")), Throwable::printStackTrace);
                
                WebSocketClient<IRCEvent, String> tc = new WebSocketClient<>();
                tc.connect("wss://irc-ws.chat.twitch.tv:443", new FrameParser<>(IRCEvent::parse, Function.identity()))
                    .subscribe();
                
                tc.outbound()
                    .next("PASS oauth:" + config.getTwitch().getToken())
                    .next("NICK " + config.getTwitch().getNick())
                    .next("JOIN #tterrag1098");
                
                DiscordRequestHelper helper = new DiscordRequestHelper(mapper, token);
                
                tc.inbound()
                    .log()
                    .doOnError(e -> e.printStackTrace())
                    .ofType(IRCEvent.Ping.class)
                    .subscribe(e -> tc.outbound().next("PONG :tmi.twitch.tv"));
                                
                tc.inbound()
                    .ofType(IRCEvent.Message.class)
                    .filter($ -> theWebhook != null)
                    .subscribe(e -> helper.executeWebhook(theWebhook, "{\"content\":\"[Twitch/#" + e.getChannel() + "] <" + e.getUser() + "> " + e.getContent() + "\"}"));
//                    .zipWith(webhookProvider)
//                    .subscribe(t -> helper.executeWebhook(t.getT2(), "{\"content\":\"[Twitch/#" + t.getT1().getChannel() + "] <" + t.getT1().getUser() + "> " + t.getT1().getContent() + "\"}"));
                
                mixer.inbound()
                    .ofType(MixerEvent.Message.class)
                    .subscribe(e -> helper.executeWebhook(theWebhook, "{\"content\":\"[Twitch/" + e.channel + "] <" + e.username + "> " + e.message.rawText() + "\"}"));
//                    .zipWith(webhookProvider)
//                    .subscribe(t -> helper.executeWebhook(t.getT2(), "{\"content\":\"[Mixer/" + t.getT1().channel + "] <" + t.getT1().username + "> " + t.getT1().message.rawText() + "\"}"));
//                
//                gatewayClient.dispatch()
//                    .ofType(MessageCreate.class)
//                    .doOnError(e -> e.printStackTrace())
//                    .subscribe(e -> tc.outbound().next("PRIVMSG #tterrag1098 :[" + e.getAuthor().getUsername() + "] " + e.getContent()));
//                
                Thread.sleep(999999);
            }
        } catch (IOException e) {
            System.out.println(new BufferedReader(new InputStreamReader(con.getErrorStream())).lines().collect(Collectors.joining("\n")));
            e.printStackTrace();
        }

    }

}
