package com.nmlp.integration;

import com.nmlp.config.MessagesConfig;
import com.nmlp.config.ReloadManager;
import com.nmlp.service.ProfileCache;
import com.nmlp.service.ProfileSnapshot;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.stream.Collectors;

/**
 * PlaceholderAPI: %nmlp_*%. Пол иконками: {@code %nmlp_gender%}, {@code %nmlp_gender_Ник%}.
 * Код пола текстом: {@code %nmlp_gender_code%}, {@code %nmlp_gender_code_Ник%}.
 */
public final class NmlpPlaceholderExpansion extends PlaceholderExpansion {

    private final ProfileCache cache;
    private final ReloadManager reload;

    public NmlpPlaceholderExpansion(
            @NotNull ProfileCache cache,
            @NotNull ReloadManager reload
    ) {
        this.cache = cache;
        this.reload = reload;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "nmlp";
    }

    @Override
    public @NotNull String getAuthor() {
        return "NMLP";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (params == null || params.isBlank()) {
            return "";
        }
        String lower = params.toLowerCase(Locale.ROOT);
        String prefixGender = "gender_";
        String prefixGenderCode = "gender_code_";

        if (lower.startsWith(prefixGenderCode) && params.length() > prefixGenderCode.length()) {
            String name = params.substring(prefixGenderCode.length());
            return genderCodeForName(name);
        }
        if (lower.startsWith(prefixGender) && params.length() > prefixGender.length()) {
            String name = params.substring(prefixGender.length());
            if (name.isBlank()) {
                return iconUnknown();
            }
            return genderIconForName(name);
        }

        ProfileSnapshot s = player == null
                ? null
                : cache.getCachedOrEmpty(player.getUniqueId(), player.getName() == null ? "" : player.getName());

        return switch (lower) {
            case "partner" -> {
                if (s == null) {
                    yield "";
                }
                yield s.partnerName() == null ? none() : s.partnerName();
            }
            case "gender" -> {
                if (s == null) {
                    yield "";
                }
                yield genderIcon(s.genderCode());
            }
            case "gender_code" -> {
                if (s == null) {
                    yield "";
                }
                yield s.genderCode() == null ? none() : s.genderCode();
            }
            case "pronouns" -> {
                if (s == null) {
                    yield "";
                }
                yield pronounsDisplay(s.pronounsCode());
            }
            case "status" -> {
                if (s == null) {
                    yield "";
                }
                yield statusDisplay(s.relationshipStatus());
            }
            case "family" -> {
                if (s == null) {
                    yield "";
                }
                yield String.valueOf(s.childrenIds().size());
            }
            case "relation_since" -> {
                if (s == null) {
                    yield "";
                }
                yield s.relationSinceEpochMs() <= 0 ? none() : String.valueOf(s.relationSinceEpochMs());
            }
            case "children" -> {
                if (s == null) {
                    yield "";
                }
                yield s.childrenIds().stream()
                        .map(u -> {
                            OfflinePlayer op = Bukkit.getOfflinePlayer(u);
                            return op.getName() == null ? u.toString().substring(0, 8) : op.getName();
                        })
                        .collect(Collectors.joining(messages().raw("placeholders.ex-list-sep", ", ")));
            }
            case "exes" -> {
                if (s == null) {
                    yield "";
                }
                yield String.join(messages().raw("placeholders.ex-list-sep", ", "), s.exNames());
            }
            default -> null;
        };
    }

    private @NotNull MessagesConfig messages() {
        return reload.messagesRaw();
    }

    private @NotNull String none() {
        return messages().raw("placeholders.none", "—");
    }

    private @NotNull String genderIcon(@Nullable String genderCode) {
        MessagesConfig mc = messages();
        if (genderCode == null) {
            return mc.raw("icons.gender.unknown", "○");
        }
        return switch (genderCode.toLowerCase(Locale.ROOT)) {
            case "male" -> mc.raw("icons.gender.male", "♂");
            case "female" -> mc.raw("icons.gender.female", "♀");
            default -> mc.raw("icons.gender.unknown", "○");
        };
    }

    private @NotNull String iconUnknown() {
        return messages().raw("icons.gender.unknown", "○");
    }

    private @NotNull String genderIconForName(@NotNull String name) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(name);
        if (!op.hasPlayedBefore() && !op.isOnline()) {
            return iconUnknown();
        }
        String displayName = op.getName() == null ? name : op.getName();
        ProfileSnapshot snap = cache.getCachedOrEmpty(op.getUniqueId(), displayName);
        return genderIcon(snap.genderCode());
    }

    private @NotNull String genderCodeForName(@NotNull String name) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(name);
        if (!op.hasPlayedBefore() && !op.isOnline()) {
            return none();
        }
        String displayName = op.getName() == null ? name : op.getName();
        ProfileSnapshot snap = cache.getCachedOrEmpty(op.getUniqueId(), displayName);
        return snap.genderCode() == null ? none() : snap.genderCode();
    }

    private @NotNull String pronounsDisplay(@Nullable String code) {
        if (code == null) {
            return none();
        }
        String key = "placeholders.pronouns." + code.toLowerCase(Locale.ROOT);
        return messages().raw(key, code);
    }

    private @NotNull String statusDisplay(@NotNull String status) {
        String key = "placeholders.status." + status;
        return messages().raw(key, status);
    }
}
