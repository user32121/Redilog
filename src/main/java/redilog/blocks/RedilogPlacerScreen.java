package redilog.blocks;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.EditBoxWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

public class RedilogPlacerScreen extends HandledScreen<RedilogPlacerScreenHandler> {

    private EditBoxWidget inputEditBox;

    public RedilogPlacerScreen(RedilogPlacerScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Override
    public void resize(MinecraftClient client, int width, int height) {
        String input = inputEditBox.getText();
        super.resize(client, width, height);
        inputEditBox.setText(input);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        //bypass pressing 'e' (inventory key) to close screen
        if (this.client.options.inventoryKey.matchesKey(keyCode, scanCode)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected void init() {
        super.init();

        inputEditBox = new EditBoxWidget(textRenderer, width / 2 - 150, 50, 300, height - 100,
                Text.translatable("screen.redilog.redilog_placer.placeholder"),
                Text.translatable("screen.redilog.redilog_placer.name"));
        inputEditBox.setText("");
        addDrawableChild(inputEditBox);
        setInitialFocus(inputEditBox);
    }

    @Override
    protected void handledScreenTick() {
        super.handledScreenTick();
        inputEditBox.tick();
    }

    @Override
    protected void drawBackground(MatrixStack matrices, float delta, int mouseX, int mouseY) {
        this.renderBackground(matrices);
    }

    @Override
    protected void drawForeground(MatrixStack matrices, int mouseX, int mouseY) {
        //NO OP
    }
}
