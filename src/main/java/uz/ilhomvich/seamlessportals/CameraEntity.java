package uz.ilhomvich.seamlessportals;

import java.util.Collections;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

/** Invisible viewpoint used to render the world seen through a portal. Never added to the world. */
public class CameraEntity extends EntityLivingBase {
    public float eye = 1.62F;

    public CameraEntity(World w) { super(w); }

    @Override
    public float getEyeHeight() { return eye; }

    public void pose(double x, double y, double z, float yaw, float pitch) {
        posX = prevPosX = lastTickPosX = x;
        posY = prevPosY = lastTickPosY = y;
        posZ = prevPosZ = lastTickPosZ = z;
        rotationYaw = prevRotationYaw = rotationYawHead = prevRotationYawHead = yaw;
        rotationPitch = prevRotationPitch = pitch;
        chunkCoordX = MathHelper.floor(x / 16.0D);
        chunkCoordY = MathHelper.floor(y / 16.0D);
        chunkCoordZ = MathHelper.floor(z / 16.0D);
    }

    @Override
    public Iterable<ItemStack> getArmorInventoryList() { return Collections.<ItemStack>emptyList(); }

    @Override
    public ItemStack getItemStackFromSlot(EntityEquipmentSlot slot) { return ItemStack.EMPTY; }

    @Override
    public void setItemStackToSlot(EntityEquipmentSlot slot, ItemStack stack) {}

    @Override
    public EnumHandSide getPrimaryHand() { return EnumHandSide.RIGHT; }
}
