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


@Feature(name = "Dispatch Automation", description = "Automates hiring and collecting from Dispatch",enabledByDefault = false)
public class DispatchPlugin implements Module, Configurable<DispatchConfig> {

    private DispatchAPI dispatchAPI;
    private StatsAPI statAPI;
    private DispatchConfig config;

    @Override
    public void setConfig(eu.darkbot.api.config.ConfigSetting<DispatchConfig> configSetting) {
        this.config = configSetting.getValue();
    }
    private DispatchManager dispatchManager;


    // Map retriever display names to config field names
    private static final Map<String, String> NAME_TO_CONFIG = new HashMap<>();
    static {
        NAME_TO_CONFIG.put("R-01", "R_01");
        NAME_TO_CONFIG.put("R-02", "R_02");
        NAME_TO_CONFIG.put("R-03", "R_03");
        NAME_TO_CONFIG.put("ACE R-01", "ACE_R_01");
        NAME_TO_CONFIG.put("ACE R-02", "ACE_R_02");
    }

    
    public void setAPI(PluginAPI api) {
        this.dispatchAPI = api.requireAPI(DispatchAPI.class);
        this.statAPI = api.requireAPI(StatsAPI.class);
        this.dispatchManager = api.requireAPI(DispatchManager.class);
    }

    public void setConfig(DispatchConfig config) {
        this.config = config;
    }

    
    public DispatchConfig getConfig() {
        return config;
    }

    
    public void Ontick() {
        if (dispatchAPI == null || config == null) return;

        collectFinished();

        int freeSlots = dispatchAPI.getAvailableSlots();
        if (freeSlots <= 0) return;

        // Get all available retrievers that are enabled in config, sort by priority (lower = higher)
        List<Retriever> toHire = dispatchAPI.getAvailableRetrievers().stream()
                .filter(this::isEnabled)
                .filter(this::canHireRetriever)
                .sorted(Comparator.comparingInt(this::getPriority))
                .collect(Collectors.toList());

        if (!toHire.isEmpty()) {
            Retriever retriever = toHire.get(0);
            dispatchAPI.overrideSelectedRetriever(retriever);
            dispatchManager.clickHire();
        }
    }

    @Override
    public void onTickModule() {
        Ontick();
    }

    private void collectFinished() {
        int idx = 0;
        for (Retriever retriever : dispatchAPI.getInProgressRetrievers()) {
            if (retriever.getDuration() <= 0) {
                dispatchManager.clickCollect(idx);
            }
            idx++;
        }
    }
    private boolean isEnabled(Retriever retriever) {
        String key = NAME_TO_CONFIG.getOrDefault(retriever.getName(), null);
        if (key == null) return false;
        try {
            return (boolean) DispatchConfig.class.getField("enable_" + key).get(config);
        } catch (Exception e) {
            return false;
        }
    }

    private int getPriority(Retriever retriever) {
        String key = NAME_TO_CONFIG.getOrDefault(retriever.getName(), null);
        if (key == null) return 100;
        try {
            return (int) DispatchConfig.class.getField("priority_" + key).get(config);
        } catch (Exception e) {
            return 100;
        }
    }

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