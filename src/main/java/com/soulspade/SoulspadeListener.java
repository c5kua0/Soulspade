package com.soulspade;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
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

    /*
     * ==========================================
     * HOTBAR SKILL SELECTOR
     * ==========================================
     *
     * Slot 1 = Dash
     * Slot 2 = Energy Blast
     * Slot 3 = Explosive Snowball
     *
     * Slots 4-9 work normally.
     */

    @EventHandler
    public void onHotbarChange(PlayerItemHeldEvent event) {

        Player player = event.getPlayer();

        ItemStack mainHand =
                player.getInventory()
                        .getItemInMainHand();

        /*
         * Only activate the selector while
         * holding Soulspade.
         */
        if (!plugin.isSoulspade(mainHand)) {
            return;
        }

        /*
         * Only the configured owner can use it.
         */
        if (!plugin.isOwner(player)) {
            return;
        }

        int newSlot =
                event.getNewSlot();

        /*
         * Slot 1
         */
        if (newSlot == 0) {

            event.setCancelled(true);

            plugin.setSelectedSkill(
                    player.getUniqueId(),
                    1
            );

            player.sendActionBar(
                    color(
                            "&b⚔ Dash &7selected"
                    )
            );

            player.playSound(
                    player.getLocation(),
                    Sound.UI_BUTTON_CLICK,
                    0.8f,
                    1.2f
            );

            return;
        }

        /*
         * Slot 2
         */
        if (newSlot == 1) {

            event.setCancelled(true);

            plugin.setSelectedSkill(
                    player.getUniqueId(),
                    2
            );

            player.sendActionBar(
                    color(
                            "&b⚡ Energy Blast &7selected"
                    )
            );

            player.playSound(
                    player.getLocation(),
                    Sound.UI_BUTTON_CLICK,
                    0.8f,
                    1.4f
            );

            return;
        }

        /*
         * Slot 3
         */
        if (newSlot == 2) {

            event.setCancelled(true);

            plugin.setSelectedSkill(
                    player.getUniqueId(),
                    3
            );

            player.sendActionBar(
                    color(
                            "&f❄ Explosive Snowball &7selected"
                    )
            );

            player.playSound(
                    player.getLocation(),
                    Sound.UI_BUTTON_CLICK,
                    0.8f,
                    1.6f
            );
        }

        /*
         * Slots 4-9 are not cancelled.
         * The player can switch normally.
         */
    }

    /*
     * ==========================================
     * RIGHT CLICK
     * ==========================================
     */

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {

        Action action =
                event.getAction();

        if (action != Action.RIGHT_CLICK_AIR &&
                action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player =
                event.getPlayer();

        ItemStack item =
                event.getItem();

        if (!plugin.isSoulspade(item)) {
            return;
        }

        /*
         * Only owner can use the weapon.
         */
        if (!plugin.isOwner(player)) {

            event.setCancelled(true);

            player.sendActionBar(
                    color(
                            "&cYou are not the owner of the Soulspade."
                    )
            );

            return;
        }

        /*
         * Prevent shovel interaction.
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

    /*
     * ==========================================
     * DASH
     * ==========================================
     */

    private void useDash(Player player) {

        if (!plugin.getConfig().getBoolean(
                "dash.enabled",
                true
        )) {

            sendMessage(
                    player,
                    "messages.skill-disabled"
            );

            return;
        }

        UUID uuid =
                player.getUniqueId();

        long remaining =
                plugin.getRemainingDashCooldown(
                        uuid
                );

        if (remaining > 0) {

            sendCooldownMessage(
                    player,
                    remaining
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

        /*
         * Start cooldown immediately.
         */
        plugin.startDashCooldown(uuid);

        Location start =
                player.getLocation().clone();

        Vector direction =
                start.getDirection()
                        .normalize();

        double actualDistance = 0.0;

        Set<UUID> hitEntities =
                new HashSet<>();

        /*
         * Move in 0.5 block increments.
         */
        for (
                double distance = 0.5;
                distance <= range;
                distance += 0.5
        ) {

            Location location =
                    start.clone().add(
                            direction.clone()
                                    .multiply(distance)
                    );

            /*
             * Stop if a solid block is reached.
             */
            if (location.getBlock().getType()
                    != Material.AIR &&
                    !location.getBlock()
                            .isPassable()) {
                break;
            }

            /*
             * Particle trail.
             */
            if (plugin.getConfig().getBoolean(
                    "dash.particles.enabled",
                    true
            )) {

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

            /*
             * Detect nearby entities.
             */
            for (Entity entity :
                    location.getWorld()
                            .getNearbyEntities(
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

                /*
                 * Prevent hitting the same target
                 * multiple times.
                 */
                if (hitEntities.contains(
                        target.getUniqueId()
                )) {
                    continue;
                }

                hitEntities.add(
                        target.getUniqueId()
                );

                /*
                 * Damage.
                 */
                if (damage > 0) {

                    target.damage(
                            damage,
                            player
                    );
                }

                /*
                 * Slowness.
                 */
                if (plugin.getConfig().getBoolean(
                        "dash.effects.slowness.enabled",
                        true
                )) {

                    int duration =
                            plugin.getConfig().getInt(
                                    "dash.effects.slowness.duration",
                                    5
                            );

                    int amplifier =
                            plugin.getConfig().getInt(
                                    "dash.effects.slowness.amplifier",
                                    1
                            );

                    target.addPotionEffect(
                            new PotionEffect(
                                    PotionEffectType.SLOWNESS,
                                    duration * 20,
                                    amplifier,
                                    false,
                                    true,
                                    true
                            )
                    );
                }

                /*
                 * Weakness.
                 */
                if (plugin.getConfig().getBoolean(
                        "dash.effects.weakness.enabled",
                        true
                )) {

                    int duration =
                            plugin.getConfig().getInt(
                                    "dash.effects.weakness.duration",
                                    5
                            );

                    int amplifier =
                            plugin.getConfig().getInt(
                                    "dash.effects.weakness.amplifier",
                                    1
                            );

                    target.addPotionEffect(
                            new PotionEffect(
                                    PotionEffectType.WEAKNESS,
                                    duration * 20,
                                    amplifier,
                                    false,
                                    true,
                                    true
                            )
                    );
                }

                /*
                 * Hit particle.
                 */
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

            actualDistance =
                    distance;
        }

        /*
         * Teleport player to the final safe position.
         */
        Location destination =
                start.clone().add(
                        direction.clone()
                                .multiply(actualDistance)
                );

        destination.setYaw(
                player.getLocation().getYaw()
        );

        destination.setPitch(
                player.getLocation().getPitch()
        );

        player.teleport(destination);

        player.getWorld().playSound(
                player.getLocation(),
                Sound.ENTITY_ENDERMAN_TELEPORT,
                1.0f,
                1.5f
        );

        player.getWorld().spawnParticle(
                Particle.SOUL,
                player.getLocation()
                        .add(0, 1, 0),
                25,
                0.5,
                0.8,
                0.5,
                0.05
        );
    }

    /*
     * ==========================================
     * ENERGY BLAST
     * ==========================================
     */

    private void useEnergyBlast(Player player) {

        if (!plugin.getConfig().getBoolean(
                "energy-blast.enabled",
                true
        )) {

            sendMessage(
                    player,
                    "messages.skill-disabled"
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

            sendCooldownMessage(
                    player,
                    remaining
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

        double hitRadius =
                plugin.getConfig().getDouble(
                        "energy-blast.hit-radius",
                        1.0
                );

        plugin.startEnergyBlastCooldown(uuid);

        Location start =
                player.getEyeLocation().clone();

        Vector direction =
                start.getDirection()
                        .normalize();

        Set<UUID> hitEntities =
                new HashSet<>();

        double actualDistance = 0.0;

        /*
         * Beam.
         */
        for (
                double distance = 0.25;
                distance <= range;
                distance += 0.25
        ) {

            Location location =
                    start.clone().add(
                            direction.clone()
                                    .multiply(distance)
                    );

            /*
             * Stop at solid blocks.
             */
            if (location.getBlock().getType()
                    != Material.AIR &&
                    !location.getBlock()
                            .isPassable()) {

                break;
            }

            actualDistance =
                    distance;

            /*
             * Beam particles.
             */
            if (plugin.getConfig().getBoolean(
                    "energy-blast.particles.enabled",
                    true
            )) {

                String primary =
                        plugin.getConfig().getString(
                                "energy-blast.particles.primary",
                                "END_ROD"
                        );

                String secondary =
                        plugin.getConfig().getString(
                                "energy-blast.particles.secondary",
                                "SOUL_FIRE_FLAME"
                        );

                int amount =
                        plugin.getConfig().getInt(
                                "energy-blast.particles.amount",
                                10
                        );

                spawnParticle(
                        location,
                        primary,
                        amount
                );

                spawnParticle(
                        location,
                        secondary,
                        Math.max(
                                1,
                                amount / 2
                        )
                );
            }

            /*
             * Detect targets.
             */
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

                if (target.equals(player)) {
                    continue;
                }

                /*
                 * Only hit each target once.
                 */
                if (hitEntities.contains(
                        target.getUniqueId()
                )) {
                    continue;
                }

                hitEntities.add(
                        target.getUniqueId()
                );

                /*
                 * Damage.
                 */
                if (damage > 0) {

                    target.damage(
                            damage,
                            player
                    );
                }

                /*
                 * Impact particles.
                 */
                target.getWorld().spawnParticle(
                        Particle.END_ROD,
                        target.getLocation()
                                .add(0, 1, 0),
                        25,
                        0.5,
                        0.7,
                        0.5,
                        0.05
                );

                target.getWorld().spawnParticle(
                        Particle.SOUL_FIRE_FLAME,
                        target.getLocation()
                                .add(0, 1, 0),
                        20,
                        0.4,
                        0.6,
                        0.4,
                        0.02
                );
            }
        }

        /*
         * Final impact.
         */
        Location impact =
                start.clone().add(
                        direction.clone()
                                .multiply(actualDistance)
                );

        impact.getWorld().spawnParticle(
                Particle.EXPLOSION,
                impact,
                1,
                0,
                0,
                0,
                0
        );

        impact.getWorld().spawnParticle(
                Particle.SOUL_FIRE_FLAME,
                impact,
                35,
                0.7,
                0.7,
                0.7,
                0.04
        );

        impact.getWorld().playSound(
                impact,
                Sound.ENTITY_GENERIC_EXPLODE,
                0.8f,
                1.8f
        );
    }

    /*
     * ==========================================
     * EXPLOSIVE SNOWBALL
     * ==========================================
     */

    private void useExplosiveSnowball(
            Player player
    ) {

        if (!plugin.getConfig().getBoolean(
                "explosive-snowball.enabled",
                true
        )) {

            sendMessage(
                    player,
                    "messages.skill-disabled"
            );

            return;
        }

        /*
         * No cooldown.
         */
        var snowball =
                player.launchProjectile(
                        org.bukkit.entity.Snowball.class
                );

        snowball.setCustomName(
                "Soulspade Explosive Snowball"
        );

        snowball.setCustomNameVisible(false);

        player.getWorld().playSound(
                player.getLocation(),
                Sound.ENTITY_SNOWBALL_THROW,
                1.0f,
                1.2f
        );

        /*
         * Small launch particles.
         */
        player.getWorld().spawnParticle(
                Particle.SNOWFLAKE,
                player.getEyeLocation(),
                12,
                0.2,
                0.2,
                0.2,
                0.02
        );
    }

    /*
     * ==========================================
     * PARTICLE HELPER
     * ==========================================
     */

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
                    0.08,
                    0.08,
                    0.08,
                    0.01
            );

        } catch (IllegalArgumentException ignored) {

            plugin.getLogger().warning(
                    "Invalid particle in config: "
                            + particleName
            );
        }
    }

    /*
     * ==========================================
     * MESSAGES
     * ==========================================
     */

    private void sendMessage(
            Player player,
            String path
    ) {

        String message =
                plugin.getConfig().getString(
                        path,
                        "&cThis skill is currently disabled."
                );

        player.sendMessage(
                color(message)
        );
    }

    private void sendCooldownMessage(
            Player player,
            long remaining
    ) {

        double seconds =
                remaining / 1000.0;

        String message =
                plugin.getConfig().getString(
                        "messages.cooldown",
                        "&cSkill is on cooldown!"
                );

        player.sendActionBar(
                color(
                        message
                                + " &7("
                                + String.format(
                                        java.util.Locale.US,
                                        "%.1f",
                                        seconds
                                )
                                + "s)"
                )
        );
    }

    private String color(String text) {

        return ChatColor.translateAlternateColorCodes(
                '&',
                text
        );
    }
}