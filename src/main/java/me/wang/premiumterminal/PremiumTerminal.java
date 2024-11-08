package me.wang.premiumterminal;

import me.wang.premiumterminal.utils.Network;
import me.wang.premiumterminal.utils.Terminal;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public final class PremiumTerminal extends JavaPlugin {

    private static Terminal terminal;
    private static Network network;
    public Plugin plugin;
    public static File logFile;
    public static YamlConfiguration logconfig;

    @Override
    public void onEnable() {
        plugin  = me.wang.premiumterminal.PremiumTerminal.getPlugin(me.wang.premiumterminal.PremiumTerminal.class);
        getCommand("premiumterminal").setExecutor( new Command());
        saveResource("log.yml",true);
        saveResource("config.yml",false);
        saveDefaultConfig();
        reloadConfig();

        logFile = new File(getDataFolder(),"log.yml");
        logconfig = YamlConfiguration.loadConfiguration(logFile);
        try {
            logconfig.save(logFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        terminal = new Terminal(this);
        terminal.start();
        network = new Network(this,"97dc98e66231.ofalias.net",54555);
        network.start();
        // Plugin startup logic

    }

    @Override
    public void onDisable() {
        terminal.stop();
        network.stop(false);
        // Plugin shutdown logic
    }

    public static Network getNetwork(){
        return network;
    }
}
