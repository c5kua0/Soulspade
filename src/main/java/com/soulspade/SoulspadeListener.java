package com.soulspade;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
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

    // ==========================================
    // HOTBAR
    // ==========================================

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
                    color(plugin.getConfig().getString(
                            "messages.dash-selected",
                            "&bSoulspade: &fDash"
                    ))
            );
        }

        else if (slot == 1) {
            player.sendActionBar(
                    color(plugin.getConfig().getString(
                            "messages.energy-selected",
                            "&bSoulspade: &fEnergy Blast"
                    ))
            );
        }

        else if (slot == 2) {
            player.sendActionBar(
                    color(plugin.getConfig().getString(
                            "messages.snowball-selected",
                            "&bSoulspade: &fExplosive Snowball"
                    ))
            );
        }
    }

    // ==========================================
    // RIGHT CLICK
    // ==========================================

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {

        if (event.getAction() != Action.RIGHT_CLICK_AIR &&
                event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();

        if (!plugin.isSoulspade(
                player.getInventory().getItemInMainHand())) {
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

    // ==========================================
    // SKILL 1 - DASH
    // ==========================================

    private void dash(Player player) {

        if (!plugin.getConfig().getBoolean("dash.enabled", true)) {
            sendDisabledMessage(player);
            return;
        }

        UUID uuid = player.getUniqueId();

        double cooldown =
                plugin.getConfig().getDouble("dash.cooldown", 3.0);

        if (isOnCooldown(dashCooldown, uuid, cooldown)) {
            sendCooldownMessage(player);
            return;
        }

        dashCooldown.put(uuid, System.currentTimeMillis());

        double range =
                plugin.getConfig().getDouble("dash.range", 10.0);

        double damage =
                plugin.getConfig().getDouble("dash.damage", 4.0);

        Vector direction =
                player.getLocation().getDirection().normalize();

        Location start =
                player.getLocation().clone();

        for (double distance = 0; distance <= range; distance += 0.5) {

            Location location =
                    start.clone().add(
                            direction.clone().multiply(distance)
                    );

            if (location.getBlock().getType().isSolid()) {
                break;
            }

            // Trail particles
            if (plugin.getConfig().getBoolean(
                    "dash.particles.enabled", true)) {

                String particleName =
                        plugin.getConfig().getString(
                                "dash.particles.trail",
                                "SOUL"
                        );

                int amount =
                        plugin.getConfig().getInt(
                                "dash.particles.amount",
                                8
                        );

                spawnParticle(
                        location,
                        particleName,
                        amount
                );
            }

            // Damage and effects
            for (Entity entity :
                    location.getWorld().getNearbyEntities(
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

                if (damage > 0) {
                    target.damage(damage, player);
                }

                applyConfiguredEffect(
                        target,
                        "dash.effects.slowness",
                        PotionEffectType.SLOWNESS
                );

                applyConfiguredEffect(
                        target,
                        "dash.effects.weakness",
                        PotionEffectType.WEAKNESS
                );
            }
        }

        // Move the player
        Location destination =
                start.clone().add(
                        direction.multiply(range)
                );

        if (!destination.getBlock().getType().isSolid()) {
            player.teleport(destination);
        }

        player.getWorld().playSound(
                player.getLocation(),
                Sound.ENTITY_PLAYER_ATTACK_SWEEP,
                1.0f,
                0.7f
        );
    }

    // ==========================================
    // SKILL 2 - ENERGY BLAST
    // ==========================================

    private void energyBlast(Player player) {

        if (!plugin.getConfig().getBoolean(
                "energy-blast.enabled", true)) {
            sendDisabledMessage(player);
            return;
        }

        UUID uuid = player.getUniqueId();

        double cooldown =
                plugin.getConfig().getDouble(
                        "energy-blast.cooldown",
                        5.0
                );

        if (isOnCooldown(blastCooldown, uuid, cooldown)) {
            sendCooldownMessage(player);
            return;
        }

        blastCooldown.put(uuid, System.currentTimeMillis());

        double range =
                plugin.getConfig().getDouble(
                        "energy-blast.range",
                        7.0
                );

        double damage =
                plugin.getConfig().getDouble(
                        "energy-blast.damage",
                        15.0
                );

        Location start =
                player.getEyeLocation();

        Vector direction =
                start.getDirection().normalize();

        for (double distance = 0.5;
             distance <= range;
             distance += 0.25) {

            Location location =
                    start.clone().add(
                            direction.clone().multiply(distance)
                    );

            if (location.getBlock().getType().isSolid()) {
                break;
            }

            if (plugin.getConfig().getBoolean(
                    "energy-blast.particles.enabled",
                    true)) {

                spawnParticle(
                        location,
                        plugin.getConfig().getString(
                                "energy-blast.particles.primary",
                                "END_ROD"
                        ),
                        plugin.getConfig().getInt(
                                "energy-blast.particles.amount",
                                10
                        )
                );

                spawnParticle(
                        location,
                        plugin.getConfig().getString(
                                "energy-blast.particles.secondary",
                                "SOUL_FIRE_FLAME"
                        ),
                        4
                );
            }

            for (Entity entity :
                    location.getWorld().getNearbyEntities(
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

                target.damage(damage, player);

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

    // ==========================================
    // SKILL 3 - EXPLOSIVE SNOWBALL
    // ==========================================

    private void explosiveSnowball(Player player) {

        if (!plugin.getConfig().getBoolean(
                "explosive-snowball.enabled",
                true)) {
            sendDisabledMessage(player);
            return;
        }

        Snowball snowball =
                player.launchProjectile(Snowball.class);

        snowball.setCustomName(
                "Soulspade Explosive Snowball"
        );

        snowball.setCustomNameVisible(false);
    }

    // ==========================================
    // CONFIGURED POTION EFFECT
    // ==========================================

    private void applyConfiguredEffect(
            LivingEntity target,
            String path,
            PotionEffectType type
    ) {

        boolean enabled =
                plugin.getConfig().getBoolean(
                        path + ".enabled",
                        true
                );

        if (!enabled) {
            return;
        }

        int duration =
                plugin.getConfig().getInt(
                        path + ".duration",
                        5
                );

        int amplifier =
                plugin.getConfig().getInt(
                        path + ".amplifier",
                        1
                );

        target.addPotionEffect(
                new PotionEffect(
                        type,
                        duration * 20,
                        amplifier
                )
        );
    }

    // ==========================================
    // PARTICLE
    // ==========================================

    private void spawnParticle(
            Location location,
            String particleName,
            int amount
    ) {

        try {

            Particle particle =
                    Particle.valueOf(
                            particleName.toUpperCase()
                    );

            location.getWorld().spawnParticle(
                    particle,
                    location,
                    amount,
                    0.15,
                    0.15,
                    0.15,
                    0.02
            );

        } catch (IllegalArgumentException ignored) {
            plugin.getLogger().warning(
                    "Invalid particle in config: "
                            + particleName
            );
        }
    }

    // ==========================================
    // COOLDOWN
    // ==========================================

    private boolean isOnCooldown(
            Map<UUID, Long> cooldowns,
            UUID uuid,
            double seconds
    ) {

        if (!cooldowns.containsKey(uuid)) {
            return false;
        }

        long elapsed =
                System.currentTimeMillis()
                        - cooldowns.get(uuid);

        return elapsed < (long) (seconds * 1000);
    }

    // ==========================================
    // MESSAGES
    // ==========================================

    private void sendDisabledMessage(Player player) {

        player.sendMessage(
                color(plugin.getConfig().getString(
                        "messages.skill-disabled",
                        "&cThis skill is currently disabled."
                ))
        );
    }

    private void sendCooldownMessage(Player player) {

        player.sendMessage(
                color(plugin.getConfig().getString(
                        "messages.cooldown",
                        "&cSkill is on cooldown!"
                ))
        );
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes(
                '&',
                text
        );
    }
}