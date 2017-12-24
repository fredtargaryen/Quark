/**
 * This class was created by <Vazkii>. It's distributed as
 * part of the Quark Mod. Get the Source Code in github:
 * https://github.com/Vazkii/Quark
 *
 * Quark is Open Source and distributed under the
 * CC-BY-NC-SA 3.0 License: https://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB
 *
 * File Created @ [01/06/2016, 23:36:06 (GMT)]
 */
package vazkii.quark.tweaks.feature;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.item.EntityMinecartChest;
import net.minecraft.entity.item.EntityMinecartCommandBlock;
import net.minecraft.entity.item.EntityMinecartEmpty;
import net.minecraft.entity.item.EntityMinecartFurnace;
import net.minecraft.entity.item.EntityMinecartHopper;
import net.minecraft.entity.item.EntityMinecartMobSpawner;
import net.minecraft.entity.item.EntityMinecartTNT;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import vazkii.quark.base.module.Feature;

public class MinecartInteraction extends Feature {

	Map<Item, Function<EntityMinecartEmpty, EntityMinecart>> inserters = new HashMap();
	boolean enableCommandAndSpawner;

	@Override
	public void setupConfig() {
		enableCommandAndSpawner = loadPropBool("Enable Command Block and Mob Spawner", "", true);
	}

	@Override
	public void init(FMLInitializationEvent event) {
		inserters.put(Item.getItemFromBlock(Blocks.CHEST), (EntityMinecartEmpty e) -> new EntityMinecartChest(e.getEntityWorld(), e.posX, e.posY, e.posZ));
		inserters.put(Item.getItemFromBlock(Blocks.TNT), (EntityMinecartEmpty e) -> new EntityMinecartTNT(e.getEntityWorld(), e.posX, e.posY, e.posZ));
		inserters.put(Item.getItemFromBlock(Blocks.FURNACE), (EntityMinecartEmpty e) -> new EntityMinecartFurnace(e.getEntityWorld(), e.posX, e.posY, e.posZ));
		inserters.put(Item.getItemFromBlock(Blocks.HOPPER), (EntityMinecartEmpty e) -> new EntityMinecartHopper(e.getEntityWorld(), e.posX, e.posY, e.posZ));

		if(enableCommandAndSpawner) {
			inserters.put(Item.getItemFromBlock(Blocks.COMMAND_BLOCK), (EntityMinecartEmpty e) -> new EntityMinecartCommandBlock(e.getEntityWorld(), e.posX, e.posY, e.posZ));
			inserters.put(Item.getItemFromBlock(Blocks.MOB_SPAWNER), (EntityMinecartEmpty e) -> new EntityMinecartMobSpawner(e.getEntityWorld(), e.posX, e.posY, e.posZ));
		}
	}

	@SubscribeEvent
	public void onEntityInteract(EntityInteract event) {
		Entity target = event.getTarget();
		if(target instanceof EntityMinecartEmpty && target.getPassengers().isEmpty()) {
			EntityPlayer player = event.getEntityPlayer();
			EnumHand hand = EnumHand.MAIN_HAND;
			ItemStack stack = player.getHeldItemMainhand();
			if(stack.isEmpty() || !inserters.containsKey(stack.getItem())) {
				stack = player.getHeldItemOffhand();
				hand = EnumHand.OFF_HAND;
			}

			if(!stack.isEmpty() && inserters.containsKey(stack.getItem())) {
				player.swingArm(hand);

				if(!event.getWorld().isRemote) {
					target.setDead();
					event.getWorld().spawnEntity(inserters.get(stack.getItem()).apply((EntityMinecartEmpty) target));


					event.setCanceled(true);
					if(!player.capabilities.isCreativeMode) {
						stack.shrink(1);

						if(stack.getCount() <= 0)
							player.setHeldItem(hand, ItemStack.EMPTY);
					}
				}
			}
		}
	}

	@Override
	public boolean hasSubscriptions() {
		return true;
	}
	
	@Override
	public boolean requiresMinecraftRestartToEnable() {
		return true;
	}
	
	@Override
	public String getFeatureIngameConfigName() {
		return "Minecart Block Placement";
	}

}
