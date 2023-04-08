package redilog.init;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import redilog.blocks.BuilderScreen;

public class RedilogClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HandledScreens.register(Redilog.BUILDER_SCREEN_HANDLER, BuilderScreen::new);
    }
}
