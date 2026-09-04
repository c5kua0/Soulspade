package com.soulspade;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class SoulspadeListener implements Listener {

    private final Soulspade plugin;

    public SoulspadeListener(Soulspade plugin) {
        this.plugin = plugin;
    }

    // =========================================================
    // HOTBAR SKILL SELECTION
    // =========================================================

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onHotbarChange(PlayerItemHeldEvent event) {

        Player player = event.getPlayer();

        if (!plugin.isOwner(player)) {
            return;
        }

        ItemStack mainHand =
                player.getInventory().getItemInMainHand();

        /*
         * Only select skills while the Soulspade
         * is currently being held.
         */
        if (!plugin.isSoulspade(mainHand)) {
            return;
        }

        int newSlot = event.getNewSlot();

        /*
         * Slots are zero-based:
         *
         * 0 = Hotbar 1
         * 1 = Hotbar 2
         * 2 = Hotbar 3
         */

        if (newSlot == 0) {

            event.setCancelled(true);

            plugin.setSelectedSkill(
                    player.getUniqueId(),
                    1
            );

            sendSkillMessage(
                    player,
                    "messages.dash-selected",
                    "&b⚔ Dash"
            );

            return;
        }

        if (newSlot == 1) {

            event.setCancelled(true);

            plugin.setSelectedSkill(
                    player.getUniqueId(),
                    2
            );

            sendSkillMessage(
                    player,
                    "messages.energy-selected",
                    "&b⚡ Energy Blast"
            );

            return;
        }

        if (newSlot == 2) {

            event.setCancelled(true);

            plugin.setSelectedSkill(
                    player.getUniqueId(),
                    3
            );

            sendSkillMessage(
                    player,
                    "messages.snowball-selected",
                    "&f❄ Explosive Snowball"
            );
        }
    }

    // =========================================================
    // RIGHT CLICK
    // =========================================================

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRightClick(PlayerInteractEvent event) {

        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Action action = event.getAction();

        if (action != Action.RIGHT_CLICK_AIR &&
                action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();

        if (!plugin.isSoulspade(
                player.getInventory().getItemInMainHand()
        )) {
            return;
        }

        if (!plugin.isOwner(player)) {

            event.setCancelled(true);

            player.sendActionBar(
                    "§cOnly the Soulspade owner can use this weapon."
            );

            return;
        }

        /*
         * Prevent the shovel's normal right-click
         * behavior from interfering with the skill.
         */
        event.setCancelled(true);

        int skill =
                plugin.getSelectedSkill(
                        player.getUniqueId()
                );

        if (skill == 1) {

            useDash(player);

        } else if (skill == 2) {

            useEnergyBlast(player);

        } else if (skill == 3) {

            useExplosiveSnowball(player);
        }
    }

    // =========================================================
    // SKILL 1 - DASH
    // =========================================================

    private void useDash(Player player) {

        if (!plugin.getConfig().getBoolean(
                "dash.enabled",
                true
        )) {
            player.sendActionBar(
                    "§cDash is disabled."
            );
            return;
        }

        UUID uuid = player.getUniqueId();

        long remaining =
                plugin.getRemainingDashCooldown(uuid);

        if (remaining > 0) {

            player.sendActionBar(
                    "§cDash is on cooldown!"
            );

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

        Location start =
                player.getLocation().clone();

        Vector direction =
                start.getDirection().normalize();

        /*
         * Slight upward adjustment prevents the dash
         * from getting stuck on tiny ground differences.
         */
        direction.setY(
                Math.max(
                        direction.getY(),
                        -0.15
                )
        );

        direction.normalize();

        Location destination =
                start.clone();

        LivingEntity hitTarget = null;

        /*
         * Move in small steps.
         * This makes the dash stop before solid blocks
         * instead of teleporting through them.
         */
        double step = 0.25;

        for (double distance = 0;
             distance <= range;
             distance += step) {

            Location point =
                    start.clone().add(
                            direction.clone()
                                    .multiply(distance)
                    );

            Block block =
                    point.getBlock();

            if (!block.isPassable()) {
                break;
            }

            destination = point;

            spawnDashParticle(point);

            /*
             * Find the first living target in the path.
             */
            for (Entity entity :
                    point.getWorld()
                            .getNearbyEntities(
                                    point,
                                    0.8,
                                    1.0,
                                    0.8
                            )) {

                if (!(entity instanceof LivingEntity target)) {
                    continue;
                }

                if (target.equals(player)) {
                    continue;
                }

                hitTarget = target;
                break;
            }

            if (hitTarget != null) {
                break;
            }
        }

        /*
         * Keep the player's original facing direction.
         */
        destination.setYaw(
                player.getLocation().getYaw()
        );

        destination.setPitch(
                player.getLocation().getPitch()
        );

        player.teleport(destination);

        /*
         * Dash sound.
         */
        player.getWorld().playSound(
                destination,
                Sound.ENTITY_ENDERMAN_TELEPORT,
                1.0f,
                1.5f
        );

        /*
         * Damage the first target hit.
         */
        if (hitTarget != null && damage > 0) {

            hitTarget.damage(
                    damage,
                    player
            );

            applyDashEffects(
                    hitTarget
            );

            hitTarget.getWorld().spawnParticle(
                    Particle.SOUL,
                    hitTarget.getLocation()
                            .add(0, 1, 0),
                    20,
                    0.4,
                    0.7,
                    0.4,
                    0.05
            );
        }

        /*
         * Small burst at the destination.
         */
        player.getWorld().spawnParticle(
                Particle.SOUL,
                destination.clone().add(0, 1, 0),
                15,
                0.4,
                0.6,
                0.4,
                0.03
        );

        plugin.startDashCooldown(uuid);
    }

    // =========================================================
    // DASH EFFECTS
    // =========================================================

    private void applyDashEffects(
            LivingEntity target
    ) {

        boolean slowness =
                plugin.getConfig().getBoolean(
                        "dash.effects.slowness.enabled",
                        true
                );

        int slownessDuration =
                plugin.getConfig().getInt(
                        "dash.effects.slowness.duration",
                        5
                );

        int slownessAmplifier =
                plugin.getConfig().getInt(
                        "dash.effects.slowness.amplifier",
                        1
                );

        if (slowness) {

            target.addPotionEffect(
                    new PotionEffect(
                            PotionEffectType.SLOWNESS,
                            slownessDuration * 20,
                            slownessAmplifier
                    )
            );
        }

        boolean weakness =
                plugin.getConfig().getBoolean(
                        "dash.effects.weakness.enabled",
                        true
                );

        int weaknessDuration =
                plugin.getConfig().getInt(
                        "dash.effects.weakness.duration",
                        5
                );

        int weaknessAmplifier =
                plugin.getConfig().getInt(
                        "dash.effects.weakness.amplifier",
                        1
                );

        if (weakness) {

            target.addPotionEffect(
                    new PotionEffect(
                            PotionEffectType.WEAKNESS,
                            weaknessDuration * 20,
                            weaknessAmplifier
                    )
            );
        }
    }

    // =========================================================
    // DASH PARTICLES
    // =========================================================

    private void spawnDashParticle(
            Location location
    ) {

        if (!plugin.getConfig().getBoolean(
                "dash.particles.enabled",
                true
        )) {
            return;
        }

        World world =
                location.getWorld();

        if (world == null) {
            return;
        }

        world.spawnParticle(
                Particle.SOUL,
                location.clone().add(0, 0.6, 0),
                plugin.getConfig().getInt(
                        "dash.particles.amount",
                        8
                ),
                0.15,
                0.25,
                0.15,
                0.01
        );
    }

    // =========================================================
    // SKILL 2 - ENERGY BLAST
    // =========================================================

    private void useEnergyBlast(
            Player player
    ) {

        if (!plugin.getConfig().getBoolean(
                "energy-blast.enabled",
                true
        )) {
            player.sendActionBar(
                    "§cEnergy Blast is disabled."
            );
            return;
        }

        UUID uuid =
                player.getUniqueId();

        long remaining =
                plugin.getRemainingEnergyBlastCooldown(
                        uuid
                );

        if (remaining > 0) {

            player.sendActionBar(
                    "§cEnergy Blast is on cooldown!"
            );

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

        /*
         * Increased default hit radius.
         */
        double hitRadius =
                plugin.getConfig().getDouble(
                        "energy-blast.hit-radius",
                        1.5
                );

        Location start =
                player.getEyeLocation();

        Vector direction =
                start.getDirection().normalize();

        Set<UUID> hitEntities =
                new HashSet<>();

        Location finalPoint =
                start.clone();

        double step = 0.25;

        for (double distance = 0;
             distance <= range;
             distance += step) {

            Location point =
                    start.clone().add(
                            direction.clone()
                                    .multiply(distance)
                    );

            if (!point.getBlock().isPassable()) {
                break;
            }

            finalPoint = point;

            spawnBlastParticles(point);

            /*
             * Detect both players and mobs.
             */
            for (Entity entity :
                    point.getWorld()
                            .getNearbyEntities(
                                    point,
                                    hitRadius,
                                    hitRadius,
                                    hitRadius
                            )) {

                if (!(entity instanceof LivingEntity target)) {
                    continue;
                }

                if (target.equals(player)) {
                    continue;
                }

                UUID targetUUID =
                        target.getUniqueId();

                /*
                 * Each target can only be hit once
                 * by this cast.
                 */
                if (!hitEntities.add(
                        targetUUID
                )) {
                    continue;
                }

                if (damage > 0) {

                    target.damage(
                            damage,
                            player
                    );
                }

                target.getWorld().spawnParticle(
                        Particle.SOUL_FIRE_FLAME,
                        target.getLocation()
                                .add(0, 1, 0),
                        20,
                        0.5,
                        0.7,
                        0.5,
                        0.03
                );
            }
        }

        /*
         * Blast impact.
         */
        finalPoint.getWorld().spawnParticle(
                Particle.END_ROD,
                finalPoint,
                35,
                0.7,
                0.7,
                0.7,
                0.08
        );

        finalPoint.getWorld().spawnParticle(
                Particle.SOUL_FIRE_FLAME,
                finalPoint,
                25,
                0.5,
                0.5,
                0.5,
                0.04
        );

        finalPoint.getWorld().playSound(
                finalPoint,
                Sound.ENTITY_PLAYER_ATTACK_STRONG,
                1.0f,
                0.7f
        );

        plugin.startEnergyBlastCooldown(
                uuid
        );
    }

    // =========================================================
    // BLAST PARTICLES
    // =========================================================

    private void spawnBlastParticles(
            Location location
    ) {

        if (!plugin.getConfig().getBoolean(
                "energy-blast.particles.enabled",
                true
        )) {
            return;
        }

        World world =
                location.getWorld();

        if (world == null) {
            return;
        }

        world.spawnParticle(
                Particle.END_ROD,
                location,
                3,
                0.15,
                0.15,
                0.15,
                0.02
        );

        world.spawnParticle(
                Particle.SOUL_FIRE_FLAME,
                location,
                3,
                0.12,
                0.12,
                0.12,
                0.01
        );
    }

    // =========================================================
    // SKILL 3 - EXPLOSIVE SNOWBALL
    // =========================================================

    private void useExplosiveSnowball(
            Player player
    ) {

        if (!plugin.getConfig().getBoolean(
                "explosive-snowball.enabled",
                true
        )) {
            player.sendActionBar(
                    "§cExplosive Snowball is disabled."
            );
            return;
        }

        Snowball snowball =
                player.launchProjectile(
                        Snowball.class
                );

        snowball.setCustomName(
                "Soulspade Explosive Snowball"
        );

        snowball.setCustomNameVisible(false);

        /*
         * Slightly faster projectile.
         */
        snowball.setVelocity(
                player.getEyeLocation()
                        .getDirection()
                        .normalize()
                        .multiply(1.7)
        );

        player.getWorld().playSound(
                player.getLocation(),
                Sound.ENTITY_SNOWBALL_THROW,
                1.0f,
                0.8f
        );
    }

    // =========================================================
    // MESSAGE HELPER
    // =========================================================

    private void sendSkillMessage(
            Player player,
            String configPath,
            String fallback
    ) {

        String message =
                plugin.getConfig().getString(
                        configPath,
                        fallback
                );

        player.sendActionBar(
                color(message)
        );
    }

    private String color(
            String text
    ) {

        return org.bukkit.ChatColor
                .translateAlternateColorCodes(
                        '&',
                        text
                );
    }
}