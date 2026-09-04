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

        for (Player player :
                plugin.getServer().getOnlinePlayers()) {

            /*
             * Only show the Soulspade UI while
             * holding the Soulspade.
             */
            ItemStack item =
                    player.getInventory()
                            .getItemInMainHand();

            if (!plugin.isSoulspade(item)) {
                continue;
            }

            /*
             * Only the configured owner gets
             * Soulspade skill information.
             */
            if (!plugin.isOwner(player)) {
                continue;
            }

            UUID uuid =
                    player.getUniqueId();

            int skill =
                    plugin.getSelectedSkill(uuid);

            /*
             * ==========================================
             * DASH
             * ==========================================
             */

            if (skill == 1) {

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

            /*
             * ==========================================
             * ENERGY BLAST
             * ==========================================
             */

            } else if (skill == 2) {

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

            /*
             * ==========================================
             * EXPLOSIVE SNOWBALL
             * ==========================================
             */

            } else if (skill == 3) {

                player.sendActionBar(
                        color(
                                "&f❄ Explosive Snowball &8— &aREADY"
                        )
                );
            }
        }
    }

    /*
     * ==========================================
     * FORMAT TIME
     * ==========================================
     */

    private String format(double number) {

        return String.format(
                java.util.Locale.US,
                "%.1f",
                number
        );
    }

    /*
     * ==========================================
     * COLOR
     * ==========================================
     */

    private String color(String text) {

        return ChatColor.translateAlternateColorCodes(
                '&',
                text
        );
    }
}