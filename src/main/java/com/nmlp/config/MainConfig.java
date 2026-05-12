package com.nmlp.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

/**
 * Typed view of {@code config.yml}.
 */
public final class MainConfig {

    private final String databaseFile;
    private final int poolSize;
    private final long maxLifetimeMs;
    private final int asyncPoolThreads;
    private final long guiCooldownMs;
    private final long proposeCooldownMs;
    private final long ringGrantCooldownMs;
    private final boolean economyEnabled;
    private final double engageCost;
    private final double marryCost;
    private final double ringCost;
    private final String ringMaterial;
    private final int ringCustomModelData;
    private final int ringPurchaseDiamonds;
    private final double proposalRadius;
    private final boolean marriageFireworks;
    private final boolean marriageHearts;
    private final int marriageParticleCount;
    private final String marriageSound;
    private final float marriageSoundVolume;
    private final float marriageSoundPitch;
    private final boolean tabHeartPrefix;
    private final String tabHeartSymbol;
    private boolean debug;
    private final boolean affectionEnabled;
    private final double affectionMaxDistance;
    private final long affectionCooldownMs;
    private final boolean hugParticles;
    private final int hugParticleCount;
    private final boolean kissParticles;
    private final int kissParticleCount;

    public MainConfig(@NotNull FileConfiguration c) {
        this.databaseFile = c.getString("database.file", "plugins/NMLP/data.db");
        this.poolSize = c.getInt("database.pool-size", 4);
        this.maxLifetimeMs = c.getLong("database.max-lifetime-ms", 1_800_000L);
        this.asyncPoolThreads = c.getInt("async.pool-threads", 4);
        this.guiCooldownMs = c.getLong("cooldowns.gui-open-ms", 500L);
        this.proposeCooldownMs = c.getLong("cooldowns.propose-ms", 30_000L);
        this.ringGrantCooldownMs = c.getLong("cooldowns.ring-grant-ms", 0L);
        this.economyEnabled = c.getBoolean("economy.enabled", false);
        this.engageCost = c.getDouble("economy.engage-cost", 0);
        this.marryCost = c.getDouble("economy.marry-cost", 0);
        this.ringCost = c.getDouble("economy.ring-cost", 0);
        this.ringMaterial = c.getString("ring.material", "CLOCK");
        this.ringCustomModelData = c.getInt("ring.custom-model-data", 0);
        this.ringPurchaseDiamonds = Math.max(0, c.getInt("ring.purchase-diamonds", 20));
        this.proposalRadius = c.getDouble("ring.proposal-radius", 5.0);
        this.marriageFireworks = c.getBoolean("effects.marriage.fireworks", true);
        this.marriageHearts = c.getBoolean("effects.marriage.hearts-particles", true);
        this.marriageParticleCount = c.getInt("effects.marriage.particle-count", 24);
        this.marriageSound = c.getString("effects.marriage.sound", "ENTITY_PLAYER_LEVELUP");
        this.marriageSoundVolume = (float) c.getDouble("effects.marriage.sound-volume", 1.0);
        this.marriageSoundPitch = (float) c.getDouble("effects.marriage.sound-pitch", 1.2);
        this.tabHeartPrefix = c.getBoolean("tab.heart-prefix-enabled", false);
        this.tabHeartSymbol = c.getString("tab.heart-symbol", "♥ ");
        this.debug = c.getBoolean("debug", false);
        this.affectionEnabled = c.getBoolean("affection.enabled", true);
        this.affectionMaxDistance = c.getDouble("affection.max-distance", 4.0);
        this.affectionCooldownMs = c.getLong("affection.cooldown-ms", 3000L);
        this.hugParticles = c.getBoolean("affection.hug.particles", true);
        this.hugParticleCount = c.getInt("affection.hug.particle-count", 10);
        this.kissParticles = c.getBoolean("affection.kiss.particles", true);
        this.kissParticleCount = c.getInt("affection.kiss.particle-count", 14);
    }

    public @NotNull String databaseFile() {
        return databaseFile;
    }

    public int poolSize() {
        return poolSize;
    }

    public long maxLifetimeMs() {
        return maxLifetimeMs;
    }

    public int asyncPoolThreads() {
        return asyncPoolThreads;
    }

    public long guiCooldownMs() {
        return guiCooldownMs;
    }

    public long proposeCooldownMs() {
        return proposeCooldownMs;
    }

    /**
     * Minimum delay between {@code /engage ring} per player; {@code 0} = no limit.
     */
    public long ringGrantCooldownMs() {
        return ringGrantCooldownMs;
    }

    public boolean economyEnabled() {
        return economyEnabled;
    }

    public double engageCost() {
        return engageCost;
    }

    public double marryCost() {
        return marryCost;
    }

    public double ringCost() {
        return ringCost;
    }

    public @NotNull String ringMaterial() {
        return ringMaterial;
    }

    public int ringCustomModelData() {
        return ringCustomModelData;
    }

    /**
     * Diamonds consumed by {@code /engage ring}; {@code 0} = no item cost.
     */
    public int ringPurchaseDiamonds() {
        return ringPurchaseDiamonds;
    }

    public double proposalRadius() {
        return proposalRadius;
    }

    public boolean marriageFireworks() {
        return marriageFireworks;
    }

    public boolean marriageHearts() {
        return marriageHearts;
    }

    public int marriageParticleCount() {
        return marriageParticleCount;
    }

    public @NotNull String marriageSound() {
        return marriageSound;
    }

    public float marriageSoundVolume() {
        return marriageSoundVolume;
    }

    public float marriageSoundPitch() {
        return marriageSoundPitch;
    }

    public boolean tabHeartPrefix() {
        return tabHeartPrefix;
    }

    public @NotNull String tabHeartSymbol() {
        return tabHeartSymbol;
    }

    public boolean debug() {
        return debug;
    }

    public void debug(boolean v) {
        this.debug = v;
    }

    public boolean affectionEnabled() {
        return affectionEnabled;
    }

    public double affectionMaxDistance() {
        return affectionMaxDistance;
    }

    public long affectionCooldownMs() {
        return affectionCooldownMs;
    }

    public boolean hugParticles() {
        return hugParticles;
    }

    public int hugParticleCount() {
        return hugParticleCount;
    }

    public boolean kissParticles() {
        return kissParticles;
    }

    public int kissParticleCount() {
        return kissParticleCount;
    }
}
