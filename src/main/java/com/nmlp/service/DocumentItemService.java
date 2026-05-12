package com.nmlp.service;

import com.nmlp.NMLPKeys;
import com.nmlp.config.ItemsConfig;
import com.nmlp.config.MainConfig;
import com.nmlp.config.ReloadManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Builds ring and document items with PDC signatures.
 */
public final class DocumentItemService {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final ReloadManager reload;
    private final NMLPKeys keys;

    public DocumentItemService(@NotNull ReloadManager reload, @NotNull NMLPKeys keys) {
        this.reload = reload;
        this.keys = keys;
    }

    public boolean isRing(@NotNull ItemStack stack) {
        if (stack.getType().isAir()) {
            return false;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return false;
        }
        Byte v = meta.getPersistentDataContainer().get(keys.ringMarker(), PersistentDataType.BYTE);
        return v != null && v == 1;
    }

    public @NotNull ItemStack createRing() {
        MainConfig mc = reload.main();
        ItemsConfig ic = reload.items();
        Material mat = Material.matchMaterial(mc.ringMaterial());
        if (mat == null) {
            mat = Material.CLOCK;
        }
        ItemStack stack = new ItemStack(mat);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(MINI.deserialize(ic.ringName()));
            List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
            for (String line : ic.ringLore()) {
                lore.add(MINI.deserialize(line));
            }
            meta.lore(lore);
            meta.getPersistentDataContainer().set(keys.ringMarker(), PersistentDataType.BYTE, (byte) 1);
            if (mc.ringCustomModelData() > 0) {
                meta.setCustomModelData(mc.ringCustomModelData());
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public @NotNull ItemStack createDocument(@NotNull String docKey, @NotNull UUID signatureSeed) {
        ItemsConfig ic = reload.items();
        Material mat = Material.matchMaterial(ic.docMaterial(docKey));
        if (mat == null) {
            mat = Material.PAPER;
        }
        ItemStack stack = new ItemStack(mat);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(MINI.deserialize(ic.docName(docKey)));
            int cmd = ic.docCmd(docKey);
            if (cmd > 0) {
                meta.setCustomModelData(cmd);
            }
            meta.getPersistentDataContainer().set(keys.documentType(), PersistentDataType.STRING, docKey);
            meta.getPersistentDataContainer().set(keys.documentSignature(), PersistentDataType.STRING, signatureSeed.toString());
            stack.setItemMeta(meta);
        }
        return stack;
    }
}
