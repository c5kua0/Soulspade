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

public class Soulspade extends JavaPlugin implements CommandExecutor {

    private NamespacedKey soulspadeKey;

    @Override
    public void onEnable() {

        soulspadeKey = new NamespacedKey(this, "soulspade");

        getCommand("soulspade").setExecutor(this);

        getServer().getPluginManager().registerEvents(
                new SoulspadeListener(this),
                this
        );

        getLogger().info("Soulspade has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Soulspade has been disabled!");
    }

    public ItemStack createSoulspade() {

        ItemStack item = new ItemStack(Material.NETHERITE_SHOVEL);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "Soulspade");

        meta.setLore(java.util.Arrays.asList(
                ChatColor.GRAY + "A weapon forged with soul energy.",
                "",
                ChatColor.AQUA + "Skills:",
                ChatColor.WHITE + "1. " + ChatColor.BLUE + "Dash",
                ChatColor.WHITE + "2. " + ChatColor.AQUA + "Energy Blast",
                ChatColor.WHITE + "3. " + ChatColor.WHITE + "Explosive Snowball",
                "",
                ChatColor.DARK_GRAY + "Use your hotbar to select a skill."
        ));

        meta.getPersistentDataContainer().set(
                soulspadeKey,
                PersistentDataType.BYTE,
                (byte) 1
        );

        item.setItemMeta(meta);

        return item;
    }

    public boolean isSoulspade(ItemStack item) {

        if (item == null || item.getType() != Material.NETHERITE_SHOVEL) {
            return false;
        }

        if (!item.hasItemMeta()) {
            return false;
        }

        return item.getItemMeta()
                .getPersistentDataContainer()
                .has(soulspadeKey, PersistentDataType.BYTE);
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        player.getInventory().addItem(createSoulspade());

        player.sendMessage(
                ChatColor.AQUA + "You received the " +
                ChatColor.DARK_AQUA + "" + ChatColor.BOLD +
                "Soulspade" +
                ChatColor.AQUA + "!"
        );

        return true;
    }

    public NamespacedKey getSoulspadeKey() {
        return soulspadeKey;
    }
              }
