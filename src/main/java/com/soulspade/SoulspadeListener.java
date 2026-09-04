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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class SoulspadeListener implements Listener {

    private final Soulspade plugin;

    private final Map<UUID, Long> dashCooldown = new HashMap<>();
    private final Map<UUID, Long> blastCooldown = new HashMap<>();

    /*
     * Selected skill:
     *
     * 1 = Dash
     * 2 = Energy Blast
     * 3 = Explosive Snowball
     */
    private final Map<UUID, Integer> selectedSkill = new HashMap<>();

    public SoulspadeListener(Soulspade plugin) {
        this.plugin = plugin;
    }

    // ==========================================
    // HOTBAR SKILL SELECTOR
    // ==========================================

    @EventHandler
    public void onHotbarChange(PlayerItemHeldEvent event) {

        Player player = event.getPlayer();

        ItemStack currentItem =
                player.getInventory().getItemInMainHand();

        // Skill selector only works while holding Soulspade
        if (!plugin.isSoulspade(currentItem)) {
            return;
        }

        int newSlot = event.getNewSlot();

        // ==========================================
        // SLOT 1 - DASH
        // ==========================================

        if (newSlot == 0) {

            event.setCancelled(true);

            selectedSkill.put(
                    player.getUniqueId(),
                    1
            );

            player.sendActionBar(
                    color(plugin.getConfig().getString(
                            "messages.dash-selected",
                            "&bSoulspade: &fDash"
                    ))
            );

            return;
        }

        // ==========================================
        // SLOT 2 - ENERGY BLAST
        // ==========================================

        if (newSlot == 1) {

            event.setCancelled(true);

            selectedSkill.put(
                    player.getUniqueId(),
                    2
            );

            player.sendActionBar(
                    color(plugin.getConfig().getString(
                            "messages.energy-selected",
                            "&bSoulspade: &fEnergy Blast"
                    ))
            );

            return;
        }

        // ==========================================
        // SLOT 3 - EXPLOSIVE SNOWBALL
        // ==========================================

        if (newSlot == 2) {

            event.setCancelled(true);

            selectedSkill.put(
                    player.getUniqueId(),
                    3
            );

            player.sendActionBar(
                    color(plugin.getConfig().getString(
                            "messages.snowball-selected",
                            "&bSoulspade: &fExplosive Snowball"
                    ))
            );

            return;
        }

        /*
         * Slots 4-9 work normally.
         * The player can switch away from Soulspade.
         */
    }

    // ==========================================
    // RIGHT CLICK = CAST SKILL
    // ==========================================

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {

        if (event.getAction() != Action.RIGHT_CLICK_AIR &&
                event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();

        // Player must be holding Soulspade
        if (!plugin.isSoulspade(
                player.getInventory().getItemInMainHand())) {
            return;
        }

        UUID uuid = player.getUniqueId();

        // Default skill = Dash
        int skill =
                selectedSkill.getOrDefault(uuid, 1);

        if (skill == 1) {
            dash(player);
        }

        else if (skill == 2) {
            energyBlast(player);
        }

        else if (skill == 3) {
            explosiveSnowball(player);
        }
    }

    // ==========================================
    // SKILL 1 - DASH
    // ==========================================

    private void dash(Player player) {

        if (!plugin.getConfig().getBoolean(
                "dash.enabled",
                true)) {

            sendDisabledMessage(player);
            return;
        }

        UUID uuid = player.getUniqueId();

        double cooldown =
                plugin.getConfig().getDouble(
                        "dash.cooldown",
                        3.0
                );

        if (isOnCooldown(
                dashCooldown,
                uuid,
                cooldown)) {

            sendCooldownMessage(player);
            return;
        }

        double range =
                plugin.getConfig().getDouble(
                        "dash.range",
                        10.0
                );

        double damage =
                plugin.getConfig().getDouble(
                        "dash.damage",
                        4.0
                );

        dashCooldown.put(
                uuid,
                System.currentTimeMillis()
        );

        Location start =
                player.getLocation().clone();

        Vector direction =
                player.getLocation()
                        .getDirection()
                        .normalize();

        Set<UUID> hitTargets =
                new HashSet<>();

        double actualDistance = 0;

        // ==========================================
        // DASH MOVEMENT
        // ==========================================

        for (
                double distance = 0;
                distance <= range;
                distance += 0.5
        ) {

            Location location =
                    start.clone().add(
                            direction.clone()
                                    .multiply(distance)
                    );

            // Stop before solid blocks
            if (location.getBlock()
                    .getType()
                    .isSolid()) {

                break;
            }

            actualDistance = distance;

            // ==========================================
            // DASH PARTICLE TRAIL
            // ==========================================

            if (plugin.getConfig().getBoolean(
                    "dash.particles.enabled",
                    true)) {

                spawnParticle(
                        location,
                        plugin.getConfig().getString(
                                "dash.particles.trail",
                                "SOUL"
                        ),
                        plugin.getConfig().getInt(
                                "dash.particles.amount",
                                8
                        )
                );
            }

            // ==========================================
            // HIT ENTITIES
            // ==========================================

            for (Entity entity :
                    location.getWorld()
                            .getNearbyEntities(
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

                // Don't hit the same target twice
                if (hitTargets.contains(
                        target.getUniqueId())) {
                    continue;
                }

                hitTargets.add(
                        target.getUniqueId()
                );

                // Damage
                if (damage > 0) {
                    target.damage(
                            damage,
                            player
                    );
                }

                // Slowness
                applyConfiguredEffect(
                        target,
                        "dash.effects.slowness",
                        PotionEffectType.SLOWNESS
                );

                // Weakness
                applyConfiguredEffect(
                        target,
                        "dash.effects.weakness",
                        PotionEffectType.WEAKNESS
                );

                // Hit particles
                target.getWorld().spawnParticle(
                        Particle.SOUL,
                        target.getLocation()
                                .add(0, 1, 0),
                        15,
                        0.4,
                        0.6,
                        0.4,
                        0.03
                );
            }
        }

        // ==========================================
        // TELEPORT TO SAFE DESTINATION
        // ==========================================

        Location destination =
                start.clone().add(
                        direction.multiply(
                                actualDistance
                        )
                );

        if (!destination.getBlock()
                .getType()
                .isSolid()) {

            player.teleport(destination);
        }

        // ==========================================
        // DASH SOUND
        // ==========================================

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
                "energy-blast.enabled",
                true)) {

            sendDisabledMessage(player);
            return;
        }

        UUID uuid = player.getUniqueId();

        double cooldown =
                plugin.getConfig().getDouble(
                        "energy-blast.cooldown",
                        5.0
                );

        if (isOnCooldown(
                blastCooldown,
                uuid,
                cooldown)) {

            sendCooldownMessage(player);
            return;
        }

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

        double hitRadius =
                plugin.getConfig().getDouble(
                        "energy-blast.hit-radius",
                        1.0
                );

        blastCooldown.put(
                uuid,
                System.currentTimeMillis()
        );

        Location start =
                player.getEyeLocation();

        Vector direction =
                start.getDirection()
                        .normalize();

        Set<UUID> hitTargets =
                new HashSet<>();

        // ==========================================
        // ENERGY BLAST BEAM
        // ==========================================

        for (
                double distance = 0.5;
                distance <= range;
                distance += 0.25
        ) {

            Location location =
                    start.clone().add(
                            direction.clone()
                                    .multiply(distance)
                    );

            // Stop at walls
            if (location.getBlock()
                    .getType()
                    .isSolid()) {

                break;
            }

            // ==========================================
            // ENERGY PARTICLES
            // ==========================================

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

            // ==========================================
            // DAMAGE TARGETS
            // ==========================================

            for (Entity entity :
                    location.getWorld()
                            .getNearbyEntities(
                                    location,
                                    hitRadius,
                                    hitRadius,
                                    hitRadius
                            )) {

                if (!(entity instanceof LivingEntity target)) {
                    continue;
                }

                // Don't hit caster
                if (target.equals(player)) {
                    continue;
                }

                // Don't hit same target twice
                if (hitTargets.contains(
                        target.getUniqueId())) {
                    continue;
                }

                hitTargets.add(
                        target.getUniqueId()
                );

                // Massive damage
                if (damage > 0) {
                    target.damage(
                            damage,
                            player
                    );
                }

                // Impact effect
                target.getWorld().spawnParticle(
                        Particle.EXPLOSION,
                        target.getLocation()
                                .add(0, 1, 0),
                        1
                );

                target.getWorld().spawnParticle(
                        Particle.END_ROD,
                        target.getLocation()
                                .add(0, 1, 0),
                        20,
                        0.5,
                        0.7,
                        0.5,
                        0.05
                );

                target.getWorld().playSound(
                        target.getLocation(),
                        Sound.ENTITY_GENERIC_EXPLODE,
                        0.7f,
                        1.5f
                );
            }
        }

        // ==========================================
        // CAST SOUND
        // ==========================================

        player.getWorld().playSound(
                player.getLocation(),
                Sound.ENTITY_EVOKER_CAST_SPELL,
                1.0f,
                1.2f
        );
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

        /*
         * No cooldown.
         */

        Snowball snowball =
                player.launchProjectile(
                        Snowball.class
                );

        snowball.setCustomName(
                "Soulspade Explosive Snowball"
        );

        snowball.setCustomNameVisible(false);
    }

    // ==========================================
    // POTION EFFECT
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
    // PARTICLE HELPER
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
    // COOLDOWN CHECK
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

        return elapsed <
                (long) (seconds * 1000);
    }

    // ==========================================
    // MESSAGES
    // ==========================================

    private void sendDisabledMessage(
            Player player) {

        player.sendMessage(
                color(plugin.getConfig().getString(
                        "messages.skill-disabled",
                        "&cThis skill is currently disabled."
                ))
        );
    }

    private void sendCooldownMessage(
            Player player) {

        player.sendMessage(
                color(plugin.getConfig().getString(
                        "messages.cooldown",
                        "&cSkill is on cooldown!"
                ))
        );
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
}