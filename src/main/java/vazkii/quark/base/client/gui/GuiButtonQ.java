package vazkii.quark.base.client.gui;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;

public class GuiButtonQ extends GuiButton {

	public GuiButtonQ(int x, int y) {
		super(-8934892, x, y, 20, 20, "q");
	}
	
	@Override
	public void drawCenteredString(FontRenderer fontRendererIn, String text, int x, int y, int color) {
		super.drawCenteredString(fontRendererIn, text, x, y, 0x48ddbc);
	}

}
