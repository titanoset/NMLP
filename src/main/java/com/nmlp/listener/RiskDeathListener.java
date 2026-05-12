package com.nmlp.listener;

import com.nmlp.config.ReloadManager;
import com.nmlp.service.RiskActionService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Clears cumulative max-health risk modifier on death (if configured).
 */
public final class RiskDeathListener implements Listener {

    private final ReloadManager reload;
    private final RiskActionService risk;

    public RiskDeathListener(@NotNull ReloadManager reload, @NotNull RiskActionService risk) {
        this.reload = reload;
        this.risk = risk;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(@NotNull PlayerDeathEvent e) {
        if (reload.buffs().resetRiskHealthOnDeath()) {
            risk.clearRiskHealthModifier(e.getEntity());
        }
        risk.clearWindows(e.getEntity().getUniqueId());
    }
}
