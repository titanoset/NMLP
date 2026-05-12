package com.nmlp.service;

import com.nmlp.config.MainConfig;
import com.nmlp.config.MessageService;
import com.nmlp.config.ReloadManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hug/kiss toward partner or, with {@code nmlp.affection.friend}, a named nearby player.
 */
public final class AffectionService {

    private final ReloadManager reload;
    private final MessageService messages;
    private final ProfileCache cache;
    private final Map<String, Long> cooldownUntil = new ConcurrentHashMap<>();

    public AffectionService(
            @NotNull ReloadManager reload,
            @NotNull MessageService messages,
            @NotNull ProfileCache cache
    ) {
        this.reload = reload;
        this.messages = messages;
        this.cache = cache;
    }

    public void tryEmote(
            @NotNull Player actor,
            @NotNull AffectionKind kind,
            @NotNull String[] args,
            @NotNull String commandLabel
    ) {
        MainConfig cfg = reload.main();
        if (!cfg.affectionEnabled()) {
            messages.send(actor, "affection.disabled", "<gray>Disabled.</gray>");
            return;
        }
        long now = System.currentTimeMillis();
        String cdKey = actor.getUniqueId() + ":" + kind.name();
        Long until = cooldownUntil.get(cdKey);
        if (until != null && now < until) {
            messages.send(actor, "affection.cooldown", "<red>Wait.</red>");
            return;
        }
        ProfileSnapshot snap = cache.getCachedOrEmpty(actor.getUniqueId(), actor.getName());
        UUID partnerUuid = snap.partnerUuid();
        Player target;
        if (args.length == 0) {
            if (partnerUuid == null) {
                messages.send(actor, "affection.need-partner-or-name", "<gray>No partner.</gray>",
                        java.util.Map.of("command", "/" + commandLabel));
                return;
            }
            target = Bukkit.getPlayer(partnerUuid);
            if (target == null || !target.isOnline()) {
                messages.send(actor, "affection.partner-offline", "<gray>Partner is not online.</gray>");
                return;
            }
        } else {
            target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                messages.send(actor, "errors.not-found", "<red>Not found.</red>");
                return;
            }
            boolean isPartner = partnerUuid != null && partnerUuid.equals(target.getUniqueId());
            if (!isPartner && !actor.hasPermission("nmlp.affection.friend")) {
                messages.send(actor, "affection.partner-only", "<red>Only your partner, unless you have permission.</red>");
                return;
            }
        }
        if (target.getUniqueId().equals(actor.getUniqueId())) {
            messages.send(actor, "affection.cannot-self", "<red>Cannot target yourself.</red>");
            return;
        }
        if (!actor.getWorld().equals(target.getWorld())) {
            messages.send(actor, "affection.different-world", "<red>Same world only.</red>");
            return;
        }
        double max = cfg.affectionMaxDistance();
        if (actor.getLocation().distanceSquared(target.getLocation()) > max * max) {
            messages.send(actor, "affection.too-far", "<red>Too far.</red>");
            return;
        }
        cooldownUntil.put(cdKey, now + cfg.affectionCooldownMs());
        playParticles(actor, target, kind, cfg);
        String base = "affection." + kind.messageKey() + ".";
        java.util.Map<String, String> map = java.util.Map.of(
                "actor", actor.getName(),
                "target", target.getName()
        );
        messages.send(actor, base + "sender", "<green>Ok</green>", map);
        messages.send(target, base + "target", "<green>Ok</green>", map);
        broadcastNearby(actor, target, base + "nearby", map);
    }

    private void broadcastNearby(@NotNull Player a, @NotNull Player b, @NotNull String path, @NotNull java.util.Map<String, String> map) {
        Location mid = midpoint(a.getLocation(), b.getLocation());
        double r2 = 20 * 20;
        for (Player p : a.getWorld().getPlayers()) {
            if (p.equals(a) || p.equals(b)) {
                continue;
            }
            if (p.getLocation().distanceSquared(mid) <= r2) {
                messages.send(p, path, "<gray>...</gray>", map);
            }
        }
    }

    private static @NotNull Location midpoint(@NotNull Location a, @NotNull Location b) {
        return new Location(a.getWorld(), (a.getX() + b.getX()) / 2, (a.getY() + b.getY()) / 2 + 1, (a.getZ() + b.getZ()) / 2);
    }

    private void playParticles(@NotNull Player a, @NotNull Player b, @NotNull AffectionKind kind, @NotNull MainConfig cfg) {
        Location mid = midpoint(a.getLocation(), b.getLocation());
        boolean on = kind == AffectionKind.HUG ? cfg.hugParticles() : cfg.kissParticles();
        int count = kind == AffectionKind.HUG ? cfg.hugParticleCount() : cfg.kissParticleCount();
        if (!on) {
            return;
        }
        Particle particle = kind == AffectionKind.KISS ? Particle.HEART : Particle.HAPPY_VILLAGER;
        a.getWorld().spawnParticle(particle, mid, count, 0.35, 0.25, 0.35, 0.01);
    }
}
