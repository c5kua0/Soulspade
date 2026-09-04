package com.soulspade;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Soulspade extends JavaPlugin {

    private NamespacedKey soulspadeKey;
    private NamespacedKey attackDamageKey;

    private final Map<UUID, Integer> selectedSkills = new HashMap<>();
    private final Map<UUID, Long> dashCooldown = new HashMap<>();
    private final Map<UUID, Long> energyBlastCooldown = new HashMap<>();

    @Override
    public void onEnable() {

        saveDefaultConfig();
        reloadConfig();

        soulspadeKey = new NamespacedKey(
                this,
                "soulspade"
        );

        attackDamageKey = new NamespacedKey(
                this,
                "soulspade_attack_damage"
        );

        registerSoulspadeCommands();

        getServer().getPluginManager().registerEvents(
                new SoulspadeListener(this),
                this
        );

        getServer().getPluginManager().registerEvents(
                new SoulspadeProjectileListener(this),
                this
        );

        getServer().getPluginManager().registerEvents(
                new SoulspadeProtectionListener(this),
                this
        );

        new SoulspadeCooldownTask(this)
                .runTaskTimer(
                        this,
                        0L,
                        2L
                );

        getLogger().info(
                "Soulspade has been enabled!"
        );
    }

    @Override
    public void onDisable() {

        selectedSkills.clear();
        dashCooldown.clear();
        energyBlastCooldown.clear();

        getLogger().info(
                "Soulspade has been disabled!"
        );
    }

    // =========================================================
    // COMMANDS
    // =========================================================

    private void registerSoulspadeCommands() {

        BasicCommand command = this::executeSoulspadeCommand;

        registerCommand(
                "soulspade",
                command
        );

        registerCommand(
                "ss",
                command
        );

        getLogger().info(
                "Registered commands: /soulspade and /ss"
        );
    }

    private void executeSoulspadeCommand(
            CommandSourceStack source,
            String[] args
    ) {

        if (!(source.getSender() instanceof Player player)) {

            source.getSender().sendPlainMessage(
                    "Only players can use this command."
            );

            return;
        }

        if (!isOwner(player)) {

            player.sendMessage(
                    color(
                            getConfig().getString(
                                    "messages.owner-only",
                                    "&cOnly the Soulspade owner can use this weapon."
                            )
                    )
            );

            return;
        }

        player.getInventory().addItem(
                createSoulspade()
        );

        player.sendMessage(
                color(
                        getConfig().getString(
                                "messages.received",
                                "&bYou received the &3&lSoulspade&b!"
                        )
                )
        );
    }

    // =========================================================
    // OWNER
    // =========================================================

    public boolean isOwner(Player player) {

        if (!getConfig().getBoolean(
                "owner.enabled",
                true
        )) {
            return true;
        }

        String ownerName = getConfig().getString(
                "owner.name",
                ""
        );

        if (ownerName == null || ownerName.isBlank()) {
            return false;
        }

        return player.getName()
                .equalsIgnoreCase(ownerName);
    }

    // =========================================================
    // SOULSPADE ITEM
    // =========================================================

    public ItemStack createSoulspade() {

        ItemStack item = new ItemStack(
                Material.NETHERITE_SHOVEL
        );

        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return item;
        }

        String configuredName = getConfig().getString(
                "weapon.name",
                "&3&lSoulspade"
        );

        meta.setDisplayName(
                color(configuredName)
        );

        meta.setLore(Arrays.asList(
                color("&7A weapon forged with soul energy."),
                "",
                color("&bSkills:"),
                color("&f1. &9Dash"),
                color("&f2. &bEnergy Blast"),
                color("&f3. &fExplosive Snowball"),
                "",
                color("&d✦ 20% Lifesteal"),
                color("&c✦ 20 Attack Damage"),
                "",
                color("&8Select a skill using your hotbar."),
                color("&8Right-click to cast.")
        ));

        // =====================================================
        // SOULSPADE IDENTIFIER
        // =====================================================

        meta.getPersistentDataContainer().set(
                soulspadeKey,
                PersistentDataType.BYTE,
                (byte) 1
        );

        // =====================================================
        // 20 TOTAL PHYSICAL ATTACK DAMAGE
        //
        // Netherite Shovel normally has 5 attack damage.
        // +15 modifier = 20 total damage.
        // =====================================================

        meta.addAttributeModifier(
                Attribute.ATTACK_DAMAGE,
                new AttributeModifier(
                        attackDamageKey,
                        15.0,
                        AttributeModifier.Operation.ADD_NUMBER,
                        EquipmentSlotGroup.MAINHAND
                )
        );

        item.setItemMeta(meta);

        return item;
    }

    public boolean isSoulspade(ItemStack item) {

        if (item == null) {
            return false;
        }

        if (item.getType() != Material.NETHERITE_SHOVEL) {
            return false;
        }

        if (!item.hasItemMeta()) {
            return false;
        }

        return item.getItemMeta()
                .getPersistentDataContainer()
                .has(
                        soulspadeKey,
                        PersistentDataType.BYTE
                );
    }

    // =========================================================
    // SKILL SELECTION
    // =========================================================

    public int getSelectedSkill(UUID uuid) {

        return selectedSkills.getOrDefault(
                uuid,
                1
        );
    }

    public void setSelectedSkill(
            UUID uuid,
            int skill
    ) {

        if (skill < 1 || skill > 3) {
            return;
        }

        selectedSkills.put(
                uuid,
                skill
        );
    }

    // =========================================================
    // DASH COOLDOWN
    // =========================================================

    public void startDashCooldown(UUID uuid) {

        dashCooldown.put(
                uuid,
                System.currentTimeMillis()
        );
    }

    public long getRemainingDashCooldown(UUID uuid) {

        double cooldown = getConfig().getDouble(
                "dash.cooldown",
                3.0
        );

        return getRemainingTime(
                dashCooldown,
                uuid,
                cooldown
        );
    }

    // =========================================================
    // ENERGY BLAST COOLDOWN
    // =========================================================

    public void startEnergyBlastCooldown(UUID uuid) {

        energyBlastCooldown.put(
                uuid,
                System.currentTimeMillis()
        );
    }

    public long getRemainingEnergyBlastCooldown(
            UUID uuid
    ) {

        double cooldown = getConfig().getDouble(
                "energy-blast.cooldown",
                5.0
        );

        return getRemainingTime(
                energyBlastCooldown,
                uuid,
                cooldown
        );
    }

    // =========================================================
    // COOLDOWN CALCULATOR
    // =========================================================

    private long getRemainingTime(
            Map<UUID, Long> cooldowns,
            UUID uuid,
            double cooldownSeconds
    ) {

        if (!cooldowns.containsKey(uuid)) {
            return 0;
        }

        long elapsed =
                System.currentTimeMillis()
                        - cooldowns.get(uuid);

        long cooldownMillis =
                (long) (cooldownSeconds * 1000);

        long remaining =
                cooldownMillis - elapsed;

        return Math.max(
                remaining,
                0
        );
    }

    public long getRemainingCooldown(
            UUID uuid,
            String skill
    ) {

        if (skill.equalsIgnoreCase("dash")) {

            return getRemainingDashCooldown(uuid);
        }

        if (skill.equalsIgnoreCase("energy-blast")) {

            return getRemainingEnergyBlastCooldown(uuid);
        }

        return 0;
    }

    // =========================================================
    // UTILITIES
    // =========================================================

    private String color(String text) {

        return ChatColor.translateAlternateColorCodes(
                '&',
                text
        );
    }

    public NamespacedKey getSoulspadeKey() {

        return soulspadeKey;
    }
}