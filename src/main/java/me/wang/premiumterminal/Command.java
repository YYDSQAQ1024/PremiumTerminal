package me.wang.premiumterminal;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.List;

public class Command implements CommandExecutor, TabExecutor {
    @Override
    public boolean onCommand(CommandSender commandSender, org.bukkit.command.Command command, String s, String[] strings) {
        if (commandSender instanceof Player){
            Player p = (Player) commandSender;
            if (!p.isOp()){
                return true;
            }
            if (strings.length == 0){
                p.sendMessage(ChatColor.AQUA+"PremiumTerminal By lao_wang");
            }
            if (strings[0].equalsIgnoreCase("uuid")){
                p.sendMessage(PremiumTerminal.getNetwork().getClientUUID());
            }
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, org.bukkit.command.Command command, String s, String[] strings) {
        return List.of("uuid");
    }
}
