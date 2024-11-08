package me.wang.premiumterminal.utils;


import com.google.common.collect.EvictingQueue;
import com.google.gson.Gson;
import me.wang.premiumterminal.PremiumTerminal;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.*;
import org.apache.logging.log4j.core.appender.DefaultErrorHandler;
import org.apache.logging.log4j.core.filter.LevelRangeFilter;
import org.apache.logging.log4j.core.layout.AbstractStringLayout;
import org.apache.logging.log4j.core.layout.PatternLayout;


import java.io.Serializable;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;



@SuppressWarnings("UnstableApiUsage")
public class Terminal implements Appender {
    private final LevelRangeFilter filter;
    private ErrorHandler handler = new DefaultErrorHandler(this);
    private final PremiumTerminal main;
    public static EvictingQueue<Log> queue = EvictingQueue.create(100);
    private final AbstractStringLayout.Serializer serializer = PatternLayout.newSerializerBuilder()
            .setPattern("%msg%xEx{full}").setDisableAnsi(true).build();


    public Terminal(PremiumTerminal main) {
        this.main = main;

        filter = LevelRangeFilter.createFilter(
                Level.getLevel(main.getConfig().getString("logger.minLevel", "OFF")),
                Level.getLevel(main.getConfig().getString("logger.maxLevel", "INFO")),
                null, null
        );

        ((Logger) LogManager.getRootLogger()).addAppender(this);
    }

    @Override
    public void append(LogEvent e) {
        if (filter.filter(e) == filter.getOnMismatch()) return;
        Log log = new Log();
        String format = translateLogLevelToMinecraft(e.getLevel().name(),serializer.toSerializable(e));
        log.setMsg(translateAnsiToMinecraft(format));
        log.setTime(getLocalDate());
        log.setLevel(e.getLevel().name());
        log.setSender(e.getLoggerName());
        queue.add(log);
    }

    @Override
    public String getName() {
        return "PremiumTerminal";
    }

    @Override
    public Layout<Serializable> getLayout() {
        return null;
    }

    @Override
    public boolean ignoreExceptions() {
        return true;
    }

    @Override
    public ErrorHandler getHandler() {
        return handler;
    }

    @Override
    public void setHandler(ErrorHandler handler) {
        this.handler = handler;
    }

    @Override
    public State getState() {
        return State.STARTED;
    }

    @Override
    public void initialize() {}

    @Override
    public void start() { }

    @Override
    public void stop() {
        ((Logger) LogManager.getRootLogger()).removeAppender(this);
    }

    @Override
    public boolean isStarted() {
        return true;
    }

    @Override
    public boolean isStopped() {
        return false;
    }


    private String getLocalDate(){
        LocalTime currentTime = LocalTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("[HH:mm:ss]");
        return currentTime.format(formatter);
    }

    private static String translateAnsiToMinecraft(String message) {
        message = message.replaceAll("\u001B\\[32m", "§a"); // 绿色
        message = message.replaceAll("\u001B\\[31m", "§c"); // 红色
        message = message.replaceAll("\u001B\\[33m", "§e"); // 黄色
        //message = message.replaceAll("\u001B\\[0m", "§r"); // 重置

        return message;
    }

    private static String translateLogLevelToMinecraft(String logLevel, String message) {
        switch (logLevel) {
            case "WARN":
                return "§e" + message;
            case "ERROR":
                return "§c" + message;
            case "DEBUG":
                return "§7" + message;
            default:
                return message;
        }
    }
}
