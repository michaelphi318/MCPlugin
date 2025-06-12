package com.dispatchplugin;

import eu.darkbot.api.PluginAPI;
import eu.darkbot.api.extensions.Module;
import eu.darkbot.api.extensions.Feature;
import eu.darkbot.api.extensions.Configurable;
import eu.darkbot.api.managers.DispatchAPI;
import eu.darkbot.api.managers.DispatchAPI.Retriever;
import eu.darkbot.api.managers.StatsAPI;
import com.github.manolo8.darkbot.core.manager.DispatchManager;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Dispatch Automation Plugin
 * - Automates hiring and collecting from Dispatch using user-configurable options and priorities.
 * - Supports tracking via print statements for future debugging and diagnostics.
 */
@Feature(
        name = "Dispatch Automation",
        description = "Automates hiring and collecting from Dispatch",
        enabledByDefault = false
)
public class DispatchPlugin implements Module, Configurable<DispatchConfig> {

    private DispatchAPI dispatchAPI;
    private StatsAPI statAPI;
    private DispatchConfig config;
    private DispatchManager dispatchManager;

    // Map retriever display names to config field names for easy config access via reflection.
    private static final Map<String, String> NAME_TO_CONFIG = new HashMap<>();
    static {
        NAME_TO_CONFIG.put("R-01", "R_01");
        NAME_TO_CONFIG.put("R-02", "R_02");
        NAME_TO_CONFIG.put("R-03", "R_03");
        NAME_TO_CONFIG.put("ACE R-01", "ACE_R_01");
        NAME_TO_CONFIG.put("ACE R-02", "ACE_R_02");
    }

    /**
     * Called by DarkBot to inject APIs. Required to access game state.
     */
    public void setAPI(PluginAPI api) {
        this.dispatchAPI = api.requireAPI(DispatchAPI.class);
        this.statAPI = api.requireAPI(StatsAPI.class);
        this.dispatchManager = api.requireAPI(DispatchManager.class);
        System.out.println("[DispatchPlugin] setAPI called - APIs injected");
    }

    /**
     * Called by DarkBot to inject config setting (for use with @Configurable).
     */
    @Override
    public void setConfig(eu.darkbot.api.config.ConfigSetting<DispatchConfig> configSetting) {
        this.config = configSetting.getValue();
        System.out.println("[DispatchPlugin] setConfig called: config loaded");
    }

    /**
     * Overloaded for convenience or legacy support (not always needed).
     */
    public void setConfig(DispatchConfig config) {
        this.config = config;
        System.out.println("[DispatchPlugin] setConfig (direct) called: config loaded");
    }

    public DispatchConfig getConfig() {
        return config;
    }

    /**
     * Logic for each tick. This is called by DarkBot on every plugin tick.
     * Handles collecting finished retrievers and hiring new ones based on config.
     */
    public void Ontick() {
        if (dispatchAPI == null || config == null) {
            System.out.println("[DispatchPlugin] APIs or config not initialized, skipping tick");
            return;
        }

        // Collect finished retrievers before hiring
        collectFinished();

        int freeSlots = dispatchAPI.getAvailableSlots();
        System.out.println("[DispatchPlugin] Free dispatch slots: " + freeSlots);
        if (freeSlots <= 0) return;

        // Build sorted list of retrievers to hire (enabled, affordable, sorted by user priority)
        List<Retriever> toHire = dispatchAPI.getAvailableRetrievers().stream()
                .filter(this::isEnabled)
                .filter(this::canHireRetriever)
                .sorted(Comparator.comparingInt(this::getPriority))
                .collect(Collectors.toList());

        if (!toHire.isEmpty()) {
            Retriever retriever = toHire.get(0);
            System.out.println("[DispatchPlugin] Hiring retriever: " + retriever.getName() +
                    " (priority=" + getPriority(retriever) + ")");
            dispatchAPI.overrideSelectedRetriever(retriever);
            dispatchManager.clickHire();
        } else {
            System.out.println("[DispatchPlugin] No enabled/affordable retrievers to hire");
        }
    }

    /**
     * Called every tick by the bot's module system.
     */
    @Override
    public void onTickModule() {
        Ontick();
    }

    /**
     * Collects finished retrievers by clicking collect on each that is complete.
     */
    private void collectFinished() {
        int idx = 0;
        for (Retriever retriever : dispatchAPI.getInProgressRetrievers()) {
            if (retriever.getDuration() <= 0) {
                System.out.println("[DispatchPlugin] Collecting finished retriever at index: " + idx
                        + " (" + retriever.getName() + ")");
                dispatchManager.clickCollect(idx);
            }
            idx++;
        }
    }

    /**
     * Checks if a retriever is enabled in the config.
     */
    private boolean isEnabled(Retriever retriever) {
        String key = NAME_TO_CONFIG.getOrDefault(retriever.getName(), null);
        if (key == null) {
            System.out.println("[DispatchPlugin] Retriever not mapped in config: " + retriever.getName());
            return false;
        }
        try {
            boolean enabled = (boolean) DispatchConfig.class.getField("enable_" + key).get(config);
            System.out.println("[DispatchPlugin] isEnabled(" + retriever.getName() + ") = " + enabled);
            return enabled;
        } catch (Exception e) {
            System.out.println("[DispatchPlugin] Error checking enabled for " + retriever.getName() + ": " + e);
            return false;
        }
    }

    /**
     * Gets the configured priority for a retriever; returns 100 if not found.
     */
    private int getPriority(Retriever retriever) {
        String key = NAME_TO_CONFIG.getOrDefault(retriever.getName(), null);
        if (key == null) return 100;
        try {
            int priority = (int) DispatchConfig.class.getField("priority_" + key).get(config);
            System.out.println("[DispatchPlugin] getPriority(" + retriever.getName() + ") = " + priority);
            return priority;
        } catch (Exception e) {
            System.out.println("[DispatchPlugin] Error getting priority for " + retriever.getName() + ": " + e);
            return 100;
        }
    }

        /**
     * Check if player can afford to hire a retriever based on its costs.
     */
    private boolean canHireRetriever(Retriever retriever) {
        for (DispatchAPI.Cost cost : retriever.getCostList()) {
            String lootId = cost.getLootId().toLowerCase();
            int required = cost.getAmount();
            int available = getPlayerResource(lootId);
            if (available < required) return false;
        }
        return true;
    }

    private int getPlayerResource(String lootId) {
        switch (lootId) {
            case "credits":
                return (int) statAPI.getTotalCredits();
            case "uridium":
                return (int) statAPI.getTotalUridium();
            default:
                try {
                    return (int) statAPI.getTotalUridium();
                } catch (Exception e) {
                    return 0;
                }
        }
    }
}