/**
 * This class was created by <FredTargaryen>. It's distributed as
 * part of the Quark Mod. Get the Source Code in github:
 * https://github.com/Vazkii/Quark
 *
 * Quark is Open Source and distributed under the
 * CC-BY-NC-SA 3.0 License: https://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB
 *
 * File Created @ [06/07/2018, 12:46:46 (GMT)]
 */
package vazkii.quark.client.feature;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRailBase;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderMinecart;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.minecart.MinecartUpdateEvent;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import vazkii.quark.base.lib.LibMisc;
import vazkii.quark.base.module.Feature;

import javax.annotation.Nullable;
import java.util.concurrent.Callable;

public class TiltingMinecarts extends Feature {

    // OVERRIDES FROM FEATURE CLASS
    @Override
    @SideOnly(Side.CLIENT)
    public void preInitClient(FMLPreInitializationEvent event) {
        //Capability
        CapabilityManager.INSTANCE.register(IMinecartTiltCap.class, new MinecartTiltStorage(), new DefaultTiltImplFactory());
        MinecraftForge.EVENT_BUS.register(this);
        RenderingRegistry.registerEntityRenderingHandler(EntityMinecart.class, new RenderMinecartFactory(EntityMinecart.class));
    }

    @Override
    public boolean hasSubscriptions() {
        return true;
    }

    @Override
    public String getFeatureDescription() {
        return "When turning, minecarts (and the players riding them) will tilt proportionally to the speed of the minecart.";
    }

    @Override
    public String getFeatureIngameConfigName() {
        return "Tilting Minecarts";
    }

    @Override
    public boolean requiresMinecraftRestartToEnable() {
        return true;
    }

    // DEFINE THE TILTING CAPABILITY
    // Useful constants

    /**
     * What direction of turn the minecart will execute
     */
    private enum EnumTurn {
        NONE, LEFT, RIGHT
    }

    /**
     * Apparently 8 metres per second, which is 8/20=0.4 blocks per tick
     */
    private static final double MAX_CART_SPEED = 0.4;

    /**
     * 60 degrees or Math.PI / 3 radians
     * TODO May need to be radians. Depends on render code
     */
    private static final double MAX_PEAK_TILT = 60.0;

    /**
     * Length of a tilt (time spent with one side in the air), in ticks
     */
    private static final byte TILT_LENGTH_TICKS = 20;

    public static final ResourceLocation TILT_CAP_LOCATION = new ResourceLocation(LibMisc.MOD_ID, "IMinecartTiltCap");

    /**
     * Functions required for a minecart (even modded entities extending EntityMinecart) to tilt
     */
    public interface IMinecartTiltCap {
        double getTiltAmount(float partialTicks);
        double getPeakTiltAmount();
        byte getTiltingTickCount();
        void setTiltAmount(double amount);
        void setPeakTiltAmount(double amount);
        void setTiltingTickAmount(byte amount);
        void updateTiltAmount(EntityMinecart em);
    }

    /**
     * NBT storage for the tilt amounts
     */
    public class MinecartTiltStorage implements Capability.IStorage<IMinecartTiltCap> {

        @Nullable
        @Override
        public NBTBase writeNBT(Capability<IMinecartTiltCap> capability, IMinecartTiltCap instance, EnumFacing side) {
            NBTTagCompound nbt = new NBTTagCompound();
            nbt.setDouble("tilt", instance.getTiltAmount(0.0F));
            nbt.setDouble("peaktilt", instance.getPeakTiltAmount());
            nbt.setByte("ticks", instance.getTiltingTickCount());
            return nbt;
        }

        @Override
        public void readNBT(Capability<IMinecartTiltCap> capability, IMinecartTiltCap instance, EnumFacing side, NBTBase nbt) {
            NBTTagCompound comp = (NBTTagCompound) nbt;
            instance.setTiltAmount(comp.getDouble("tilt"));
            instance.setPeakTiltAmount(comp.getDouble("peaktilt"));
            instance.setTiltingTickAmount(comp.getByte("ticks"));
        }
    }

    /**
     * Returns the default implementation of IMinecartTiltCap, which should be enough for modded carts too
     */
    public class DefaultTiltImplFactory implements Callable<IMinecartTiltCap> {
        public IMinecartTiltCap call() {
            return new DefaultTiltImpl();
        }

        private class DefaultTiltImpl implements IMinecartTiltCap {
            private double tilt;
            private double prevTilt;
            private double peakTilt;
            private byte tickCount;

            @Override
            public double getTiltAmount(float partialTicks) {
                return this.prevTilt + (this.tilt - this.prevTilt) * partialTicks;
            }

            @Override
            public double getPeakTiltAmount() {
                return this.peakTilt;
            }

            @Override
            public byte getTiltingTickCount() {
                return this.tickCount;
            }

            @Override
            public void setTiltAmount(double amount) {
                this.tilt = amount;
            }

            @Override
            public void setPeakTiltAmount(double amount) {
                this.peakTilt = amount;
            }

            @Override
            public void setTiltingTickAmount(byte amount) {
                this.tickCount = amount;
            }

            @Override
            public void updateTiltAmount(EntityMinecart em) {
                this.prevTilt = this.tilt;
                if (this.tickCount == 0) {
                    // Cart not tilting; check if moving
                    EnumFacing travelDirection = null;
                    if (em.motionX > 0.0) travelDirection = EnumFacing.EAST;
                    else if (em.motionX < 0.0) travelDirection = EnumFacing.WEST;
                    else if (em.motionZ > 0.0) travelDirection = EnumFacing.NORTH;
                    else if (em.motionZ < 0.0) travelDirection = EnumFacing.SOUTH;
                    if (travelDirection != null) {
                        //Cart is moving; check if it is on a block that will turn it
                        EnumTurn turn = EnumTurn.NONE;
                        BlockPos minecartPos = new BlockPos(em.posX, em.posY, em.posZ);
                        IBlockState below = em.world.getBlockState(minecartPos);
                        Block belowBlock = below.getBlock();
                        if (BlockRailBase.isRailBlock(below)) {
                            BlockRailBase.EnumRailDirection erd = ((BlockRailBase) belowBlock).getRailDirection(em.world, minecartPos, below, em);
                            // TODO Check for a straight block next to the turn
                            switch (erd) {
                                case NORTH_EAST:
                                    if(travelDirection == EnumFacing.SOUTH) turn = EnumTurn.LEFT;
                                    else if(travelDirection == EnumFacing.WEST) turn = EnumTurn.RIGHT;
                                    break;
                                case SOUTH_EAST:
                                    if(travelDirection == EnumFacing.NORTH) turn = EnumTurn.RIGHT;
                                    else if(travelDirection == EnumFacing.WEST) turn = EnumTurn.LEFT;
                                    break;
                                case NORTH_WEST:
                                    if(travelDirection == EnumFacing.SOUTH) turn = EnumTurn.RIGHT;
                                    else if(travelDirection == EnumFacing.EAST) turn = EnumTurn.LEFT;
                                    break;
                                case SOUTH_WEST:
                                    if(travelDirection == EnumFacing.NORTH) turn = EnumTurn.LEFT;
                                    else if(travelDirection == EnumFacing.EAST) turn = EnumTurn.RIGHT;
                                    break;
                                default:
                                    break;
                            }
                            if(turn != EnumTurn.NONE) {
                                // Minecart will definitely turn; start a new tilt
                                // Get cart speed. Assuming on flat ground; ignore Y motion
                                double speed = Math.sqrt(em.motionX * em.motionX + em.motionZ + em.motionZ);
                                double peakTilt = (speed / MAX_CART_SPEED) * MAX_PEAK_TILT;
                                // TODO may need to be RIGHT
                                if(turn == EnumTurn.LEFT) peakTilt *= -1;
                                this.peakTilt = peakTilt;
                                this.tickCount += 1;
                            }
                        }
                    }
                } else {
                    //Manage tilt amount based on tick count
                    this.tilt = this.peakTilt * Math.sin((this.tickCount / (double) TILT_LENGTH_TICKS) * Math.PI);
                    if(this.tickCount >= TILT_LENGTH_TICKS) {
                        this.tickCount = 0;
                    } else {
                        this.tickCount += 1;
                    }
                }
            }
        }
    }

    // CODE TO ADD THE CAPABILITY TO NEW ENTITIES
    @CapabilityInject(IMinecartTiltCap.class)
    public static final Capability<IMinecartTiltCap> TILTCAP = null;

    @SubscribeEvent
    public void onEntityConstruct(AttachCapabilitiesEvent<Entity> evt) {
        if(evt.getObject() instanceof EntityMinecart) {
            evt.addCapability(TILT_CAP_LOCATION,
                    //Full name ICapabilitySerializableProvider
                    new ICapabilitySerializable<NBTTagCompound>() {
                        IMinecartTiltCap inst = TILTCAP.getDefaultInstance();

                        @Override
                        public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
                            return capability == TILTCAP;
                        }

                        @Override
                        public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
                            return capability == TILTCAP ? TILTCAP.<T>cast(inst) : null;
                        }

                        @Override
                        public NBTTagCompound serializeNBT() {
                            return (NBTTagCompound) TILTCAP.getStorage().writeNBT(TILTCAP, inst, null);
                        }

                        @Override
                        public void deserializeNBT(NBTTagCompound nbt) {
                            TILTCAP.getStorage().readNBT(TILTCAP, inst, null, nbt);
                        }
                    }
            );
        }
    }

    // EVENT HANDLERS TO UPDATE MINECARTS

    /**
     * Updates the tilt capability for the rendering event handlers
     */
    @SubscribeEvent
    public void onMinecartUpdate(MinecartUpdateEvent mue) {
        EntityMinecart em = mue.getMinecart();
        if(em.hasCapability(TILTCAP, null)) {
            em.getCapability(TILTCAP, null).updateTiltAmount(em);
        }
    }

    /**
     * Tilts the model of a player riding a minecart
     */
    @SubscribeEvent
    public void onPossibleRiderRender(RenderPlayerEvent.Pre rpe) {
        EntityPlayer player = rpe.getEntityPlayer();
        if(player.isRiding()) {
            Entity entityBeingRidden = player.getLowestRidingEntity();
            if(entityBeingRidden.hasCapability(TILTCAP, null)) {
                GL11.glRotated(entityBeingRidden.getCapability(TILTCAP, null)
                        .getTiltAmount(rpe.getPartialRenderTick()), 0.0, 0.0, 1.0);
            }
        }
    }

    /**
     * Tilts the minecart model. Replaces the entityRenderMap entry for the entity with an instance of this, wrapping it
     */
    public class RenderMinecartFactory<T extends Entity> implements IRenderFactory<T> {

        private Class<T> clarse;

        public RenderMinecartFactory(Class<T> clarse) {
            this.clarse = clarse;
        }

        @Override
        public Render<? super T> createRenderFor(RenderManager manager) {
            return new RenderTiltingMinecart(manager, manager.entityRenderMap.get(this.clarse));
        }

        private class RenderTiltingMinecart extends Render<? super T> {
            private Render<T> renderer;

            public RenderTiltingMinecart(RenderManager rm, Render<? extends Entity> renderer) {
                super(rm);
                this.renderer = renderer;
            }

            /**
             * Renders the desired {@code T} type Entity.
             */
            public void doRender(T entity, double x, double y, double z, float entityYaw, float partialTicks) {
                GlStateManager.pushMatrix();
                GL11.glRotated(entity.getCapability(TILTCAP, null).getTiltAmount(partialTicks), 0.0, 0.0, 1.0);
                this.renderer.doRender(entity, x, y, z, entityYaw, partialTicks);
                GlStateManager.popMatrix();
            }

            @Nullable
            @Override
            protected ResourceLocation getEntityTexture(Entity entity) {
                returnthis.renderer.get;
            }
        }

    }

}
