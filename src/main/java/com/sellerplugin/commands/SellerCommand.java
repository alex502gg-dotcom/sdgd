package com.sellerplugin.commands;

import com.sellerplugin.SellerPlugin;
import com.sellerplugin.gui.ShopGui;
import com.sellerplugin.listeners.GuiListener;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SellerCommand implements CommandExecutor {

    private final SellerPlugin plugin;

    public SellerCommand(SellerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;

        // /seller reload — admin sub-command
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!player.hasPermission("sellerplugin.admin")) {
                player.sendMessage(plugin.getShopConfig().getMsgNoPermission());
                return true;
            }
            plugin.reloadShopConfig();
            player.sendMessage(plugin.getShopConfig().getMsgReloadSuccess());
            return true;
        }

        if (!player.hasPermission("sellerplugin.use")) {
            player.sendMessage(plugin.getShopConfig().getMsgNoPermission());
            return true;
        }

        // Open shop at page 1 (or a specific page: /seller 2)
        int page = 1;
        if (args.length == 1) {
            try { page = Integer.parseInt(args[0]); } catch (NumberFormatException ignored) {}
        }

        ShopGui gui = ((GuiListener) plugin.getServer()
                .getPluginManager()
                .getRegisteredListeners(plugin)
                .stream()
                .filter(r -> r.getListener() instanceof GuiListener)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("GuiListener not registered"))
                .getListener())
                .getGui();

        gui.open(player, page);
        return true;
    }
}
