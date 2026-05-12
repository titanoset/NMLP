package com.nmlp.service;

import com.nmlp.config.MainConfig;
import org.bukkit.FireworkEffect;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Marriage celebration effects (main thread).
 */
public final class EffectService {

    private final MainConfig cfg;

    public EffectService(@NotNull MainConfig cfg) {
        this.cfg = cfg;
    }

    public void playMarriageCelebration(@NotNull Player a, @Nullable Player b) {
        Location loc = a.getLocation().add(0, 1, 0);
        if (cfg.marriageHearts()) {
            a.getWorld().spawnParticle(Particle.HEART, loc, cfg.marriageParticleCount(), 0.5, 0.5, 0.5, 0.02);
            if (b != null && b.isOnline()) {
                b.getWorld().spawnParticle(Particle.HEART, b.getLocation().add(0, 1, 0), cfg.marriageParticleCount(), 0.5, 0.5, 0.5, 0.02);
            }
        }
        NamespacedKey key = parseSoundKey(cfg.marriageSound());
        if (key != null) {
            Sound sound = Registry.SOUNDS.get(key);
            if (sound != null) {
                a.playSound(a.getLocation(), sound, cfg.marriageSoundVolume(), cfg.marriageSoundPitch());
                if (b != null && b.isOnline()) {
                    b.playSound(b.getLocation(), sound, cfg.marriageSoundVolume(), cfg.marriageSoundPitch());
                }
            }
        }
        if (cfg.marriageFireworks()) {
            spawnFirework(loc);
            if (b != null && b.isOnline()) {
                spawnFirework(b.getLocation().add(0, 1, 0));
            }
        }
    }

    private static @Nullable NamespacedKey parseSoundKey(@NotNull String raw) {
        if (raw.contains(":")) {
            return NamespacedKey.fromString(raw);
        }
        String lower = raw.toLowerCase(java.util.Locale.ROOT);
        String dotted = lower.replace('_', '.');
        return NamespacedKey.minecraft(dotted);
    }

    private static void spawnFirework(@NotNull Location loc) {
        Firework fw = loc.getWorld().spawn(loc, Firework.class);
        FireworkMeta meta = fw.getFireworkMeta();
        FireworkEffect.Builder b = FireworkEffect.builder();
        b.with(FireworkEffect.Type.BALL_LARGE);
        b.flicker(true);
        b.trail(true);
        int ri = ThreadLocalRandom.current().nextInt(3);
        b.withColor(org.bukkit.Color.fromRGB(255, 100 + ri * 20, 180 + ri * 10));
        meta.addEffect(b.build());
        meta.setPower(1);
        fw.setFireworkMeta(meta);
    }
}
