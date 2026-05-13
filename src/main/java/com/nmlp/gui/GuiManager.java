package com.nmlp.gui;

import com.nmlp.config.GuiConfig;
import com.nmlp.config.MainConfig;
import com.nmlp.config.ReloadManager;
import com.nmlp.model.FamilyLinkRow;
import com.nmlp.model.RelationshipRow;
import com.nmlp.repository.FamilyRepository;
import com.nmlp.repository.RelationshipRepository;
import com.nmlp.service.ProfileCache;
import com.nmlp.service.ProfileSnapshot;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Opens chest-style GUIs (main thread after async data load).
 */
public final class GuiManager {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final JavaPlugin plugin;
    private final ReloadManager reload;
    private final ProfileCache cache;
    private final RelationshipRepository relationships;
    private final FamilyRepository family;
    private final Map<UUID, Long> lastOpen = new ConcurrentHashMap<>();
    private final GuiSessionRegistry sessions;

    public GuiManager(
            @NotNull JavaPlugin plugin,
            @NotNull ReloadManager reload,
            @NotNull ProfileCache cache,
            @NotNull RelationshipRepository relationships,
            @NotNull FamilyRepository family,
            @NotNull GuiSessionRegistry sessions
    ) {
        this.plugin = plugin;
        this.reload = reload;
        this.cache = cache;
        this.relationships = relationships;
        this.family = family;
        this.sessions = sessions;
    }

    public @NotNull GuiSessionRegistry sessions() {
        return sessions;
    }

    public boolean tryCooldown(@NotNull Player p) {
        long now = System.currentTimeMillis();
        MainConfig mc = reload.main();
        Long prev = lastOpen.get(p.getUniqueId());
        if (prev != null && now - prev < mc.guiCooldownMs()) {
            return false;
        }
        lastOpen.put(p.getUniqueId(), now);
        return true;
    }

    public void clearGuiCooldown(@NotNull Player p) {
        lastOpen.remove(p.getUniqueId());
    }

    public int hubSlot(@NotNull String key) {
        return reload.gui().hubSlot(key);
    }

    /** Слот кнопки «В меню» для сундука с заданным числом рядов. */
    public int menuBackSlot(int chestRows) {
        return reload.gui().toHubSlot(chestRows);
    }

    public void runNext(@NotNull Runnable r) {
        plugin.getServer().getScheduler().runTask(plugin, r);
    }

    public void openHub(@NotNull Player viewer) {
        GuiConfig g = reload.gui();
        Inventory inv = Bukkit.createInventory(null, g.rows("hub") * 9, MINI.deserialize(g.title("hub")));
        sessions.open(viewer.getUniqueId(), GuiSessionRegistry.Kind.HUB);
        fillFiller(inv, g);
        inv.setItem(g.hubSlot("profile"), button(Material.BOOK, g.label("hub_profile"), g.label("hub_profile_hint")));
        inv.setItem(g.hubSlot("family"), button(Material.CAKE, g.label("hub_family"), g.label("hub_family_hint")));
        inv.setItem(g.hubSlot("tree"), button(Material.OAK_SAPLING, g.label("hub_tree"), g.label("hub_tree_hint")));
        inv.setItem(g.hubSlot("history"), button(Material.PAPER, g.label("hub_history"), g.label("hub_history_hint")));
        inv.setItem(g.hubSlot("settings"), button(Material.LIME_DYE, g.label("hub_settings"), g.label("hub_settings_hint")));
        inv.setItem(g.hubSlot("close"), button(Material.BARRIER, g.label("hub_close"), g.label("hub_close_hint")));
        viewer.openInventory(inv);
    }

    public void openProfile(@NotNull Player viewer) {
        if (!tryCooldown(viewer)) {
            return;
        }
        GuiConfig g = reload.gui();
        ProfileSnapshot snap = cache.getCachedOrEmpty(viewer.getUniqueId(), viewer.getName());
        int rows = g.rows("profile");
        Inventory inv = Bukkit.createInventory(null, rows * 9, MINI.deserialize(g.title("profile")));
        sessions.open(viewer.getUniqueId(), GuiSessionRegistry.Kind.PROFILE);
        fillFiller(inv, g);
        inv.setItem(13, headItem(viewer.getUniqueId(), g.label("you"), viewer.getName()));
        if (snap.partnerUuid() != null) {
            inv.setItem(15, headItem(snap.partnerUuid(), g.label("partner"), snap.partnerName() == null ? "?" : snap.partnerName()));
        }
        inv.setItem(31, button(Material.BOOK, g.label("history_title"), g.label("history_hint")));
        placeBackToMenu(inv, g, rows);
        viewer.openInventory(inv);
    }

    public void openFamily(@NotNull Player viewer) {
        if (!tryCooldown(viewer)) {
            return;
        }
        UUID u = viewer.getUniqueId();
        family.activeLinksFor(u).whenComplete((links, ex) -> plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!viewer.isOnline()) {
                return;
            }
            GuiConfig g = reload.gui();
            Inventory inv = Bukkit.createInventory(null, g.rows("family") * 9, MINI.deserialize(g.title("family")));
            sessions.open(u, GuiSessionRegistry.Kind.FAMILY);
            fillFiller(inv, g);
            int slot = 10;
            for (FamilyLinkRow link : links) {
                if (slot > 34) {
                    break;
                }
                UUID other = link.fromUuid().equals(u) ? link.toUuid() : link.fromUuid();
                inv.setItem(slot++, headItem(other, g.linkTypeLabel(link.linkType().name()), resolveName(other)));
            }
            placeBackToMenu(inv, g, g.rows("family"));
            viewer.openInventory(inv);
        }));
    }

    public void openTree(@NotNull Player viewer) {
        if (!tryCooldown(viewer)) {
            return;
        }
        UUID u = viewer.getUniqueId();
        CompletableFuture<Optional<RelationshipRow>> fRel = relationships.findActiveFor(u);
        CompletableFuture<List<UUID>> fParents = family.parentsOf(u);
        fRel.thenCombine(fParents, (relOpt, parents) -> new TreeData(relOpt.orElse(null), parents))
                .whenComplete((data, ex) -> plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (!viewer.isOnline()) {
                        return;
                    }
                    GuiConfig g = reload.gui();
                    Inventory inv = Bukkit.createInventory(null, g.rows("tree") * 9, MINI.deserialize(g.title("tree")));
                    sessions.open(u, GuiSessionRegistry.Kind.TREE);
                    fillFiller(inv, g);
                    inv.setItem(13, headItem(u, g.label("tree_root"), viewer.getName()));
                    int i = 29;
                    for (UUID p : data.parents()) {
                        if (i > 33) {
                            break;
                        }
                        inv.setItem(i++, headItem(p, g.label("parent"), resolveName(p)));
                    }
                    i = 11;
                    List<UUID> children = cache.getCachedOrEmpty(u, viewer.getName()).childrenIds();
                    for (UUID ch : children) {
                        if (i > 17) {
                            break;
                        }
                        inv.setItem(i++, headItem(ch, g.label("child"), resolveName(ch)));
                    }
                    if (data.relationship() != null) {
                        RelationshipRow rel = data.relationship();
                        UUID partner = rel.playerLow().equals(u) ? rel.playerHigh() : rel.playerLow();
                        inv.setItem(22, headItem(partner, g.label("partner"), resolveName(partner)));
                    }
                    placeBackToMenu(inv, g, g.rows("tree"));
                    viewer.openInventory(inv);
                }));
    }

    private record TreeData(RelationshipRow relationship, List<UUID> parents) {
    }

    public void openHistory(@NotNull Player viewer) {
        if (!tryCooldown(viewer)) {
            return;
        }
        UUID u = viewer.getUniqueId();
        relationships.historyFor(u, 45).whenComplete((rows, ex) -> plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!viewer.isOnline()) {
                return;
            }
            GuiConfig g = reload.gui();
            Inventory inv = Bukkit.createInventory(null, g.rows("history") * 9, MINI.deserialize(g.title("history")));
            sessions.open(u, GuiSessionRegistry.Kind.HISTORY);
            fillFiller(inv, g);
            int slot = 10;
            for (RelationshipRow row : rows) {
                if (slot >= inv.getSize() - 9) {
                    break;
                }
                inv.setItem(slot++, paperRow(row, g));
            }
            placeBackToMenu(inv, g, g.rows("history"));
            viewer.openInventory(inv);
        }));
    }

    public void openSettings(@NotNull Player viewer) {
        if (!tryCooldown(viewer)) {
            return;
        }
        GuiConfig g = reload.gui();
        Inventory inv = Bukkit.createInventory(null, g.rows("settings") * 9, MINI.deserialize(g.title("settings")));
        sessions.open(viewer.getUniqueId(), GuiSessionRegistry.Kind.SETTINGS);
        fillFiller(inv, g);
        inv.setItem(13, settingsInfoItem(g));
        placeBackToMenu(inv, g, g.rows("settings"));
        viewer.openInventory(inv);
    }

    private @NotNull ItemStack paperRow(@NotNull RelationshipRow row, @NotNull GuiConfig g) {
        ItemStack p = new ItemStack(Material.PAPER);
        ItemMeta m = p.getItemMeta();
        if (m != null) {
            String statusRu = reload.messagesRaw().raw("placeholders.status." + row.status().name(), row.status().name());
            m.displayName(MINI.deserialize("<gray>" + statusRu + "</gray>"));
            m.lore(List.of(
                    MINI.deserialize("<dark_gray>Начало: <white>" + row.startedAt() + "</white></dark_gray>"),
                    MINI.deserialize(row.endedAt() == null
                            ? "<green>Активно"
                            : "<red>Окончено: <white>" + row.endedAt() + "</white>")
            ));
            p.setItemMeta(m);
        }
        return p;
    }

    private void placeBackToMenu(@NotNull Inventory inv, @NotNull GuiConfig g, int rows) {
        int slot = g.toHubSlot(rows);
        inv.setItem(slot, button(Material.ARROW, g.label("nav_back_to_menu"), g.label("nav_back_to_menu_hint")));
    }

    private void fillFiller(@NotNull Inventory inv, @NotNull GuiConfig g) {
        Material mat = Material.matchMaterial(g.fillerMaterial());
        if (mat == null) {
            mat = Material.GRAY_STAINED_GLASS_PANE;
        }
        ItemStack f = new ItemStack(mat);
        ItemMeta m = f.getItemMeta();
        if (m != null) {
            m.displayName(MINI.deserialize("<gray> </gray>"));
            f.setItemMeta(m);
        }
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, f.clone());
        }
    }

    private @NotNull ItemStack headItem(@NotNull UUID uuid, @NotNull String titleMini, @NotNull String name) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta sm = (SkullMeta) skull.getItemMeta();
        if (sm != null) {
            sm.displayName(MINI.deserialize(titleMini));
            sm.lore(List.of(MINI.deserialize("<white>" + name)));
            OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
            sm.setOwningPlayer(op);
            skull.setItemMeta(sm);
        }
        return skull;
    }

    private @NotNull ItemStack settingsInfoItem(@NotNull GuiConfig g) {
        ItemStack s = new ItemStack(Material.LIME_DYE);
        ItemMeta m = s.getItemMeta();
        if (m != null) {
            m.displayName(MINI.deserialize(g.label("settings_gender")));
            m.lore(List.of(
                    MINI.deserialize(g.label("settings_gender_lore_1")),
                    MINI.deserialize(g.label("settings_gender_lore_2"))
            ));
            s.setItemMeta(m);
        }
        return s;
    }

    private @NotNull ItemStack button(@NotNull Material mat, @NotNull String title, @NotNull String hint) {
        ItemStack s = new ItemStack(mat);
        ItemMeta m = s.getItemMeta();
        if (m != null) {
            m.displayName(MINI.deserialize(title));
            m.lore(List.of(MINI.deserialize(hint)));
            s.setItemMeta(m);
        }
        return s;
    }

    private static @NotNull String resolveName(@NotNull UUID uuid) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
        String n = op.getName();
        return n == null ? uuid.toString().substring(0, 8) : n;
    }
}
