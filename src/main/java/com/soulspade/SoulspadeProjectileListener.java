package com.soulspade;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;

public class SoulspadeProjectileListener implements Listener {

    private final Soulspade plugin;

    public SoulspadeProjectileListener(Soulspade plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {

        if (!(event.getEntity() instanceof Snowball snowball)) {
            return;
        }

        if (!"Soulspade Explosive Snowball".equals(
                snowball.getCustomName())) {
            return;
        }

        Location location = snowball.getLocation();

        Player shooter = null;

        if (snowball.getShooter() instanceof Player player) {
            shooter = player;
        }

        // Visual explosion
        location.getWorld().spawnParticle(
                Particle.EXPLOSION,
                location,
                3,
                0.3,
                0.3,
                0.3,
                0.05
        );

        location.getWorld().spawnParticle(
                Particle.SOUL_FIRE_FLAME,
                location,
                35,
                1.5,
                1.0,
                1.5,
                0.05
        );

        location.getWorld().spawnParticle(
                Particle.END_ROD,
                location,
                20,
                1.2,
                0.8,
                1.2,
                0.03
        );

        location.getWorld().playSound(
                location,
                Sound.ENTITY_GENERIC_EXPLODE,
                1.0f,
                1.2f
        );

        // Damage nearby mobs and players
        for (Entity entity : location.getWorld().getNearbyEntities(
                location,
                3.0,
                3.0,
                3.0
        )) {

            if (!(entity instanceof LivingEntity target)) {
                continue;
            }

            if (shooter != null && target.equals(shooter)) {
                continue;
            }

            // 3 damage
            if (shooter != null) {
                target.damage(3.0, shooter);
            } else {
                target.damage(3.0);
            }
        }

        /*
         * IMPORTANT:
         *
         * We intentionally DO NOT call createExplosion().
         *
         * This means the ability damages entities and creates
         * explosion particles, but blocks remain untouched.
         */

        snowball.remove();
    }
}
