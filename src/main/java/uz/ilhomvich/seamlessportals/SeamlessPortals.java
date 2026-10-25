package uz.ilhomvich.seamlessportals;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

@Mod(modid = SeamlessPortals.MODID, name = "Seamless Portals", version = "1.0.0", acceptedMinecraftVersions = "[1.12.2]")
public class SeamlessPortals {
    public static final String MODID = "seamlessportals";

    @Mod.Instance(MODID)
    public static SeamlessPortals instance;

    @SidedProxy(clientSide = "uz.ilhomvich.seamlessportals.ClientProxy", serverSide = "uz.ilhomvich.seamlessportals.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent e) {
        proxy.preInit();
    }
}
