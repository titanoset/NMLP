package com.nmlp;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Persistent data container keys for anti-forgery document items.
 */
public final class NMLPKeys {

    private final NamespacedKey documentType;
    private final NamespacedKey documentSignature;
    private final NamespacedKey ringMarker;

    public NMLPKeys(@NotNull Plugin plugin) {
        this.documentType = new NamespacedKey(plugin, "document_type");
        this.documentSignature = new NamespacedKey(plugin, "document_sig");
        this.ringMarker = new NamespacedKey(plugin, "ring");
    }

    public @NotNull NamespacedKey documentType() {
        return documentType;
    }

    public @NotNull NamespacedKey documentSignature() {
        return documentSignature;
    }

    public @NotNull NamespacedKey ringMarker() {
        return ringMarker;
    }
}
