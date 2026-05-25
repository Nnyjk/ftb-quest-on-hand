package com.planboard.neoforge;

import com.planboard.neoforge.client.PlanBoardKeyMappings;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(PlanBoardMod.MODID)
public class PlanBoardMod {

    public static final String MODID = "planboard";
    public static final Logger LOGGER = LoggerFactory.getLogger(PlanBoardMod.class);

    public PlanBoardMod(IEventBus modEventBus) {
        LOGGER.info("Plan Board initializing...");

        // Register deferred work
        modEventBus.addListener(this::onCommonSetup);
    }

    private void onCommonSetup(net.neoforged.neoforge.event.BuildCapacityEvent event) {
        LOGGER.info("Plan Board common setup complete.");
    }
}
