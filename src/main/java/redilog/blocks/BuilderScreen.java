package redilog.blocks;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.EditBoxWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import redilog.init.Redilog;

public class BuilderScreen extends HandledScreen<BuilderScreenHandler> {

    private EditBoxWidget redilogEditBox;
    private ButtonWidget saveButton;
    private ButtonWidget buildButton;

    public BuilderScreen(BuilderScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Override
    public void resize(MinecraftClient client, int width, int height) {
        String input = redilogEditBox.getText();
        super.resize(client, width, height);
        redilogEditBox.setText(input);
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

        redilogEditBox = new EditBoxWidget(textRenderer, width / 2 - 150, 50, 300, height - 100,
                Text.translatable("screen.redilog.builder.editbox.placeholder"),
                Text.translatable("screen.redilog.builder.editbox.name"));
        redilogEditBox.setText(handler.getOriginalText());
        addDrawableChild(redilogEditBox);
        setInitialFocus(redilogEditBox);

        saveButton = new ButtonWidget(width / 2 - 150, 50 + height - 100 + 10, 145, 20,
                Text.translatable("screen.redilog.builder.save.text"), this::onPress);
        addDrawableChild(saveButton);

        buildButton = new ButtonWidget(width / 2 + 5, 50 + height - 100 + 10, 145, 20,
                Text.translatable("screen.redilog.builder.build.text"), this::onPress);
        addDrawableChild(buildButton);
    }

    @Override
    protected void handledScreenTick() {
        super.handledScreenTick();
        redilogEditBox.tick();
    }

    @Override
    protected void drawBackground(MatrixStack matrices, float delta, int mouseX, int mouseY) {
        this.renderBackground(matrices);
    }

    @Override
    protected void drawForeground(MatrixStack matrices, int mouseX, int mouseY) {
        //NO OP
    }

    private void onPress(ButtonWidget button) {
        PacketByteBuf packet = PacketByteBufs.create();
        packet.writeIdentifier(handler.getWorldId());
        packet.writeBlockPos(handler.getPos());
        packet.writeString(redilogEditBox.getText());
        packet.writeBoolean(button == buildButton); //determines if should also run build process
        ClientPlayNetworking.send(Redilog.BUILDER_SYNC_PACKET, packet);
        close();
    }
}
