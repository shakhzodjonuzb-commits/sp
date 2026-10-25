package uz.ilhomvich.seamlessportals;

import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public class Net {
    public static SimpleNetworkWrapper CH;

    public static void init() {
        CH = NetworkRegistry.INSTANCE.newSimpleChannel(SeamlessPortals.MODID);
        CH.registerMessage(PacketTeleport.Handler.class, PacketTeleport.class, 0, Side.SERVER);
    }
}
