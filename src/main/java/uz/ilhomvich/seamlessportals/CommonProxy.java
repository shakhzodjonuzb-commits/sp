package uz.ilhomvich.seamlessportals;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.EntityRegistry;

public class CommonProxy {
    public void preInit() {
        EntityRegistry.registerModEntity(new ResourceLocation(SeamlessPortals.MODID, "portal"),
                EntityPortal.class, "portal", 0, SeamlessPortals.instance, 256, 20, false);
        Net.init();
    }
}
