package uz.ilhomvich.seamlessportals;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Client already moved through the portal (for seamlessness); server validates and mirrors it. */
public class PacketTeleport implements IMessage {
    int id;
    double x, y, z, mx, mz;
    float yaw, pitch;

    public PacketTeleport() {}

    public PacketTeleport(int id, double x, double y, double z, float yaw, float pitch, double mx, double mz) {
        this.id = id; this.x = x; this.y = y; this.z = z; this.yaw = yaw; this.pitch = pitch; this.mx = mx; this.mz = mz;
    }

    @Override
    public void fromBytes(ByteBuf b) {
        id = b.readInt(); x = b.readDouble(); y = b.readDouble(); z = b.readDouble();
        yaw = b.readFloat(); pitch = b.readFloat(); mx = b.readDouble(); mz = b.readDouble();
    }

    @Override
    public void toBytes(ByteBuf b) {
        b.writeInt(id); b.writeDouble(x); b.writeDouble(y); b.writeDouble(z);
        b.writeFloat(yaw); b.writeFloat(pitch); b.writeDouble(mx); b.writeDouble(mz);
    }

    public static class Handler implements IMessageHandler<PacketTeleport, IMessage> {
        @Override
        public IMessage onMessage(final PacketTeleport m, MessageContext ctx) {
            final EntityPlayerMP p = ctx.getServerHandler().player;
            p.getServerWorld().addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    Entity e = p.world.getEntityByID(m.id);
                    if (!(e instanceof EntityPortal)) return;
                    EntityPortal pt = (EntityPortal) e;
                    if (!pt.isLinked() || p.getDistanceSq(pt.posX, pt.posY, pt.posZ) > 100.0D) return;
                    Vec3d d = pt.destBase();
                    double dx = m.x - d.x, dz = m.z - d.z;
                    if (dx * dx + dz * dz > 9.0D || Math.abs(m.y - d.y) > 5.0D) return;
                    p.setLocationAndAngles(m.x, m.y, m.z, m.yaw, m.pitch);
                    p.rotationYawHead = m.yaw;
                    p.motionX = m.mx;
                    p.motionZ = m.mz;
                    p.fallDistance = 0.0F;
                    p.connection.captureCurrentPosition();
                }
            });
            return null;
        }
    }
}
