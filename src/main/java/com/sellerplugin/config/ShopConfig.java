package com.sellerplugin.config;

import com.sellerplugin.SellerPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;
import java.util.stream.Collectors;

public class ShopConfig {

    private final SellerPlugin plugin;

    private String guiTitle;
    private int pages;
    private int rows;
    private List<ShopItem> items = new ArrayList<>();

    // Messages
    private String msgBuySuccess;
    private String msgSellSuccess;
    private String msgNotEnoughMoney;
    private String msgNotEnoughItems;
    private String msgItemNotBuyable;
    private String msgItemNotSellable;
    private String msgNoPermission;
    private String msgReloadSuccess;
    private String msgAutoSellSuccess;
    private String msgAutoItemNotFound;
    private String msgAutoUsage;

    public ShopConfig(SellerPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        FileConfiguration cfg = plugin.getConfig();

        // GUI settings
        guiTitle = color(cfg.getString("gui.title", "&6&lShop"));
        pages    = Math.max(1, Math.min(10, cfg.getInt("gui.pages", 3)));
        rows     = Math.max(1, Math.min(6, cfg.getInt("gui.rows", 5)));

        // Messages
        msgBuySuccess      = color(cfg.getString("messages.buy-success",      "&aBought!"));
        msgSellSuccess     = color(cfg.getString("messages.sell-success",     "&aSold!"));
        msgNotEnoughMoney  = color(cfg.getString("messages.not-enough-money", "&cNot enough money!"));
        msgNotEnoughItems  = color(cfg.getString("messages.not-enough-items", "&cNot enough items!"));
        msgItemNotBuyable  = color(cfg.getString("messages.item-not-buyable", "&cNot buyable!"));
        msgItemNotSellable = color(cfg.getString("messages.item-not-sellable","&cNot sellable!"));
        msgNoPermission    = color(cfg.getString("messages.no-permission",    "&cNo permission!"));
        msgReloadSuccess   = color(cfg.getString("messages.reload-success",   "&aReloaded!"));
        msgAutoSellSuccess = color(cfg.getString("messages.auto-sell-success",
                "&aAuto-sold &e{amount}x &aitem(s) for &6${price}&a!"));
        msgAutoItemNotFound = color(cfg.getString("messages.auto-item-not-found",
                "&cSellable item #{item} was not found on page {page}."));
        msgAutoUsage = color(cfg.getString("messages.auto-usage",
                "&cUsage: /seller auto or /seller auto [page] [item]"));

        // Items
        items.clear();
        List<Map<?, ?>> rawItems = cfg.getMapList("items");
        for (Map<?, ?> raw : rawItems) {
            try {
                int    slot     = toInt(raw.get("slot"), 0);
                int    page     = toInt(raw.get("page"), 1);
                String matName  = raw.get("material").toString().toUpperCase();
                Material mat    = Material.matchMaterial(matName);

                if (mat == null) {
                    plugin.getLogger().warning("Unknown material: " + matName + " — skipping item.");
                    continue;
                }

                Object nameObj   = raw.get("name");
                String name      = color(nameObj != null ? nameObj.toString() : mat.name());
                double buyPrice  = toDouble(raw.get("buy-price"),  0);
                double sellPrice = toDouble(raw.get("sell-price"), 0);
                int    amount    = toInt(raw.get("amount"), 1);

                List<String> lore = new ArrayList<>();
                Object rawLore = raw.get("lore");
                if (rawLore instanceof List<?>) {
                    for (Object line : (List<?>) rawLore) {
                        lore.add(color(line.toString()));
                    }
                }

                // Validate page/slot bounds
                if (page < 1 || page > pages) {
                    plugin.getLogger().warning("Item '" + name + "' has page " + page
                        + " but pages is set to " + pages + ". Skipping.");
                    continue;
                }
                int maxSlots = rows * 9;
                if (slot < 0 || slot >= maxSlots) {
                    plugin.getLogger().warning("Item '" + name + "' has slot " + slot
                        + " which is out of range (max " + (maxSlots - 1) + "). Skipping.");
                    continue;
                }

                items.add(new ShopItem(slot, page, mat, name, lore, buyPrice, sellPrice, amount));

            } catch (Exception e) {
                plugin.getLogger().warning("Failed to parse a shop item: " + e.getMessage());
            }
        }

        plugin.getLogger().info("Loaded " + items.size() + " shop items across " + pages + " page(s).");
    }

    // ---------------------------------------------------------------- helpers

    public List<ShopItem> getItemsForPage(int page) {
        return items.stream()
                .filter(i -> i.getPage() == page)
                .collect(Collectors.toList());
    }

    private static String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    private static int toInt(Object o, int def) {
        if (o == null) return def;
        try { return Integer.parseInt(o.toString()); } catch (Exception e) { return def; }
    }

    private static double toDouble(Object o, double def) {
        if (o == null) return def;
        try { return Double.parseDouble(o.toString()); } catch (Exception e) { return def; }
    }

    // ---------------------------------------------------------------- getters

    public String getGuiTitle()          { return guiTitle; }
    public int    getPages()             { return pages; }
    public int    getRows()              { return rows; }
    public List<ShopItem> getItems()     { return items; }

    public String getMsgBuySuccess()      { return msgBuySuccess; }
    public String getMsgSellSuccess()     { return msgSellSuccess; }
    public String getMsgNotEnoughMoney()  { return msgNotEnoughMoney; }
    public String getMsgNotEnoughItems()  { return msgNotEnoughItems; }
    public String getMsgItemNotBuyable()  { return msgItemNotBuyable; }
    public String getMsgItemNotSellable() { return msgItemNotSellable; }
    public String getMsgNoPermission()    { return msgNoPermission; }
    public String getMsgReloadSuccess()   { return msgReloadSuccess; }
    public String getMsgAutoSellSuccess() { return msgAutoSellSuccess; }
    public String getMsgAutoItemNotFound(){ return msgAutoItemNotFound; }
    public String getMsgAutoUsage()       { return msgAutoUsage; }
}
