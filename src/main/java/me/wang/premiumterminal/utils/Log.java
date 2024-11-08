package me.wang.premiumterminal.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Log {
    private String time;
    private String level;
    private String msg;
    private String sender;
    private List<String> filter = new ArrayList<>(Arrays.asList(
            "Minecraft",
            "net.minecraft.server.MinecraftServer",
            ""
    ));



    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public void setSender(String sender){
        this.sender = sender;
    }

    public String getLevel() {
        return level;
    }

    public String getSender() {
        return sender;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getMsg() {
        return msg;
    }

    public List<String> getFilter(){
        return filter;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }


    @Override
    public String toString() {
        String format = String.format("[%s]",sender);
        if (filter.contains(sender)){
            format = "";
        }
        return String.format("[%s %s]:%s %s", time, level, format, msg);
    }
}

