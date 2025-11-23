package org.skydream.sphinxriddle;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

@Mod(SphinxRiddle.MODID)
public class SphinxRiddle {
    public static final String MODID = "sphinxriddle";
    private static final Logger LOGGER = LogUtils.getLogger();

    private static QuizManager quizManager;

    public SphinxRiddle(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::onClientSetup);
        NeoForge.EVENT_BUS.register(this);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("SphinxRiddle mod initialized");
    }

    private void onClientSetup(final FMLClientSetupEvent event) {
        LOGGER.info("SphinxRiddle client setup complete");
        event.enqueueWork(() -> LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName()));
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        try {
            quizManager = new QuizManager(event.getServer());
            quizManager.loadConfig();
            quizManager.startAutoQuestionTimer();
            quizManager.initializeScoreboard(); // 初始化计分板
            LOGGER.info("SphinxRiddle started successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to start SphinxRiddle", e);
        }
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        if (quizManager != null) {
            quizManager.stopAutoQuestionTimer();
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        QuizCommands.register(event.getDispatcher(), event.getBuildContext());
    }

    @SubscribeEvent
    public void onServerChat(ServerChatEvent event) {
        if (quizManager != null) {
            quizManager.handlePlayerAnswer(event.getPlayer(), event.getRawText());
        }
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        if (quizManager != null) {
            quizManager.tick();
        }
    }

    public static QuizManager getQuizManager() {
        return quizManager;
    }
}