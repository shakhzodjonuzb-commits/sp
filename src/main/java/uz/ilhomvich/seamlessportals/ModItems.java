package uz.ilhomvich.seamlessportals;

import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@Mod.EventBusSubscriber(modid = SeamlessPortals.MODID)
public class ModItems {
    public static final Item WAND = new PortalWandItem();

    @SubscribeEvent
    public static void items(RegistryEvent.Register<Item> e) {
        e.getRegistry().register(WAND);
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void models(ModelRegistryEvent e) {
        ModelLoader.setCustomModelResourceLocation(WAND, 0, new ModelResourceLocation(WAND.getRegistryName(), "inventory"));
    }
}
