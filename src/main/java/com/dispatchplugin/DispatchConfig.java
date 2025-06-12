package com.dispatchplugin;

// CORRECTED IMPORTS from the new API package structure
import eu.darkbot.api.config.annotations.Configuration;
import eu.darkbot.api.config.annotations.Option;
import eu.darkbot.api.config.annotations.Number;

@Configuration("dispatch_manager")
public class DispatchConfig {

    @Option("Enable R-01 (Credits)")
    public boolean enable_R_01 = true;
    @Option("R-01 Priority")
    @Number(min = -10, max = 99, step = 1)
    public int priority_R_01 = 1;

    @Option("Enable R-02 (Credits)")
    public boolean enable_R_02 = true;
    @Option("R-02 Priority")
    @Number(min = -10, max = 99, step = 1)
    public int priority_R_02 = 2;

    @Option("Enable R-03 (Credits)")
    public boolean enable_R_03 = false;
    @Option("R-03 Priority")
    @Number(min = -10, max = 99, step = 1)
    public int priority_R_03 = 3;

    @Option("Enable Ace R-01 (Uridium)")
    public boolean enable_ACE_R_01 = false;
    @Option("Ace R-01 Priority")
    @Number(min = -10, max = 99, step = 1)
    public int priority_ACE_R_01 = 4;

    @Option("Enable Ace R-02 (Uridium)")
    public boolean enable_ACE_R_02 = false;
    @Option("Ace R-02 Priority")
    @Number(min = -10, max = 99, step = 1)
    public int priority_ACE_R_02 = 5;
}