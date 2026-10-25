package uz.ilhomvich.seamlessportals;

import java.util.ArrayList;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

public class PortalWandItem extends Item {
    public PortalWandItem() {
        setRegistryName(new ResourceLocation(SeamlessPortals.MODID, "portal_wand"));
        setUnlocalizedName("portal_wand");
        setCreativeTab(CreativeTabs.MISC);
        setMaxStackSize(1);
    }

    private static void msg(EntityPlayer p, String s) {
        p.sendStatusMessage(new TextComponentString(s), true);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand,
                                      EnumFacing side, float hx, float hy, float hz) {
        if (world.isRemote) return EnumActionResult.SUCCESS;
        ItemStack st = player.getHeldItem(hand);
        BlockPos base = pos.offset(side);
        int idx = player.getHorizontalFacing().getOpposite().getHorizontalIndex();
        NBTTagCompound tag = st.getTagCompound();
        if (tag != null && tag.hasKey("pf")) {
            BlockPos first = new BlockPos(tag.getInteger("px"), tag.getInteger("py"), tag.getInteger("pz"));
            int fIdx = tag.getInteger("pf");
            if (first.equals(base)) {
                msg(player, "Boshqa joyni tanlang");
                return EnumActionResult.FAIL;
            }
            EntityPortal a = new EntityPortal(world);
            a.configure(first, fIdx, base, idx);
            EntityPortal b = new EntityPortal(world);
            b.configure(base, idx, first, fIdx);
            world.spawnEntity(a);
            world.spawnEntity(b);
            st.setTagCompound(null);
            msg(player, "Portal juftligi yaratildi");
        } else {
            NBTTagCompound t = new NBTTagCompound();
            t.setInteger("px", base.getX());
            t.setInteger("py", base.getY());
            t.setInteger("pz", base.getZ());
            t.setInteger("pf", idx);
            st.setTagCompound(t);
            msg(player, "1-portal belgilandi. Ikkinchisini tanlang");
        }
        return EnumActionResult.SUCCESS;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack st = player.getHeldItem(hand);
        if (!world.isRemote && player.isSneaking()) {
            st.setTagCompound(null);
            int n = 0;
            for (Entity e : new ArrayList<Entity>(world.loadedEntityList)) {
                if (!(e instanceof EntityPortal) || e.getDistanceSq(player) > 64.0D) continue;
                EntityPortal p = (EntityPortal) e;
                Vec3d d = p.destBase();
                for (Entity o : new ArrayList<Entity>(world.loadedEntityList)) {
                    if (o instanceof EntityPortal && o != p && o.getDistanceSq(d.x, d.y, d.z) < 0.25D) o.setDead();
                }
                p.setDead();
                n++;
            }
            msg(player, n + " ta portal o'chirildi");
        }
        return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, st);
    }
}
