package vazkii.quark.base.network.message;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import vazkii.arl.network.NetworkMessage;
import vazkii.quark.client.feature.TiltingMinecarts;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class MessageStartTilt extends NetworkMessage {

    public long UUIDMostSig;
    public long UUIDLeastSig;
    public double peakTilt;

    public MessageStartTilt() { }

	public MessageStartTilt(UUID minecartID, double peakTilt) {
        this.UUIDMostSig = minecartID.getMostSignificantBits();
        this.UUIDLeastSig = minecartID.getLeastSignificantBits();
        this.peakTilt = peakTilt;
    }

    @Override
    public IMessage handleMessage(MessageContext context) {
        Minecraft.getMinecraft().addScheduledTask(new Runnable() {
            @Override
            public void run() {
                UUID minecartID = new UUID(UUIDMostSig, UUIDLeastSig);
                List<Entity> l = Minecraft.getMinecraft().world.getLoadedEntityList();
                Iterator<Entity> cartFinder = l.iterator();
                Entity e;
                while(cartFinder.hasNext()) {
                    e = cartFinder.next();
                    if(e.getPersistentID().equals(minecartID)) {
                        if(e.hasCapability(TiltingMinecarts.TILTCAP, null)) {
                            //Can assume e is a minecart
                            e.getCapability(TiltingMinecarts.TILTCAP, null).setPeakTiltAmount(peakTilt);
                        }
                    }
                }
            }
        });
        return null;
    }
}
