package com.tterrag.chatmux.bridge.factorio;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListenerAdapter;

import com.tterrag.chatmux.websocket.FrameParser;
import com.tterrag.chatmux.websocket.WebSocketClient;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.annotation.NonNull;

@RequiredArgsConstructor
@Accessors(fluent = true)
@Slf4j
public class FactorioClient implements WebSocketClient<FactorioMessage, String> {
    
    private static final String TIMESTAMP_REGEX = "(?<date>\\d{4}-\\d{2}-\\d{2})\\s(?<time>\\d{2}:\\d{2}:\\d{2})";
    
    private static final Pattern CHAT_MSG = Pattern.compile(
            TIMESTAMP_REGEX + "\\s"
            + "\\[(?<type>CHAT|SHOUT)\\]\\s"
            + "(?!<server>)(?<user>\\S+)\\s*"
            + "(?:\\[(?<team>[^\\]]+)\\])?\\s*"
            + "(?:\\(shout\\))?:\\s*"
            + "(?<message>.+)$"
    );
    
    private static final Pattern JOIN_LEAVE_MSG = Pattern.compile(    
            TIMESTAMP_REGEX + "\\s"
            + "\\[(?<type>JOIN|LEAVE)\\]\\s"
            + "(?<user>\\S+)\\s"
            + "(?<message>.+)$"
    );
    
    private static final Pattern COMMAND_MSG = Pattern.compile(
            TIMESTAMP_REGEX + "\\s"
            + "\\[(?<type>COMMAND)\\]\\s"
            + "(?!<server>)(?<user>\\S+)\\s*"
            + "(?:\\[(?<team>[^\\]]+)\\])?\\s*"
            + "(?:\\(command\\)):\\s*"
            + "(?<message>.+)$"
    );
    
    public static final String GLOBAL_TEAM = "global";
    
    private final File input, output;

    @Getter(onMethod = @__({@Override}))
    private final EmitterProcessor<FactorioMessage> inbound = EmitterProcessor.create(false);
    private final EmitterProcessor<String> outbound = EmitterProcessor.create(false);
    
    private final FluxSink<FactorioMessage> inboundSink = inbound.sink(FluxSink.OverflowStrategy.LATEST);
    private final FluxSink<String> outboundSink = outbound.sink(FluxSink.OverflowStrategy.LATEST); 
    
    public Mono<Void> connect() {
        Tailer tailer = new Tailer(input, new TailerListenerAdapter() {
            @Override
            public void handle(String line) {
                line = line.trim();
                Matcher m = CHAT_MSG.matcher(line);
                if (m.matches()) {
                    String type = m.group("type");
                    String team = "SHOUT".equals(type) ? GLOBAL_TEAM : m.group("team");
                    inboundSink.next(new FactorioMessage(m.group("user"), team, m.group("message"), false));
                    return;
                }
                m = JOIN_LEAVE_MSG.matcher(line);
                if (m.matches()) {
                    inboundSink.next(new FactorioMessage(m.group("user"), GLOBAL_TEAM, m.group("message"), true));
                    return;
                }
                m = COMMAND_MSG.matcher(line);
                if (m.matches()) {
                    inboundSink.next(new FactorioMessage(m.group("user"), GLOBAL_TEAM, "Ran command: `" + m.group("message") + "`", true));
                    return;
                }
            }
          
            @Override
            public void handle(Exception ex) {
                inboundSink.next(new FactorioMessage("ERROR", GLOBAL_TEAM, ex.toString(), false));
            }
            
            @Override
            public void fileNotFound() {
                inboundSink.error(new FileNotFoundException(input.getAbsolutePath()));
            }
        }, 1000, true);
        
        return Mono.fromRunnable(tailer::run)
          .subscribeOn(Schedulers.newSingle("FactorioSource chat reader", true))
          .doOnCancel(() -> {
              log.info("Chat reader canceled");
              tailer.stop();
          })
          .zipWith(outbound.flatMap(s -> 
              Mono.fromCallable(() -> {
                  try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(output))) {
                      out.write((s + "\n").getBytes());
                  }
                  return s;
              }).doOnError(t -> inboundSink.next(new FactorioMessage("ERROR", GLOBAL_TEAM, t.toString(), false)))
          ).then())
          .then();
    }

    @Override
    public FluxSink<String> outbound() {
        return outboundSink;
    }

    @Override
    @Deprecated
    public Mono<Void> connect(@NonNull String string, FrameParser<FactorioMessage, String> frameParser) {
        return Mono.empty();
    }
}
