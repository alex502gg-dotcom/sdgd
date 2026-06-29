package com.sellerplugin.economy;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;
import com.sellerplugin.SellerPlugin;
import net.ess3.api.Economy;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.math.BigDecimal;

public class EconomyManager {

    private final SellerPlugin plugin;
    private Essentials essentials;

    public EconomyManager(SellerPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Hook into EssentialsX. Returns true if successful.
     */
    public boolean setup() {
        Plugin ess = plugin.getServer().getPluginManager().getPlugin("Essentials");
        if (ess instanceof Essentials) {
            essentials = (Essentials) ess;
            plugin.getLogger().info("Hooked into EssentialsX economy.");
            return true;
        }
        return false;
    }

    public double getBalance(Player player) {
        User user = essentials.getUser(player);
        if (user == null) return 0;
        return user.getMoney().doubleValue();
    }

    public boolean has(Player player, double amount) {
        return getBalance(player) >= amount;
    }

    /**
     * Withdraw money from player. Returns true on success.
     */
    public boolean withdraw(Player player, double amount) {
        User user = essentials.getUser(player);
        if (user == null) return false;
        try {
            Economy.subtract(user, BigDecimal.valueOf(amount));
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to withdraw from " + player.getName() + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Deposit money to player. Returns true on success.
     */
    public boolean deposit(Player player, double amount) {
        User user = essentials.getUser(player);
        if (user == null) return false;
        try {
            Economy.add(user, BigDecimal.valueOf(amount));
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to deposit to " + player.getName() + ": " + e.getMessage());
            return false;
        }
    }

    public String format(double amount) {
        return String.format("%.2f", amount);
    }
}
