package com.nmlp;

import com.nmlp.command.*;
import com.nmlp.config.MessageService;
import com.nmlp.config.ReloadManager;
import com.nmlp.database.HikariSourceFactory;
import com.nmlp.database.MigrationRunner;
import com.nmlp.gui.GuiManager;
import com.nmlp.gui.GuiSessionRegistry;
import com.nmlp.integration.NmlpPlaceholderExpansion;
import com.nmlp.integration.TabPlaceholderNote;
import com.nmlp.integration.VaultHook;
import com.nmlp.listener.GuiProtectListener;
import com.nmlp.listener.PlayerJoinListener;
import com.nmlp.listener.RingInteractListener;
import com.nmlp.listener.RiskDeathListener;
import com.nmlp.repository.*;
import com.nmlp.service.*;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

/**
 * NMLP — No More Lonely Person (social / relationships / family).
 */
public final class NMLPPlugin extends JavaPlugin {

    private volatile boolean debugRuntime;

    private ExecutorService asyncExecutor;
    private HikariDataSource dataSource;
    private ReloadManager reloadManager;
    private ProfileCache profileCache;
    private GenderService genderService;
    private RelationshipService relationshipService;
    private FamilyService familyService;
    private DocumentItemService documentItemService;
    private EffectService effectService;
    private GuiManager guiManager;
    private GuiSessionRegistry guiSessions;
    private VaultHook vaultHook;
    private AffectionService affectionService;
    private RiskActionService riskActionService;

    @Override
    public void onEnable() {
        try {
            MessageService messages = new MessageService();
            reloadManager = new ReloadManager(this, messages);
            reloadManager.loadAll();
            syncDebugFromConfig();

            dataSource = HikariSourceFactory.create(reloadManager.main(), getDataFolder());
            MigrationRunner migrations = new MigrationRunner(dataSource, getClass().getClassLoader(), getLogger());
            migrations.migrate();

            asyncExecutor = Executors.newFixedThreadPool(
                    Math.max(1, reloadManager.main().asyncPoolThreads()),
                    r -> {
                        Thread t = new Thread(r, "NMLP-Async");
                        t.setDaemon(true);
                        return t;
                    }
            );

            PlayerRepository players = new SqlPlayerRepository(dataSource, asyncExecutor);
            RelationshipRepository relationships = new SqlRelationshipRepository(dataSource, asyncExecutor);
            FamilyRepository family = new SqlFamilyRepository(dataSource, asyncExecutor);
            HistoryRepository history = new SqlHistoryRepository(dataSource, asyncExecutor);

            profileCache = new ProfileCache(players, relationships, family);
            NMLPKeys keys = new NMLPKeys(this);
            documentItemService = new DocumentItemService(reloadManager, keys);
            effectService = new EffectService(reloadManager.main());
            genderService = new GenderService(players, history, profileCache);
            relationshipService = new RelationshipService(this, effectService, relationships, history, profileCache, reloadManager.main());
            familyService = new FamilyService(family, history, profileCache);
            affectionService = new AffectionService(reloadManager, messages, profileCache);
            riskActionService = new RiskActionService(this, reloadManager, messages);

            guiSessions = new GuiSessionRegistry();
            guiManager = new GuiManager(this, reloadManager, profileCache, relationships, family, guiSessions);

            vaultHook = new VaultHook();
            vaultHook.trySetup(this);
            TabPlaceholderNote.logOnce(this);

            registerCommands();
            getServer().getPluginManager().registerEvents(new PlayerJoinListener(this, players, messages, profileCache), this);
            getServer().getPluginManager().registerEvents(new RingInteractListener(this, reloadManager, documentItemService, relationshipService, messages), this);
            getServer().getPluginManager().registerEvents(new GuiProtectListener(guiSessions, guiManager), this);
            getServer().getPluginManager().registerEvents(new RiskDeathListener(reloadManager, riskActionService), this);

            if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
                boolean ok = new NmlpPlaceholderExpansion(profileCache, reloadManager).register();
                getLogger().info("PlaceholderAPI expansion registered: " + ok);
            }
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to enable NMLP", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    private void registerCommands() {
        MessageService messages = reloadManager.messages();
        if (getCommand("gender") != null) {
            GenderCommand gc = new GenderCommand(this, genderService, messages);
            getCommand("gender").setExecutor(gc);
            getCommand("gender").setTabCompleter(gc);
        }
        if (getCommand("engage") != null) {
            EngageCommand ec = new EngageCommand(this, relationshipService, messages, documentItemService);
            getCommand("engage").setExecutor(ec);
            getCommand("engage").setTabCompleter(ec);
        }
        if (getCommand("marry") != null) {
            getCommand("marry").setExecutor(new MarryCommand(this, relationshipService, messages, documentItemService));
        }
        if (getCommand("divorce") != null) {
            getCommand("divorce").setExecutor(new DivorceCommand(this, relationshipService, messages));
        }
        if (getCommand("partner") != null) {
            getCommand("partner").setExecutor(new PartnerCommand(guiManager, messages));
        }
        if (getCommand("family") != null) {
            getCommand("family").setExecutor(new FamilyCommand(guiManager, messages));
        }
        if (getCommand("tree") != null) {
            getCommand("tree").setExecutor(new TreeCommand(guiManager, messages));
        }
        if (getCommand("adopt") != null) {
            AdoptCommand ac = new AdoptCommand(this, familyService, messages);
            getCommand("adopt").setExecutor(ac);
            getCommand("adopt").setTabCompleter(ac);
        }
        if (getCommand("hug") != null) {
            AffectionCommand hug = new AffectionCommand(affectionService, AffectionKind.HUG);
            getCommand("hug").setExecutor(hug);
            getCommand("hug").setTabCompleter(hug);
        }
        if (getCommand("kiss") != null) {
            AffectionCommand kiss = new AffectionCommand(affectionService, AffectionKind.KISS);
            getCommand("kiss").setExecutor(kiss);
            getCommand("kiss").setTabCompleter(kiss);
        }
        if (getCommand("risk") != null) {
            RiskCommand rc = new RiskCommand(reloadManager, riskActionService, messages);
            getCommand("risk").setExecutor(rc);
            getCommand("risk").setTabCompleter(rc);
        }
        if (getCommand("nmlp") != null) {
            getCommand("nmlp").setExecutor(new NmlpAdminCommand(this, reloadManager, messages, documentItemService));
        }
    }

    @Override
    public void onDisable() {
        if (asyncExecutor != null) {
            asyncExecutor.shutdown();
        }
        if (dataSource != null) {
            dataSource.close();
        }
    }

    public void setDebug(boolean v) {
        this.debugRuntime = v;
    }

    public boolean debug() {
        return debugRuntime;
    }

    public void syncDebugFromConfig() {
        this.debugRuntime = reloadManager != null && reloadManager.main().debug();
    }

    public @NotNull EffectService effectService() {
        return effectService;
    }

    public @NotNull ProfileCache profileCache() {
        return profileCache;
    }
}
