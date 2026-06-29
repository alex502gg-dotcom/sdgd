package com.sellerplugin;

import com.sellerplugin.commands.SellerCommand;
import com.sellerplugin.config.ShopConfig;
import com.sellerplugin.economy.EconomyManager;
import com.sellerplugin.listeners.GuiListener;
import org.bukkit.plugin.java.JavaPlugin;

public class SellerPlugin extends JavaPlugin {

    private static SellerPlugin instance;
    private ShopConfig shopConfig;
    private EconomyManager economyManager;
    private GuiListener guiListener;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        shopConfig = new ShopConfig(this);
        shopConfig.load();

        economyManager = new EconomyManager(this);
        if (!economyManager.setup()) {
            getLogger().severe("EssentialsX not found or economy not available! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        guiListener = new GuiListener(this);
        getServer().getPluginManager().registerEvents(guiListener, this);

        getCommand("seller").setExecutor(new SellerCommand(this));

        getLogger().info("SellerPlugin enabled successfully!");
    }

    @Override
    public void onDisable() {
        getLogger().info("SellerPlugin disabled.");
    }

    public void reloadShopConfig() {
        reloadConfig();
        shopConfig.load();
    }

    public static SellerPlugin getInstance()  { return instance; }
    public ShopConfig getShopConfig()         { return shopConfig; }
    public EconomyManager getEconomyManager() { return economyManager; }
    public GuiListener getGuiListener()       { return guiListener; }
}
