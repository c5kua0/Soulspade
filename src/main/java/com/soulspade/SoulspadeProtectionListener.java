package com.soulspade;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;

public class SoulspadeProtectionListener implements Listener {

    private final Soulspade plugin;

    public SoulspadeProtectionListener(Soulspade plugin) {
        this.plugin = plugin;
    }

    // ==========================================
    // PREVENT DROPPING
    // ==========================================

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {

        ItemStack item = event.getItemDrop().getItemStack();

        if (plugin.isSoulspade(item)) {
            event.setCancelled(true);
        }
    }

    // ==========================================
    // PREVENT PUTTING INTO CONTAINERS
    // ==========================================

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();

        // Soulspade being moved from its inventory slot
        if (plugin.isSoulspade(current)) {

            // Allow normal inventory movement only.
            // Prevent moving it into another inventory.
            if (event.getClickedInventory() != null
                    && event.getView().getTopInventory()
                    != event.getClickedInventory()) {

                return;
            }

            event.setCancelled(true);
            return;
        }

        // Soulspade being placed into a container
        if (plugin.isSoulspade(cursor)) {
            event.setCancelled(true);
        }
    }

    // ==========================================
    // PREVENT INVENTORY DRAGGING
    // ==========================================

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {

        if (plugin.isSoulspade(event.getOldCursor())) {
            event.setCancelled(true);
            return;
        }

        for (ItemStack item : event.getNewItems().values()) {

            if (plugin.isSoulspade(item)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    // ==========================================
    // PREVENT PLACING
    // ==========================================

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {

        if (plugin.isSoulspade(
                event.getItemInHand())) {

            event.setCancelled(true);
        }
    }

    // ==========================================
    // PREVENT PICKUP OF DROPPED SOULSPADE
    // ==========================================
    //
    // This is mainly a safety check in case an
    // external plugin creates a Soulspade drop.
    //

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {

        if (event.getEntity() instanceof Player player) {

            ItemStack item =
                    event.getItem().getItemStack();

            if (plugin.isSoulspade(item)) {

                // Soulspade should never normally
                // exist as a dropped item.
                event.setCancelled(true);
            }
        }
    }

    // ==========================================
    // PREVENT SPAWNING SOULSPADE AS DROPPED ITEM
    // ==========================================

    @EventHandler
    public void onItemSpawn(ItemSpawnEvent event) {

        ItemStack item =
                event.getEntity().getItemStack();

        if (plugin.isSoulspade(item)) {
            event.getEntity().remove();
        }
    }
}