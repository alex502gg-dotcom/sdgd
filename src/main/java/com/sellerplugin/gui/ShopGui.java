package com.sellerplugin.gui;

import com.sellerplugin.SellerPlugin;
import com.sellerplugin.config.ShopConfig;
import com.sellerplugin.config.ShopItem;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ShopGui {

    private final SellerPlugin plugin;

    // Navigation item slots (always in the last row)
    public static final int NAV_PREV  = 0;   // relative to last row start
    public static final int NAV_INFO  = 4;
    public static final int NAV_NEXT  = 8;

    // Slot offsets encoded in item display names so the listener can identify them
    public static final String META_BUY_PREFIX  = "§0§0BUY:";
    public static final String META_SELL_PREFIX = "§0§0SELL:";
    public static final String META_PREV        = "§0§0PREV";
    public static final String META_NEXT        = "§0§0NEXT";

    public ShopGui(SellerPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Open a shop page for a player.
     *
     * @param player target player
     * @param page   1-based page number
     */
    public void open(Player player, int page) {
        ShopConfig cfg = plugin.getShopConfig();
        int totalPages = cfg.getPages();
        page = Math.max(1, Math.min(totalPages, page));

        // rows + 1 navigation row, capped at 6
        int contentRows = cfg.getRows();
        int totalRows   = Math.min(6, contentRows + 1);
        int invSize     = totalRows * 9;

        String title = cfg.getGuiTitle()
                + ChatColor.DARK_GRAY + " [" + page + "/" + totalPages + "]";

        Inventory inv = Bukkit.createInventory(null, invSize, title);

        // Fill content slots
        for (ShopItem item : cfg.getItemsForPage(page)) {
            if (item.getSlot() >= contentRows * 9) continue; // safety
            inv.setItem(item.getSlot(), buildShopItem(item));
        }

        // Navigation row (last row)
        int navRowStart = (totalRows - 1) * 9;

        // Previous page button
        if (page > 1) {
            inv.setItem(navRowStart + NAV_PREV, navItem(
                    Material.ARROW,
                    ChatColor.YELLOW + "◀ Previous Page",
                    META_PREV,
                    ChatColor.GRAY + "Go to page " + (page - 1)
            ));
        }

        // Info button
        inv.setItem(navRowStart + NAV_INFO, navItem(
                Material.PAPER,
                ChatColor.GOLD + "Page " + page + " / " + totalPages,
                "INFO",
                ChatColor.GRAY + "Left-click item = BUY",
                ChatColor.GRAY + "Right-click item = SELL 1",
                ChatColor.GRAY + "Shift+Right = SELL ALL"
        ));

        // Next page button
        if (page < totalPages) {
            inv.setItem(navRowStart + NAV_NEXT, navItem(
                    Material.ARROW,
                    ChatColor.YELLOW + "Next Page ▶",
                    META_NEXT,
                    ChatColor.GRAY + "Go to page " + (page + 1)
            ));
        }

        // Fill empty nav slots with glass panes
        for (int i = navRowStart; i < invSize; i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, glass());
            }
        }

        player.openInventory(inv);
    }

    // ---------------------------------------------------------------- builders

    private ItemStack buildShopItem(ShopItem shopItem) {
        ItemStack stack = new ItemStack(shopItem.getMaterial(), shopItem.getAmount());
        ItemMeta meta   = stack.getItemMeta();
        if (meta == null) return stack;

        meta.setDisplayName(shopItem.getDisplayName());

        List<String> lore = new ArrayList<>(shopItem.getLore());
        lore.add("");
        if (shopItem.isBuyable()) {
            lore.add(ChatColor.GREEN + "Left-click » BUY x" + shopItem.getAmount()
                    + " for $" + String.format("%.2f", shopItem.getBuyPrice()));
        }
        if (shopItem.isSellable()) {
            lore.add(ChatColor.RED + "Right-click » SELL x1 for $"
                    + String.format("%.2f", shopItem.getSellPrice()));
            lore.add(ChatColor.DARK_RED + "Shift+Right » SELL ALL");
        }

        // Encode buy/sell prices in lore line 0 as invisible metadata
        // (they're read back by the listener)
        lore.add(ChatColor.BLACK + "" + ChatColor.BLACK
                + "BUY:" + shopItem.getBuyPrice()
                + ":SELL:" + shopItem.getSellPrice()
                + ":AMT:" + shopItem.getAmount()
                + ":MAT:" + shopItem.getMaterial().name());

        meta.setLore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack navItem(Material mat, String name, String tag, String... loreLines) {
        ItemStack stack = new ItemStack(mat);
        ItemMeta meta   = stack.getItemMeta();
        if (meta == null) return stack;
        meta.setDisplayName(name);
        List<String> lore = new ArrayList<>(Arrays.asList(loreLines));
        // hidden tag
        lore.add(ChatColor.BLACK + "" + ChatColor.BLACK + "TAG:" + tag);
        meta.setLore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack glass() {
        ItemStack g = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta m  = g.getItemMeta();
        if (m != null) { m.setDisplayName(" "); g.setItemMeta(m); }
        return g;
    }

    // ---------------------------------------------------------------- helpers (static, used by listener)

    /**
     * Extract the hidden metadata line value for a given key from an ItemStack's lore.
     * Metadata line format: §0§0KEY:VALUE:KEY2:VALUE2...
     */
    public static String extractMeta(ItemStack item, String key) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) return null;
        for (String line : meta.getLore()) {
            String stripped = ChatColor.stripColor(line);
            if (stripped != null && stripped.contains(key + ":")) {
                // parse key:value pairs
                String[] parts = stripped.split(":");
                for (int i = 0; i < parts.length - 1; i++) {
                    if (parts[i].equals(key)) return parts[i + 1];
                }
            }
        }
        return null;
    }
}
