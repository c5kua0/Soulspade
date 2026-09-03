package com.soulspade;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;

public class Soulspade extends JavaPlugin implements CommandExecutor {

    private NamespacedKey soulspadeKey;

    @Override
    public void onEnable() {

        // Load config.yml
        saveDefaultConfig();
        reloadConfig();

        soulspadeKey = new NamespacedKey(this, "soulspade");

        // Register command
        if (getCommand("soulspade") != null) {
            getCommand("soulspade").setExecutor(this);
        }

        // Register listeners
        getServer().getPluginManager().registerEvents(
                new SoulspadeListener(this),
                this
        );

        getServer().getPluginManager().registerEvents(
                new SoulspadeProjectileListener(this),
                this
        );

        getLogger().info("Soulspade has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Soulspade has been disabled!");
    }

    // ==========================================
    // CREATE SOULSPADE
    // ==========================================

    public ItemStack createSoulspade() {

        ItemStack item =
                new ItemStack(Material.NETHERITE_SHOVEL);

        ItemMeta meta = item.getItemMeta();

        String configuredName =
                getConfig().getString(
                        "weapon.name",
                        "&3&lSoulspade"
                );

        meta.setDisplayName(color(configuredName));

        meta.setLore(Arrays.asList(
                color("&7A weapon forged with soul energy."),
                "",
                color("&bSkills:"),
                color("&f1. &9Dash"),
                color("&f2. &bEnergy Blast"),
                color("&f3. &fExplosive Snowball"),
                "",
                color("&8Select a skill using your hotbar."),
                color("&8Right-click to cast.")
        ));

        // Mark the item as Soulspade
        meta.getPersistentDataContainer().set(
                soulspadeKey,
                PersistentDataType.BYTE,
                (byte) 1
        );

        item.setItemMeta(meta);

        return item;
    }

    // ==========================================
    // CHECK SOULSPADE
    // ==========================================

    public boolean isSoulspade(ItemStack item) {

        if (item == null) {
            return false;
        }

        if (item.getType() != Material.NETHERITE_SHOVEL) {
            return false;
        }

        if (!item.hasItemMeta()) {
            return false;
        }

        return item.getItemMeta()
                .getPersistentDataContainer()
                .has(
                        soulspadeKey,
                        PersistentDataType.BYTE
                );
    }

    // ==========================================
    // /SOULSPADE COMMAND
    // ==========================================

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {

        if (!(sender instanceof Player player)) {

            sender.sendMessage(
                    "Only players can use this command."
            );

            return true;
        }

        if (!player.hasPermission("soulspade.use")) {

            player.sendMessage(
                    color("&cYou don't have permission to use this command.")
            );

            return true;
        }

        player.getInventory().addItem(
                createSoulspade()
        );

        String message =
                getConfig().getString(
                        "messages.received",
                        "&bYou received the &3&lSoulspade&b!"
                );

        player.sendMessage(color(message));

        return true;
    }

    // ==========================================
    // COLOR
    // ==========================================

    private String color(String text) {

        return ChatColor.translateAlternateColorCodes(
                '&',
                text
        );
    }

    // ==========================================
    // GET KEY
    // ==========================================

    public NamespacedKey getSoulspadeKey() {
        return soulspadeKey;
    }
}