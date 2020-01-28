package com.tterrag.chatmux.factorio;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListenerAdapter;

import emoji4j.EmojiUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.annotation.NonNull;
import reactor.util.annotation.Nullable;

@RequiredArgsConstructor
@Accessors(fluent = true)
@Slf4j
public class FactorioClient {
    
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
    
    @NonNull
    public static final String GLOBAL_TEAM = "global";
    
    @NonNull
    private final File input, output;

    @Getter
    @NonNull
    private final EmitterProcessor<FactorioMessage> inbound = EmitterProcessor.create(false);
    @NonNull
    private final EmitterProcessor<String> outbound = EmitterProcessor.create(false);
    
    @NonNull
    private final FluxSink<FactorioMessage> inboundSink = inbound.sink(FluxSink.OverflowStrategy.LATEST);
    @NonNull
    private final FluxSink<String> outboundSink = outbound.sink(FluxSink.OverflowStrategy.LATEST); 
    
    public Mono<Void> connect() {
        Tailer tailer = new Tailer(input, new TailerListenerAdapter() {
            @Override
            public void handle(@Nullable String line) {
                log.debug("Processing input: " + line);
                line = line == null ? "" : line.trim();
                Matcher m = CHAT_MSG.matcher(line);
                if (m.matches()) {
                    String type = m.group("type");
                    String team = "SHOUT".equals(type) ? GLOBAL_TEAM : m.group("team");
                    if (team == null) {
                        team = GLOBAL_TEAM;
                    }
                    inboundSink.next(new FactorioMessage(m.group("user"), team, EmojiUtils.emojify(m.group("message")), false));
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
            public void handle(@Nullable Exception ex) {
                log.error("Exception from factorio output tailer", ex);
                inboundSink.next(new FactorioMessage("ERROR", GLOBAL_TEAM, ex == null ? "Unknown" : ex.toString(), false));
            }
            
            @Override
            public void fileNotFound() {
                inboundSink.error(new FileNotFoundException(input.getAbsolutePath()));
            }
        }, 1000, true);
        
        return Mono.fromRunnable(tailer::run)
          .subscribeOn(Schedulers.newSingle("Factorio chat reader", true))
          .doOnCancel(() -> {
              log.error("Chat reader canceled");
              tailer.stop();
          })
          .zipWith(outbound.flatMap(s -> 
              Mono.fromCallable(() -> {
                  try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(output))) {
                      out.write((EmojiUtils.shortCodify(s) + "\n").getBytes());
                  }
                  return s;
              })
              .doOnError(t -> inboundSink.next(new FactorioMessage("ERROR", GLOBAL_TEAM, t.toString(), false)))
              .doOnError(t -> log.error("Exception from factorio output", t))
          ).then())
          .then();
    }

    public FluxSink<String> outbound() {
        return outboundSink;
    }
}
