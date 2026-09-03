package com.soulspade;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SoulspadeListener implements Listener {

    private final Soulspade plugin;

    private final Map<UUID, Long> dashCooldown = new HashMap<>();
    private final Map<UUID, Long> blastCooldown = new HashMap<>();

    public SoulspadeListener(Soulspade plugin) {
        this.plugin = plugin;
    }

    // =========================
    // HOTBAR SKILL SELECTION
    // =========================

    @EventHandler
    public void onHotbarChange(PlayerItemHeldEvent event) {

        Player player = event.getPlayer();

        ItemStack item = player.getInventory().getItem(event.getNewSlot());

        if (!plugin.isSoulspade(item)) {
            return;
        }

        int slot = event.getNewSlot();

        if (slot == 0) {
            player.sendActionBar(
                    ChatColor.AQUA + "Soulspade: " +
                    ChatColor.WHITE + "Dash"
            );
        }

        else if (slot == 1) {
            player.sendActionBar(
                    ChatColor.AQUA + "Soulspade: " +
                    ChatColor.WHITE + "Energy Blast"
            );
        }

        else if (slot == 2) {
            player.sendActionBar(
                    ChatColor.AQUA + "Soulspade: " +
                    ChatColor.WHITE + "Explosive Snowball"
            );
        }
    }

    // =========================
    // RIGHT CLICK
    // =========================

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {

        if (event.getAction() != Action.RIGHT_CLICK_AIR &&
            event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();

        if (!plugin.isSoulspade(player.getInventory().getItemInMainHand())) {
            return;
        }

        int slot = player.getInventory().getHeldItemSlot();

        if (slot == 0) {
            dash(player);
        }

        else if (slot == 1) {
            energyBlast(player);
        }

        else if (slot == 2) {
            explosiveSnowball(player);
        }
    }

    // =========================
    // SKILL 1 - DASH
    // =========================

    private void dash(Player player) {

        UUID uuid = player.getUniqueId();

        if (isOnCooldown(dashCooldown, uuid, 3)) {
            return;
        }

        dashCooldown.put(uuid, System.currentTimeMillis());

        Vector direction = player.getLocation().getDirection().normalize();

        Location start = player.getLocation().clone();

        // 10 block dash
        for (int i = 0; i < 10; i++) {

            Location location = start.clone().add(
                    direction.clone().multiply(i)
            );

            // Stop if there is a solid block
            if (location.getBlock().getType().isSolid()) {
                break;
            }

            // Particle trail
            player.getWorld().spawnParticle(
                    Particle.SOUL,
                    location,
                    8,
                    0.25,
                    0.25,
                    0.25,
                    0.02
            );

            // Hit nearby enemies
            for (Entity entity : location.getWorld().getNearbyEntities(
                    location,
                    1.2,
                    1.2,
                    1.2
            )) {

                if (!(entity instanceof LivingEntity target)) {
                    continue;
                }

                if (target.equals(player)) {
                    continue;
                }

                target.addPotionEffect(
                        new org.bukkit.potion.PotionEffect(
                                org.bukkit.potion.PotionEffectType.SLOWNESS,
                                100,
                                1
                        )
                );

                target.addPotionEffect(
                        new org.bukkit.potion.PotionEffect(
                                org.bukkit.potion.PotionEffectType.WEAKNESS,
                                100,
                                1
                        )
                );
            }
        }

        Location destination = start.clone().add(
                direction.multiply(10)
        );

        player.teleport(destination);

        player.getWorld().playSound(
                player.getLocation(),
                Sound.ENTITY_PLAYER_ATTACK_SWEEP,
                1.0f,
                0.7f
        );
    }

    // =========================
    // SKILL 2 - ENERGY BLAST
    // =========================

    private void energyBlast(Player player) {

        UUID uuid = player.getUniqueId();

        if (isOnCooldown(blastCooldown, uuid, 5)) {
            return;
        }

        blastCooldown.put(uuid, System.currentTimeMillis());

        Location start = player.getEyeLocation();
        Vector direction = start.getDirection().normalize();

        for (int i = 1; i <= 7; i++) {

            Location location = start.clone().add(
                    direction.clone().multiply(i)
            );

            if (location.getBlock().getType().isSolid()) {
                break;
            }

            // Energy particles
            player.getWorld().spawnParticle(
                    Particle.END_ROD,
                    location,
                    10,
                    0.15,
                    0.15,
                    0.15,
                    0.03
            );

            player.getWorld().spawnParticle(
                    Particle.SOUL_FIRE_FLAME,
                    location,
                    6,
                    0.15,
                    0.15,
                    0.15,
                    0.01
            );

            // Hit enemies
            for (Entity entity : location.getWorld().getNearbyEntities(
                    location,
                    1.0,
                    1.0,
                    1.0
            )) {

                if (!(entity instanceof LivingEntity target)) {
                    continue;
                }

                if (target.equals(player)) {
                    continue;
                }

                // Massive damage
                target.damage(15.0, player);

                // Impact particles
                target.getWorld().spawnParticle(
                        Particle.EXPLOSION,
                        target.getLocation(),
                        1
                );

                target.getWorld().playSound(
                        target.getLocation(),
                        Sound.ENTITY_GENERIC_EXPLODE,
                        0.7f,
                        1.5f
                );

                return;
            }
        }
    }

    // =========================
    // SKILL 3 - EXPLOSIVE SNOWBALL
    // =========================

    private void explosiveSnowball(Player player) {

        Snowball snowball = player.launchProjectile(Snowball.class);

        snowball.setCustomName("Soulspade Explosive Snowball");
        snowball.setCustomNameVisible(false);

        // No cooldown
        // No block damage
        // Explosion happens when it hits
    }

    // =========================
    // COOLDOWN CHECK
    // =========================

    private boolean isOnCooldown(
            Map<UUID, Long> cooldowns,
            UUID uuid,
            int seconds
    ) {

        if (!cooldowns.containsKey(uuid)) {
            return false;
        }

        long lastUse = cooldowns.get(uuid);

        long elapsed =
                System.currentTimeMillis() - lastUse;

        return elapsed < seconds * 1000L;
    }
            }
