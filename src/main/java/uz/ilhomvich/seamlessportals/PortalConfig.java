package uz.ilhomvich.seamlessportals;

import net.minecraftforge.common.config.Config;

@Config(modid = SeamlessPortals.MODID, name = "seamlessportals")
public class PortalConfig {
    @Config.Comment("Bir vaqtda nechta portal ichini chizish (har biri qo'shimcha dunyo render qiladi)")
    @Config.RangeInt(min = 1, max = 4)
    public static int maxViews = 2;

    @Config.Comment("Portal tasviri o'lchami (ekranga nisbatan). Kichik = tezroq")
    @Config.RangeDouble(min = 0.25, max = 1.0)
    public static double resolutionScale = 0.5;

    @Config.Comment("Portal ichidagi dunyo render masofasi (chunk). Kichik = kamroq RAM/CPU")
    @Config.RangeInt(min = 2, max = 16)
    public static int portalRenderDistance = 5;

    @Config.Comment("Shu masofadan uzoqdagi portallar ichi chizilmaydi (blok)")
    @Config.RangeDouble(min = 8, max = 128)
    public static double maxDistance = 48;
}
