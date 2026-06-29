package com.sellerplugin.listeners;

import com.sellerplugin.SellerPlugin;
import com.sellerplugin.config.ShopConfig;
import com.sellerplugin.economy.EconomyManager;
import com.sellerplugin.gui.ShopGui;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GuiListener implements Listener {

    private final SellerPlugin   plugin;
    private final ShopGui        gui;
    private static final Pattern PAGE_PATTERN =
            Pattern.compile("\\[(\\d+)/(\\d+)]");

    public GuiListener(SellerPlugin plugin) {
        this.plugin = plugin;
        this.gui    = new ShopGui(plugin);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player player = (Player) e.getWhoClicked();

        String title = e.getView().getTitle();
        ShopConfig cfg = plugin.getShopConfig();

        // Check this is our GUI by matching the title prefix
        String baseTitle = cfg.getGuiTitle();
        if (!title.startsWith(baseTitle)) return;

        e.setCancelled(true);

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        // Determine current page from title
        int currentPage = 1;
        Matcher m = PAGE_PATTERN.matcher(title);
        if (m.find()) currentPage = Integer.parseInt(m.group(1));

        // Check for navigation tags
        String tag = ShopGui.extractMeta(clicked, "TAG");
        if (tag != null) {
            if (tag.equals("PREV")) {
                gui.open(player, currentPage - 1);
            } else if (tag.equals("NEXT")) {
                gui.open(player, currentPage + 1);
            }
            return;
        }

        // Check for shop item metadata
        String matStr  = ShopGui.extractMeta(clicked, "MAT");
        String buyStr  = ShopGui.extractMeta(clicked, "BUY");
        String sellStr = ShopGui.extractMeta(clicked, "SELL");
        String amtStr  = ShopGui.extractMeta(clicked, "AMT");

        if (matStr == null) return; // not a shop item

        Material material = Material.matchMaterial(matStr);
        if (material == null) return;

        double buyPrice  = buyStr  != null ? safeDouble(buyStr)  : 0;
        double sellPrice = sellStr != null ? safeDouble(sellStr) : 0;
        int    amount    = amtStr  != null ? safeInt(amtStr)     : 1;

        ClickType click = e.getClick();
        EconomyManager eco = plugin.getEconomyManager();

        if (click == ClickType.LEFT) {
            // BUY
            if (buyPrice <= 0) {
                player.sendMessage(cfg.getMsgItemNotBuyable());
                return;
            }
            if (!eco.has(player, buyPrice)) {
                player.sendMessage(cfg.getMsgNotEnoughMoney()
                        .replace("{price}", eco.format(buyPrice)));
                return;
            }
            eco.withdraw(player, buyPrice);
            player.getInventory().addItem(new ItemStack(material, amount));
            player.sendMessage(cfg.getMsgBuySuccess()
                    .replace("{amount}", String.valueOf(amount))
                    .replace("{item}", formatMaterial(material))
                    .replace("{price}", eco.format(buyPrice)));

        } else if (click == ClickType.RIGHT) {
            // SELL 1
            sell(player, material, sellPrice, 1, eco, cfg);

        } else if (click == ClickType.SHIFT_RIGHT) {
            // SELL ALL
            int count = countItems(player, material);
            if (count == 0) {
                player.sendMessage(cfg.getMsgNotEnoughItems());
                return;
            }
            sell(player, material, sellPrice, count, eco, cfg);
        }
    }

    // ---------------------------------------------------------------- helpers

    private void sell(Player player, Material material, double priceEach, int qty,
                      EconomyManager eco, ShopConfig cfg) {
        if (priceEach <= 0) {
            player.sendMessage(cfg.getMsgItemNotSellable());
            return;
        }
        if (countItems(player, material) < qty) {
            player.sendMessage(cfg.getMsgNotEnoughItems());
            return;
        }
        removeItems(player, material, qty);
        double total = priceEach * qty;
        eco.deposit(player, total);
        player.sendMessage(cfg.getMsgSellSuccess()
                .replace("{amount}", String.valueOf(qty))
                .replace("{item}", formatMaterial(material))
                .replace("{price}", eco.format(total)));
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
        return ChatColor.stripColor(
            mat.name().replace('_', ' ')
               .toLowerCase()
               .replaceAll("(^|\\s)(.)", m -> m.group(1) + m.group(2).toUpperCase())
        );
    }

    private double safeDouble(String s) {
        try { return Double.parseDouble(s); } catch (Exception e) { return 0; }
    }

    private int safeInt(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return 1; }
    }

    public ShopGui getGui() { return gui; }
}
