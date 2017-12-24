package vazkii.quark.management.feature;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import vazkii.arl.network.NetworkHandler;
import vazkii.quark.base.module.Feature;
import vazkii.quark.base.module.ModuleLoader;
import vazkii.quark.base.network.message.MessageDeleteItem;
import vazkii.quark.management.client.gui.GuiButtonTrash;

public class DeleteItems extends Feature {

	boolean keyboardDown = false;
	boolean mouseDown = false;
	GuiButtonTrash trash;

//	Will be used eventually
//	@SubscribeEvent
//	@SideOnly(Side.CLIENT)
//	public void initGui(GuiScreenEvent.InitGuiEvent.Post event) {
//		trash = null;
//		if(event.getGui() instanceof GuiContainer) {
//			GuiContainer guiInv = (GuiContainer) event.getGui();
//			Container container = guiInv.inventorySlots;
//			EntityPlayer player = Minecraft.getMinecraft().player;
//			
//			boolean accept = guiInv instanceof GuiContainer;
//
//			if(!accept)
//				return;
//
//			int guiLeft = guiInv.getGuiLeft();
//			int guiTop = guiInv.getGuiTop();
//			int guiWidth = guiInv.getXSize();
//			int guiHeight = guiInv.getYSize();
//
//			for(Slot s : container.inventorySlots)
//				if(s.inventory == player.inventory && s.getSlotIndex() == 9) {
//					trash = new GuiButtonTrash(guiInv, 82424, guiLeft + guiWidth + 2, guiTop + guiHeight - 25);
//					event.getButtonList().add(trash);
//					break;
//				}
//		}
//	}
//
//	@SubscribeEvent
//	public void mouseEvent(GuiScreenEvent.MouseInputEvent.Pre event) {
//		handleMouseclick(event);
//	}
//	
//	@SubscribeEvent
//	public void mouseEvent(MouseEvent event) {
//		handleMouseclick(event);
//	}
//	
//	private void handleMouseclick(Event event) {
//		mouseDown = Mouse.isButtonDown(0);
//		boolean click = mouseDown;
//
//		GuiScreen current = Minecraft.getMinecraft().currentScreen;
//		if(click && current instanceof GuiContainer) {
//			GuiContainer gui = (GuiContainer) current;
//			if(trash != null && trash.ready) {
//				event.setCanceled(true);
//			}
//		}
//	}
	
	@SubscribeEvent
	public void keyboardEvent(GuiScreenEvent.KeyboardInputEvent.Post event) {
		boolean down = Keyboard.isKeyDown(Keyboard.KEY_DELETE);
		if(GuiScreen.isCtrlKeyDown() && down && !this.keyboardDown && event.getGui() instanceof GuiContainer) {
			GuiContainer gui = (GuiContainer) event.getGui();
			Slot slot = gui.getSlotUnderMouse();
			if(slot != null) {
				IInventory inv = slot.inventory;
				if(inv instanceof InventoryPlayer) {
					int index = slot.getSlotIndex();

					if(Minecraft.getMinecraft().player.capabilities.isCreativeMode && index >= 36)
						index -= 36; // Creative mode messes with the indexes for some reason

					if(index < ((InventoryPlayer) inv).mainInventory.size())
						NetworkHandler.INSTANCE.sendToServer(new MessageDeleteItem(index));
				}
			}
		}
		this.keyboardDown = down;
	}
	
	public static void deleteItem(EntityPlayer player, int slot) {
		if(slot > player.inventory.mainInventory.size() || !canItemBeDeleted(player.inventory.getStackInSlot(slot)))
			return;
		
		if(slot == player.inventory.mainInventory.size())
			player.inventory.setItemStack(ItemStack.EMPTY);

		player.inventory.setInventorySlotContents(slot, ItemStack.EMPTY);
	}
	
	public static boolean canItemBeDeleted(ItemStack stack) {
		return ModuleLoader.isFeatureEnabled(DeleteItems.class) && !stack.isEmpty() && !FavoriteItems.isItemFavorited(stack);
	}
	
	@Override
	public boolean hasSubscriptions() {
		return isClient();
	}
	
	@Override
	public String getFeatureIngameConfigName() {
		return "Delete Items";
	}
	
}
