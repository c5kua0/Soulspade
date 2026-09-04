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

        // ==========================================
        // CONFIG
        // ==========================================

        double damage =
                plugin.getConfig().getDouble(
                        "explosive-snowball.damage",
                        3.0
                );

        double radius =
                plugin.getConfig().getDouble(
                        "explosive-snowball.radius",
                        3.0
                );

        boolean breakBlocks =
                plugin.getConfig().getBoolean(
                        "explosive-snowball.break-blocks",
                        false
                );

        // ==========================================
        // PARTICLES
        // ==========================================

        if (plugin.getConfig().getBoolean(
                "explosive-snowball.particles.enabled",
                true
        )) {

            spawnParticle(
                    location,
                    plugin.getConfig().getString(
                            "explosive-snowball.particles.explosion",
                            "EXPLOSION"
                    ),
                    plugin.getConfig().getInt(
                            "explosive-snowball.particles.explosion-amount",
                            3
                    )
            );

            spawnParticle(
                    location,
                    plugin.getConfig().getString(
                            "explosive-snowball.particles.primary",
                            "SOUL_FIRE_FLAME"
                    ),
                    plugin.getConfig().getInt(
                            "explosive-snowball.particles.primary-amount",
                            35
                    )
            );

            spawnParticle(
                    location,
                    plugin.getConfig().getString(
                            "explosive-snowball.particles.secondary",
                            "END_ROD"
                    ),
                    plugin.getConfig().getInt(
                            "explosive-snowball.particles.secondary-amount",
                            20
                    )
            );
        }

        // ==========================================
        // SOUND
        // ==========================================

        location.getWorld().playSound(
                location,
                Sound.ENTITY_GENERIC_EXPLODE,
                1.0f,
                1.2f
        );

        // ==========================================
        // DAMAGE
        // ==========================================

        for (Entity entity :
                location.getWorld().getNearbyEntities(
                        location,
                        radius,
                        radius,
                        radius
                )) {

            if (!(entity instanceof LivingEntity target)) {
                continue;
            }

            // Don't damage shooter
            if (shooter != null &&
                    target.equals(shooter)) {
                continue;
            }

            if (damage > 0) {

                if (shooter != null) {
                    target.damage(
                            damage,
                            shooter
                    );
                } else {
                    target.damage(damage);
                }
            }

            // Soul explosion effect
            target.getWorld().spawnParticle(
                    Particle.SOUL,
                    target.getLocation()
                            .add(0, 1, 0),
                    12,
                    0.4,
                    0.6,
                    0.4,
                    0.03
            );
        }

        // ==========================================
        // OPTIONAL REAL EXPLOSION
        // ==========================================
        //
        // Default is false.
        // Therefore blocks are protected.
        //

        if (breakBlocks) {

            location.getWorld().createExplosion(
                    location.getX(),
                    location.getY(),
                    location.getZ(),
                    (float) Math.min(radius, 6.0),
                    false,
                    true
            );
        }

        snowball.remove();
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
                    0.5,
                    0.5,
                    0.5,
                    0.05
            );

        } catch (IllegalArgumentException ignored) {

            plugin.getLogger().warning(
                    "Invalid particle in config: "
                            + particleName
            );
        }
    }
}