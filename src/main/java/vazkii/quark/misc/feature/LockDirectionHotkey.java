package vazkii.quark.misc.feature;

import java.util.HashMap;

import org.lwjgl.opengl.GL11;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDirectional;
import net.minecraft.block.BlockHopper;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.BlockLog;
import net.minecraft.block.BlockQuartz;
import net.minecraft.block.BlockRotatedPillar;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.RayTraceResult.Type;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentKeybind;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.event.world.BlockEvent.PlaceEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.Event.Result;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import vazkii.arl.network.NetworkHandler;
import vazkii.quark.api.IRotationLockHandler;
import vazkii.quark.base.client.ModKeybinds;
import vazkii.quark.base.lib.LibMisc;
import vazkii.quark.base.module.Feature;
import vazkii.quark.base.network.message.MessageSetLockProfile;

public class LockDirectionHotkey extends Feature {

	private static final String TAG_LOCKED_ONCE = "quark:locked_once";
	
	private static final HashMap<String, LockProfile> lockProfiles = new HashMap();
	private LockProfile clientProfile;
	
	@Override
	@SideOnly(Side.CLIENT)
	public void preInitClient(FMLPreInitializationEvent event) {
		ModKeybinds.initLockKey();
	}
	
	@SubscribeEvent
	public void onBlockPlaced(PlaceEvent event) {
		if(event.isCanceled() || event.getResult() == Result.DENY)
			return;
		
		World world = event.getWorld();
		IBlockState state = event.getPlacedBlock();
		BlockPos pos = event.getPos();
		
		String name = event.getPlayer().getName();
		if(lockProfiles.containsKey(name)) {
			LockProfile profile = lockProfiles.get(name);
			setBlockRotated(world, state, pos, profile.facing.getOpposite(), true, profile.half);
		}
	}
	
	public static void setBlockRotated(World world, IBlockState state, BlockPos pos, EnumFacing face) {
		setBlockRotated(world, state, pos, face, false, -1);
	}

	public static void setBlockRotated(World world, IBlockState state, BlockPos pos, EnumFacing face, boolean stateCheck, int half) {
		IBlockState setState = state;
		ImmutableMap<IProperty<?>, Comparable<?>> props = state.getProperties(); 
		Block block = state.getBlock();
		
		// API hook
		if(block instanceof IRotationLockHandler)
			setState = ((IRotationLockHandler) block).setRotation(world, pos, setState, face, half != -1, half == 1);
		
		// General Facing
		else if(props.containsKey(BlockDirectional.FACING))
			setState = state.withProperty(BlockDirectional.FACING, face);
		
		// Horizontal Facing
		else if(props.containsKey(BlockHorizontal.FACING) && face.getAxis() != Axis.Y) {
			if(block instanceof BlockStairs)
				setState = state.withProperty(BlockHorizontal.FACING, face.getOpposite());
			else setState = state.withProperty(BlockHorizontal.FACING, face);
		} 
		
		// Pillar Axis
		else if(props.containsKey(BlockRotatedPillar.AXIS))
			setState = state.withProperty(BlockRotatedPillar.AXIS, face.getAxis());
		
		// Log Axis
		else if(props.containsKey(BlockLog.LOG_AXIS))
			setState = state.withProperty(BlockLog.LOG_AXIS, BlockLog.EnumAxis.fromFacingAxis(face.getAxis()));
		
		// Quartz Variant/Axis
		else if(props.containsKey(BlockQuartz.VARIANT)) {
			BlockQuartz.EnumType type = state.getValue(BlockQuartz.VARIANT);
			if(ImmutableSet.of(BlockQuartz.EnumType.LINES_X, BlockQuartz.EnumType.LINES_Y, BlockQuartz.EnumType.LINES_Z).contains(type))
				setState = state.withProperty(BlockQuartz.VARIANT, BlockQuartz.VARIANT.parseValue("lines_" + face.getAxis().getName()).or(BlockQuartz.EnumType.LINES_Y));
		}
		
		// Hopper Facing
		else if(props.containsKey(BlockHopper.FACING))
			setState = state.withProperty(BlockHopper.FACING, face == EnumFacing.DOWN ? face : face.getOpposite());
			
		if(half != -1) {
			if(block instanceof BlockStairs)
				setState = setState.withProperty(BlockStairs.HALF, half == 1 ? BlockStairs.EnumHalf.TOP : BlockStairs.EnumHalf.BOTTOM);
			else if(block instanceof BlockSlab)
				setState = setState.withProperty(BlockSlab.HALF, half == 1 ? BlockSlab.EnumBlockHalf.TOP : BlockSlab.EnumBlockHalf.BOTTOM);
		}
		
		if(!stateCheck || setState != state)
			world.setBlockState(pos, setState);
	}
	
	@SubscribeEvent
	public void onPlayerLogoff(PlayerLoggedOutEvent event) {
		lockProfiles.remove(event.player.getName());
	}

	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void onKeyInput(KeyInputEvent event) {
		Minecraft mc = Minecraft.getMinecraft();
		boolean down = ModKeybinds.lockKey.isKeyDown();
		if(mc.inGameHasFocus && down) {
			LockProfile newProfile;
			RayTraceResult result = mc.objectMouseOver;
			
			if(result != null && result.typeOfHit == Type.BLOCK) {
				int half = (int) ((result.hitVec.y - (int) result.hitVec.y) * 2);
				if(result.sideHit.getAxis() == Axis.Y)
					half = -1;
				
				newProfile = new LockProfile(result.sideHit.getOpposite(), half);

			} else {
				Vec3d look = mc.player.getLookVec();
				newProfile = new LockProfile(EnumFacing.getFacingFromVector((float) look.x, (float) look.y, (float) look.z), -1);
			}
			
			if(clientProfile != null && clientProfile.equals(newProfile))
				clientProfile = null;
			else clientProfile = newProfile;
			NetworkHandler.INSTANCE.sendToServer(new MessageSetLockProfile(clientProfile));
		}
	}
	
	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void onHUDRender(RenderGameOverlayEvent.Post event) {
		if(event.getType() == ElementType.ALL && clientProfile != null) {
			Minecraft mc = Minecraft.getMinecraft();
			GlStateManager.pushMatrix();
			GlStateManager.enableBlend();
			GlStateManager.enableAlpha();
			GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
			GlStateManager.color(1F, 1F, 1F, 0.5F);
			
			mc.renderEngine.bindTexture(LibMisc.GENERAL_ICONS_RESOURCE);
			
			int x = event.getResolution().getScaledWidth() / 2 + 20;
			int y = event.getResolution().getScaledHeight() / 2 - 8;
			GuiScreen.drawModalRectWithCustomSizedTexture(x, y, clientProfile.facing.ordinal() * 16, 32, 16, 16, 256, 256);
			
			if(clientProfile.half > -1)
				GuiScreen.drawModalRectWithCustomSizedTexture(x + 16, y, clientProfile.half * 16, 48, 16, 16, 256, 256);
			
			GlStateManager.popMatrix();
		}
	}
	
	public static void setProfile(EntityPlayer player, LockProfile profile) {
		String name = player.getName();
		
		if(profile == null)
			lockProfiles.remove(name);
		else {
			boolean locked = player.getEntityData().getBoolean(TAG_LOCKED_ONCE);
			if(!locked) {
				ITextComponent text = new TextComponentTranslation("quarkmisc.rotationLockBefore");
				ITextComponent keybind = new TextComponentKeybind("quark.keybind.lockBuilding");
				keybind.getStyle().setColor(TextFormatting.AQUA);
				text.appendSibling(keybind);
				text.appendSibling(new TextComponentTranslation("quarkmisc.rotationLockAfter"));
				player.sendMessage(text);
				
				player.getEntityData().setBoolean(TAG_LOCKED_ONCE, true);
			}
			
			lockProfiles.put(name, profile);
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
	
	public static class LockProfile {
		
		EnumFacing facing;
		int half;
		
		public LockProfile(EnumFacing facing, int half) {
			this.facing = facing;
			this.half = half;
		}
		
		public static LockProfile readProfile(ByteBuf buf) {
			boolean valid = buf.readBoolean();
			if(!valid)
				return null;
			
			int face = buf.readInt();
			int half = buf.readInt();
			return new LockProfile(EnumFacing.class.getEnumConstants()[face], half);
		}

		public static void writeProfile(LockProfile p, ByteBuf buf) {
			if(p == null)
				buf.writeBoolean(false);
			else {
				buf.writeBoolean(true);
				buf.writeInt(p.facing.ordinal());
				buf.writeInt(p.half);
			}
		}
		
		@Override
		public boolean equals(Object other) {
			if(other == this)
				return true;
			if(!(other instanceof LockProfile))
				return false;
				
			LockProfile otherProfile = (LockProfile) other;
			return otherProfile.facing == facing && otherProfile.half == half;
		}
		
	}
	
}
