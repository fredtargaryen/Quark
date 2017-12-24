package vazkii.quark.base.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.client.event.RenderHandEvent;
import vazkii.quark.base.module.Module;

public class GuiButtonModule extends GuiButton {

	final Module module;
	
	public GuiButtonModule(int x, int y, Module module) {
		super(0, x, y, 150, 20, I18n.translateToLocal("quark.config.module." + module.name));
		this.module = module;
	}
	
	@Override
	public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
		super.drawButton(mc, mouseX, mouseY, partialTicks);
		
		if(visible) {
			ItemStack stack = module.getIconStack();
			RenderHelper.enableGUIStandardItemLighting();
			GlStateManager.enableDepth();
			mc.getRenderItem().renderItemIntoGUI(stack, x + 6, y + 2);
		}
	}
	
	@Override
	public void drawCenteredString(FontRenderer fontRendererIn, String text, int x, int y, int color) {
		super.drawCenteredString(fontRendererIn, text, x + 14, y, color);
	}

}
