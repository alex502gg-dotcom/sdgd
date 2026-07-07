package com.sellerplugin.commands;

import com.sellerplugin.SellerPlugin;
import com.sellerplugin.config.ShopItem;
import com.sellerplugin.economy.EconomyManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

        if (args.length >= 1 && args[0].equalsIgnoreCase("auto")) {
            handleAutoSell(player, args);
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

        plugin.getGuiListener().getGui().open(player, page);
        return true;
    }

    private void handleAutoSell(Player player, String[] args) {
        if (!player.hasPermission("sellerplugin.auto")) {
            player.sendMessage(plugin.getShopConfig().getMsgNoPermission());
            return;
        }

        if (args.length == 1) {
            sellAllConfiguredItems(player);
            return;
        }

        if (args.length == 3) {
            int page = parsePositiveInt(args[1]);
            int itemNumber = parsePositiveInt(args[2]);

            if (page <= 0 || itemNumber <= 0) {
                player.sendMessage(plugin.getShopConfig().getMsgAutoUsage());
                return;
            }

            sellPageItem(player, page, itemNumber);
            return;
        }

        player.sendMessage(plugin.getShopConfig().getMsgAutoUsage());
    }

    private void sellAllConfiguredItems(Player player) {
        Map<Material, ShopItem> bestSellItems = new LinkedHashMap<>();
        for (ShopItem item : plugin.getShopConfig().getItems()) {
            if (!item.isSellable()) continue;

            ShopItem current = bestSellItems.get(item.getMaterial());
            if (current == null || item.getSellPrice() > current.getSellPrice()) {
                bestSellItems.put(item.getMaterial(), item);
            }
        }

        int soldAmount = 0;
        double totalPrice = 0;
        EconomyManager eco = plugin.getEconomyManager();

        for (ShopItem item : bestSellItems.values()) {
            int count = countItems(player, item.getMaterial());
            if (count <= 0) continue;

            removeItems(player, item.getMaterial(), count);
            soldAmount += count;
            totalPrice += item.getSellPrice() * count;
        }

        if (soldAmount <= 0) {
            player.sendMessage(plugin.getShopConfig().getMsgNotEnoughItems());
            return;
        }

        eco.deposit(player, totalPrice);
        player.sendMessage(plugin.getShopConfig().getMsgAutoSellSuccess()
                .replace("{amount}", String.valueOf(soldAmount))
                .replace("{price}", eco.format(totalPrice)));
    }

    private void sellPageItem(Player player, int page, int itemNumber) {
        List<ShopItem> pageItems = plugin.getShopConfig().getItemsForPage(page).stream()
                .sorted(Comparator.comparingInt(ShopItem::getSlot))
                .collect(Collectors.toList());

        if (itemNumber > pageItems.size()) {
            player.sendMessage(plugin.getShopConfig().getMsgAutoItemNotFound()
                    .replace("{item}", String.valueOf(itemNumber))
                    .replace("{page}", String.valueOf(page)));
            return;
        }

        ShopItem item = pageItems.get(itemNumber - 1);
        if (!item.isSellable()) {
            player.sendMessage(plugin.getShopConfig().getMsgItemNotSellable());
            return;
        }

        int count = countItems(player, item.getMaterial());
        if (count <= 0) {
            player.sendMessage(plugin.getShopConfig().getMsgNotEnoughItems());
            return;
        }

        removeItems(player, item.getMaterial(), count);
        double totalPrice = item.getSellPrice() * count;
        EconomyManager eco = plugin.getEconomyManager();
        eco.deposit(player, totalPrice);
        player.sendMessage(plugin.getShopConfig().getMsgSellSuccess()
                .replace("{amount}", String.valueOf(count))
                .replace("{item}", formatMaterial(item.getMaterial()))
                .replace("{price}", eco.format(totalPrice)));
    }

    private int countItems(Player player, Material mat) {
        int count = 0;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack != null && stack.getType() == mat) {
                count += stack.getAmount();
            }
        }
        return count;
    }

    private void removeItems(Player player, Material mat, int amount) {
        ItemStack[] contents = player.getInventory().getContents();
        int remaining = amount;
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack stack = contents[i];
            if (stack != null && stack.getType() == mat) {
                if (stack.getAmount() <= remaining) {
                    remaining -= stack.getAmount();
                    contents[i] = null;
                } else {
                    stack.setAmount(stack.getAmount() - remaining);
                    remaining = 0;
                }
            }
        }
        player.getInventory().setContents(contents);
    }

    private String formatMaterial(Material mat) {
        String[] words = mat.name().toLowerCase().replace('_', ' ').split(" ");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (sb.length() > 0) sb.append(' ');
            if (!word.isEmpty()) sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return ChatColor.stripColor(sb.toString());
    }

    private int parsePositiveInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }
}
