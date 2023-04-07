package redilog.init;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import redilog.blocks.RedilogPlacerScreen;

public class RedilogClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HandledScreens.register(Redilog.REDILOG_PLACER_SCREEN_HANDLER, RedilogPlacerScreen::new);
    }
}
