package com.soulspade;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public class SoulspadeCooldownTask extends BukkitRunnable {

    private final Soulspade plugin;

    public SoulspadeCooldownTask(Soulspade plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {

        for (Player player : plugin.getServer().getOnlinePlayers()) {

            // Only show Soulspade information while holding it
            ItemStack item =
                    player.getInventory().getItemInMainHand();

            if (!plugin.isSoulspade(item)) {
                continue;
            }

            UUID uuid = player.getUniqueId();

            int skill =
                    plugin.getSelectedSkill(uuid);

            if (skill == 1) {

                double cooldown =
                        plugin.getConfig().getDouble(
                                "dash.cooldown",
                                3.0
                        );

                long remaining =
                        plugin.getRemainingCooldown(
                                uuid,
                                "dash"
                        );

                if (remaining > 0) {

                    double seconds =
                            remaining / 1000.0;

                    player.sendActionBar(
                            color(
                                    "&b⚔ Dash &8— &c"
                                            + format(seconds)
                                            + "s"
                            )
                    );

                } else {

                    player.sendActionBar(
                            color(
                                    "&b⚔ Dash &8— &aREADY"
                            )
                    );
                }

            } else if (skill == 2) {

                double cooldown =
                        plugin.getConfig().getDouble(
                                "energy-blast.cooldown",
                                5.0
                        );

                long remaining =
                        plugin.getRemainingCooldown(
                                uuid,
                                "energy-blast"
                        );

                if (remaining > 0) {

                    double seconds =
                            remaining / 1000.0;

                    player.sendActionBar(
                            color(
                                    "&b⚡ Energy Blast &8— &c"
                                            + format(seconds)
                                            + "s"
                            )
                    );

                } else {

                    player.sendActionBar(
                            color(
                                    "&b⚡ Energy Blast &8— &aREADY"
                            )
                    );
                }

            } else if (skill == 3) {

                player.sendActionBar(
                        color(
                                "&f❄ Explosive Snowball &8— &aREADY"
                        )
                );
            }
        }
    }

    private String format(double number) {

        return String.format(
                java.util.Locale.US,
                "%.1f",
                number
        );
    }

    private String color(String text) {

        return ChatColor.translateAlternateColorCodes(
                '&',
                text
        );
    }
}