package com.sellerplugin.config;

import org.bukkit.Material;
import java.util.List;

public class ShopItem {

    private final int slot;
    private final int page;
    private final Material material;
    private final String displayName;
    private final List<String> lore;
    private final double buyPrice;
    private final double sellPrice;
    private final int amount;

    public ShopItem(int slot, int page, Material material, String displayName,
                    List<String> lore, double buyPrice, double sellPrice, int amount) {
        this.slot        = slot;
        this.page        = page;
        this.material    = material;
        this.displayName = displayName;
        this.lore        = lore;
        this.buyPrice    = buyPrice;
        this.sellPrice   = sellPrice;
        this.amount      = amount;
    }

    public int      getSlot()        { return slot; }
    public int      getPage()        { return page; }
    public Material getMaterial()    { return material; }
    public String   getDisplayName() { return displayName; }
    public List<String> getLore()    { return lore; }
    public double   getBuyPrice()    { return buyPrice; }
    public double   getSellPrice()   { return sellPrice; }
    public int      getAmount()      { return amount; }

    public boolean isBuyable()  { return buyPrice  > 0; }
    public boolean isSellable() { return sellPrice > 0; }
}
