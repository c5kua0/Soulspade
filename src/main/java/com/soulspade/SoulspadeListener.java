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
import org.bukkit.event.entity.EntityDamageByEntityEvent;
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
    // LIFESTEAL
    // =========================================================

    @EventHandler(
            priority = EventPriority.MONITOR,
            ignoreCancelled = true
    )
    public void onSoulspadeDamage(
            EntityDamageByEntityEvent event
    ) {

        if (!(event.getEntity() instanceof LivingEntity)) {
            return;
        }

        Player attacker = null;

        if (event.getDamager() instanceof Player player) {
            attacker = player;
        }

        else if (event.getDamager() instanceof Snowball snowball) {

            if (!"Soulspade Explosive Snowball".equals(
                    snowball.getCustomName()
            )) {
                return;
            }

            if (snowball.getShooter() instanceof Player player) {
                attacker = player;
            }
        }

        if (attacker == null) {
            return;
        }

        if (!plugin.isOwner(attacker)) {
            return;
        }

        boolean snowball =
                event.getDamager() instanceof Snowball;

        if (!snowball &&
                !plugin.isSoulspade(
                        attacker.getInventory()
                                .getItemInMainHand()
                )) {
            return;
        }

        double percent =
                plugin.getConfig().getDouble(
                        "combat.lifesteal-percent",
                        20.0
                );

        if (percent <= 0) {
            return;
        }

        double damage =
                event.getFinalDamage();

        if (damage <= 0) {
            return;
        }

        healLifesteal(
                attacker,
                damage * percent / 100.0
        );
    }

    private void healLifesteal(
            Player player,
            double amount
    ) {

        if (amount <= 0) {
            return;
        }

        var maxHealth =
                player.getAttribute(
                        org.bukkit.attribute.Attribute.MAX_HEALTH
                );

        if (maxHealth == null) {
            return;
        }

        double max =
                maxHealth.getValue();

        double newHealth =
                Math.min(
                        player.getHealth() + amount,
                        max
                );

        player.setHealth(newHealth);

        if (plugin.getConfig().getBoolean(
                "combat.lifesteal-particles",
                true
        )) {

            player.getWorld().spawnParticle(
                    Particle.HEART,
                    player.getLocation()
                            .add(0, 2, 0),
                    3,
                    0.25,
                    0.25,
                    0.25,
                    0.02
            );
        }
    }

    // =========================================================
    // HOTBAR SKILL SELECTION
    // =========================================================

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onHotbarChange(
            PlayerItemHeldEvent event
    ) {

        Player player = event.getPlayer();

        if (!plugin.isOwner(player)) {
            return;
        }

        if (!plugin.isSoulspade(
                player.getInventory()
                        .getItemInMainHand()
        )) {
            return;
        }

        int slot = event.getNewSlot();

        if (slot == 0) {

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

        } else if (slot == 1) {

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

        } else if (slot == 2) {

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
    public void onRightClick(
            PlayerInteractEvent event
    ) {

        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Action action =
                event.getAction();

        if (action != Action.RIGHT_CLICK_AIR &&
                action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player =
                event.getPlayer();

        if (!plugin.isSoulspade(
                player.getInventory()
                        .getItemInMainHand()
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

        event.setCancelled(true);

        int skill =
                plugin.getSelectedSkill(
                        player.getUniqueId()
                );

        switch (skill) {

            case 1 -> useDash(player);

            case 2 -> useEnergyBlast(player);

            case 3 -> useExplosiveSnowball(player);
        }
    }

    // =========================================================
    // PUNISHMENT OF THE PROUD STYLE DASH
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

        UUID uuid =
                player.getUniqueId();

        long cooldown =
                plugin.getRemainingDashCooldown(uuid);

        if (cooldown > 0) {

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
                player.getLocation()
                        .getDirection()
                        .normalize();

        // Don't allow the dash to force the player downward.
        if (direction.getY() < -0.25) {
            direction.setY(-0.25);
            direction.normalize();
        }

        Location destination =
                start.clone();

        LivingEntity targetHit = null;

        // =====================================================
        // FIND TARGET FIRST
        // =====================================================

        for (
                double distance = 0.25;
                distance <= range;
                distance += 0.15
        ) {

            Location check =
                    start.clone().add(
                            direction.clone()
                                    .multiply(distance)
                    );

            Block block =
                    check.getBlock();

            if (!block.isPassable()) {
                break;
            }

            destination =
                    check.clone();

            // Large combat hitbox.
            for (
                    Entity entity :
                    check.getWorld()
                            .getNearbyEntities(
                                    check,
                                    1.35,
                                    1.35,
                                    1.35
                            )
            ) {

                if (!(entity instanceof LivingEntity target)) {
                    continue;
                }

                if (target.equals(player)) {
                    continue;
                }

                if (target.isDead()) {
                    continue;
                }

                targetHit = target;
                break;
            }

            if (targetHit != null) {
                break;
            }
        }

        // =====================================================
        // TELEPORT
        // =====================================================

        destination.setYaw(
                start.getYaw()
        );

        destination.setPitch(
                start.getPitch()
        );

        player.teleport(destination);

        // =====================================================
        // DASH EFFECT
        // =====================================================

        World world =
                player.getWorld();

        world.playSound(
                player.getLocation(),
                Sound.ENTITY_ENDERMAN_TELEPORT,
                1.0f,
                1.5f
        );

        world.spawnParticle(
                Particle.SOUL,
                start.clone().add(0, 1, 0),
                20,
                0.4,
                0.7,
                0.4,
                0.03
        );

        world.spawnParticle(
                Particle.SOUL_FIRE_FLAME,
                destination.clone().add(0, 1, 0),
                30,
                0.5,
                0.8,
                0.5,
                0.04
        );

        // =====================================================
        // HIT TARGET
        // =====================================================

        if (targetHit != null) {

            double before =
                    targetHit.getHealth();

            targetHit.damage(
                    damage,
                    player
            );

            double after =
                    Math.max(
                            0,
                            targetHit.getHealth()
                    );

            double actualDamage =
                    Math.max(
                            0,
                            before - after
                    );

            // Lifesteal is handled here too,
            // so Dash always counts toward lifesteal.
            if (actualDamage > 0) {

                double percent =
                        plugin.getConfig().getDouble(
                                "combat.lifesteal-percent",
                                20.0
                        );

                healPlayer(
                        player,
                        actualDamage * percent / 100.0
                );
            }

            applyDashEffects(
                    targetHit
            );

            Location hitLocation =
                    targetHit.getLocation()
                            .add(0, 1, 0);

            world.spawnParticle(
                    Particle.SOUL_FIRE_FLAME,
                    hitLocation,
                    35,
                    0.5,
                    0.8,
                    0.5,
                    0.04
            );

            world.spawnParticle(
                    Particle.CRIT,
                    hitLocation,
                    20,
                    0.4,
                    0.6,
                    0.4,
                    0.2
            );

            world.playSound(
                    hitLocation,
                    Sound.ENTITY_PLAYER_ATTACK_STRONG,
                    1.0f,
                    0.7f
            );

            player.sendActionBar(
                    "§b⚔ DASH HIT §7• §c"
                            + damage
                            + " Damage"
            );

        } else {

            player.sendActionBar(
                    "§b⚔ DASH"
            );
        }

        plugin.startDashCooldown(uuid);
    }

    // =========================================================
    // DASH LIFESTEAL
    // =========================================================

    private void healPlayer(
            Player player,
            double amount
    ) {

        if (amount <= 0) {
            return;
        }

        var maxHealth =
                player.getAttribute(
                        org.bukkit.attribute.Attribute.MAX_HEALTH
                );

        if (maxHealth == null) {
            return;
        }

        double max =
                maxHealth.getValue();

        player.setHealth(
                Math.min(
                        player.getHealth() + amount,
                        max
                )
        );

        if (plugin.getConfig().getBoolean(
                "combat.lifesteal-particles",
                true
        )) {

            player.getWorld().spawnParticle(
                    Particle.HEART,
                    player.getLocation()
                            .add(0, 2, 0),
                    3,
                    0.25,
                    0.25,
                    0.25,
                    0.02
            );
        }
    }

    // =========================================================
    // DASH EFFECTS
    // =========================================================

    private void applyDashEffects(
            LivingEntity target
    ) {

        if (plugin.getConfig().getBoolean(
                "dash.effects.slowness.enabled",
                true
        )) {

            target.addPotionEffect(
                    new PotionEffect(
                            PotionEffectType.SLOWNESS,
                            plugin.getConfig().getInt(
                                    "dash.effects.slowness.duration",
                                    5
                            ) * 20,
                            plugin.getConfig().getInt(
                                    "dash.effects.slowness.amplifier",
                                    1
                            )
                    )
            );
        }

        if (plugin.getConfig().getBoolean(
                "dash.effects.weakness.enabled",
                true
        )) {

            target.addPotionEffect(
                    new PotionEffect(
                            PotionEffectType.WEAKNESS,
                            plugin.getConfig().getInt(
                                    "dash.effects.weakness.duration",
                                    5
                            ) * 20,
                            plugin.getConfig().getInt(
                                    "dash.effects.weakness.amplifier",
                                    1
                            )
                    )
            );
        }
    }

    // =========================================================
    // ENERGY BLAST
    // =========================================================

    private void useEnergyBlast(
            Player player
    ) {

        if (!plugin.getConfig().getBoolean(
                "energy-blast.enabled",
                true
        )) {
            return;
        }

        UUID uuid =
                player.getUniqueId();

        if (plugin.getRemainingEnergyBlastCooldown(
                uuid
        ) > 0) {

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

        double radius =
                plugin.getConfig().getDouble(
                        "energy-blast.hit-radius",
                        1.5
                );

        Location start =
                player.getEyeLocation();

        Vector direction =
                start.getDirection()
                        .normalize();

        Set<UUID> hit =
                new HashSet<>();

        Location end =
                start.clone();

        for (
                double distance = 0;
                distance <= range;
                distance += 0.20
        ) {

            Location point =
                    start.clone().add(
                            direction.clone()
                                    .multiply(distance)
                    );

            if (!point.getBlock().isPassable()) {
                break;
            }

            end = point;

            spawnBlastParticles(point);

            for (
                    Entity entity :
                    point.getWorld()
                            .getNearbyEntities(
                                    point,
                                    radius,
                                    radius,
                                    radius
                            )
            ) {

                if (!(entity instanceof LivingEntity target)) {
                    continue;
                }

                if (target.equals(player)) {
                    continue;
                }

                if (!hit.add(
                        target.getUniqueId()
                )) {
                    continue;
                }

                target.damage(
                        damage,
                        player
                );

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

        end.getWorld().spawnParticle(
                Particle.END_ROD,
                end,
                35,
                0.7,
                0.7,
                0.7,
                0.08
        );

        end.getWorld().spawnParticle(
                Particle.SOUL_FIRE_FLAME,
                end,
                25,
                0.5,
                0.5,
                0.5,
                0.04
        );

        end.getWorld().playSound(
                end,
                Sound.ENTITY_PLAYER_ATTACK_STRONG,
                1.0f,
                0.7f
        );

        plugin.startEnergyBlastCooldown(uuid);
    }

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
    // EXPLOSIVE SNOWBALL
    // =========================================================

    private void useExplosiveSnowball(
            Player player
    ) {

        if (!plugin.getConfig().getBoolean(
                "explosive-snowball.enabled",
                true
        )) {
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
    // MESSAGE
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