package uz.ilhomvich.seamlessportals;

import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** The portal surface is drawn by ClientHandler; the entity renderer itself draws nothing. */
@SideOnly(Side.CLIENT)
public class RenderPortalNoop extends Render<EntityPortal> {
    public RenderPortalNoop(RenderManager m) { super(m); }

    @Override
    public void doRender(EntityPortal e, double x, double y, double z, float yaw, float pt) {}

    @Override
    protected ResourceLocation getEntityTexture(EntityPortal e) { return null; }
}
