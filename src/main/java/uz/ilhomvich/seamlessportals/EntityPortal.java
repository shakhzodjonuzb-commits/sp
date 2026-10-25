package uz.ilhomvich.seamlessportals;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/** A portal plane (2x3, vertical) standing in free air. Linked to a partner by synced destination data. */
public class EntityPortal extends Entity {
    private static final DataParameter<Integer> FACING = EntityDataManager.createKey(EntityPortal.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> D_FACING = EntityDataManager.createKey(EntityPortal.class, DataSerializers.VARINT);
    private static final DataParameter<BlockPos> D_POS = EntityDataManager.createKey(EntityPortal.class, DataSerializers.BLOCK_POS);
    private static final DataParameter<Boolean> LINKED = EntityDataManager.createKey(EntityPortal.class, DataSerializers.BOOLEAN);

    public EntityPortal(World w) {
        super(w);
        setSize(0.2F, 0.2F);
        noClip = true;
        ignoreFrustumCheck = true;
    }

    @Override
    protected void entityInit() {
        dataManager.register(FACING, 0);
        dataManager.register(D_FACING, 0);
        dataManager.register(D_POS, BlockPos.ORIGIN);
        dataManager.register(LINKED, Boolean.FALSE);
    }

    public void configure(BlockPos base, int facing, BlockPos destBase, int destFacing) {
        setPosition(base.getX() + 0.5D, base.getY(), base.getZ() + 0.5D);
        dataManager.set(FACING, facing & 3);
        dataManager.set(D_FACING, destFacing & 3);
        dataManager.set(D_POS, destBase);
        dataManager.set(LINKED, Boolean.TRUE);
    }

    public int facing() { return dataManager.get(FACING); }
    public int destFacing() { return dataManager.get(D_FACING); }
    public boolean isLinked() { return dataManager.get(LINKED); }
    public Vec3d base() { return new Vec3d(posX, posY, posZ); }

    public Vec3d destBase() {
        BlockPos d = dataManager.get(D_POS);
        return new Vec3d(d.getX() + 0.5D, d.getY(), d.getZ() + 0.5D);
    }

    @Override
    public void onUpdate() {
        // static object: nothing to simulate
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound c) {
        dataManager.set(FACING, c.getInteger("f"));
        dataManager.set(D_FACING, c.getInteger("df"));
        dataManager.set(D_POS, new BlockPos(c.getInteger("dx"), c.getInteger("dy"), c.getInteger("dz")));
        dataManager.set(LINKED, c.getBoolean("l"));
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound c) {
        c.setInteger("f", facing());
        c.setInteger("df", destFacing());
        BlockPos d = dataManager.get(D_POS);
        c.setInteger("dx", d.getX());
        c.setInteger("dy", d.getY());
        c.setInteger("dz", d.getZ());
        c.setBoolean("l", isLinked());
    }
}
