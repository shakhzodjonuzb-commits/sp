package uz.ilhomvich.seamlessportals;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ClientProxy extends CommonProxy {
    @Override
    public void preInit() {
        super.preInit();
        RenderingRegistry.registerEntityRenderingHandler(EntityPortal.class, RenderPortalNoop::new);
        MinecraftForge.EVENT_BUS.register(new ClientHandler());
    }
}
