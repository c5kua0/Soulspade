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

    /*
     * ==========================================
     * PREVENT DROPPING
     * ==========================================
     */

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {

        ItemStack item =
                event.getItemDrop().getItemStack();

        if (!plugin.isSoulspade(item)) {
            return;
        }

        event.setCancelled(true);

        event.getPlayer().sendActionBar(
                "§cThe Soulspade cannot be dropped!"
        );
    }

    /*
     * ==========================================
     * PREVENT INVENTORY MOVEMENT
     * ==========================================
     */

    @EventHandler
    public void onInventoryClick(
            InventoryClickEvent event
    ) {

        if (!(event.getWhoClicked()
                instanceof Player player)) {
            return;
        }

        ItemStack current =
                event.getCurrentItem();

        ItemStack cursor =
                event.getCursor();

        /*
         * Soulspade in the clicked slot.
         */
        if (plugin.isSoulspade(current)) {

            event.setCancelled(true);

            player.sendActionBar(
                    "§cThe Soulspade cannot be moved!"
            );

            return;
        }

        /*
         * Soulspade on the cursor.
         */
        if (plugin.isSoulspade(cursor)) {

            event.setCancelled(true);

            player.sendActionBar(
                    "§cThe Soulspade cannot be moved!"
            );
        }
    }

    /*
     * ==========================================
     * PREVENT INVENTORY DRAGGING
     * ==========================================
     */

    @EventHandler
    public void onInventoryDrag(
            InventoryDragEvent event
    ) {

        /*
         * Soulspade already on cursor.
         */
        if (plugin.isSoulspade(
                event.getOldCursor()
        )) {

            event.setCancelled(true);
            return;
        }

        /*
         * Soulspade being dragged into slots.
         */
        for (ItemStack item :
                event.getNewItems().values()) {

            if (plugin.isSoulspade(item)) {

                event.setCancelled(true);
                return;
            }
        }
    }

    /*
     * ==========================================
     * PREVENT BLOCK PLACEMENT
     * ==========================================
     */

    @EventHandler
    public void onBlockPlace(
            BlockPlaceEvent event
    ) {

        if (!plugin.isSoulspade(
                event.getItemInHand()
        )) {
            return;
        }

        event.setCancelled(true);

        event.getPlayer().sendActionBar(
                "§cThe Soulspade cannot be placed!"
        );
    }

    /*
     * ==========================================
     * PREVENT PICKUP
     * ==========================================
     */

    @EventHandler
    public void onPickup(
            EntityPickupItemEvent event
    ) {

        if (!(event.getEntity()
                instanceof Player player)) {
            return;
        }

        ItemStack item =
                event.getItem().getItemStack();

        if (!plugin.isSoulspade(item)) {
            return;
        }

        /*
         * Prevent anyone from picking up
         * a dropped Soulspade.
         *
         * This keeps the weapon bound to
         * the original owner.
         */
        event.setCancelled(true);

        player.sendActionBar(
                "§cThe Soulspade belongs to its owner!"
        );
    }

    /*
     * ==========================================
     * REMOVE DROPPED SOULSPADE
     * ==========================================
     *
     * Extra protection:
     * if a Soulspade somehow becomes an
     * ItemEntity, remove it instead of
     * allowing it to remain on the ground.
     */

    @EventHandler
    public void onItemSpawn(
            ItemSpawnEvent event
    ) {

        ItemStack item =
                event.getEntity().getItemStack();

        if (!plugin.isSoulspade(item)) {
            return;
        }

        event.getEntity().remove();
    }
}