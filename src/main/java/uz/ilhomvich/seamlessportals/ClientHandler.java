package uz.ilhomvich.seamlessportals;

import java.nio.DoubleBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Seamless portals, client side.
 *  1) RenderTick START: for the nearest portals render the world from the "virtual camera"
 *     (player's camera mapped through the portal pair) into an FBO, using a separate RenderGlobal per view.
 *  2) RenderWorldLast: stencil-mask the portal rectangle and paste the FBO screen-aligned (1:1) there.
 *  3) PlayerTick: when the eye crosses the portal plane the player is moved to the partner at once
 *     (same frame => no visible cut) and the server is told.
 */
@SideOnly(Side.CLIENT)
public class ClientHandler {
    private static final int MAX = 4;

    private final Minecraft mc = Minecraft.getMinecraft();
    private final Framebuffer[] fbos = new Framebuffer[MAX];
    private final RenderGlobal[] rgs = new RenderGlobal[MAX];
    private final WorldClient[] rgWorlds = new WorldClient[MAX];
    private final int[] rgRd = new int[MAX];
    private final List<EntityPortal> active = new ArrayList<EntityPortal>();
    private CameraEntity cam;

    private boolean rendering;
    private boolean clipOn;
    private final double[] clip = new double[4];
    private final DoubleBuffer clipBuf = BufferUtils.createDoubleBuffer(4);

    // ------------------------------------------------------------------ 1) portal views
    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent e) {
        if (e.phase != TickEvent.Phase.START || rendering) return;
        active.clear();
        if (mc.world == null || mc.player == null || mc.skipRenderWorld) return;
        if (mc.gameSettings.thirdPersonView != 0 || !OpenGlHelper.isFramebufferEnabled()) return;

        Framebuffer main = mc.getFramebuffer();
        if (!main.isStencilEnabled()) main.enableStencil();

        float pt = e.renderTickTime;
        EntityPlayerSP p = mc.player;
        final Vec3d eye = new Vec3d(
                p.lastTickPosX + (p.posX - p.lastTickPosX) * pt,
                p.lastTickPosY + (p.posY - p.lastTickPosY) * pt + p.getEyeHeight(),
                p.lastTickPosZ + (p.posZ - p.lastTickPosZ) * pt);
        Vec3d look = p.getLook(pt);
        float yaw = p.prevRotationYaw + (p.rotationYaw - p.prevRotationYaw) * pt;
        float pitch = p.prevRotationPitch + (p.rotationPitch - p.prevRotationPitch) * pt;

        List<EntityPortal> cands = new ArrayList<EntityPortal>();
        for (Entity en : mc.world.loadedEntityList) {
            if (!(en instanceof EntityPortal)) continue;
            EntityPortal c = (EntityPortal) en;
            if (!c.isLinked()) continue;
            Vec3d d = center(c).subtract(eye);
            double dist = d.lengthVector();
            if (dist > PortalConfig.maxDistance) continue;
            if (dist > 4.0D && d.dotProduct(look) / dist < 0.25D) continue;
            cands.add(c);
        }
        Collections.sort(cands, new Comparator<EntityPortal>() {
            @Override
            public int compare(EntityPortal a, EntityPortal b) {
                return Double.compare(center(a).squareDistanceTo(eye), center(b).squareDistanceTo(eye));
            }
        });
        int n = Math.min(cands.size(), Math.min(PortalConfig.maxViews, MAX));
        for (int i = 0; i < n; i++) {
            EntityPortal c = cands.get(i);
            renderView(i, c, eye, yaw, pitch, pt);
            active.add(c);
        }
    }

    private static Vec3d center(EntityPortal c) {
        return new Vec3d(c.posX, c.posY + PortalMath.H / 2, c.posZ);
    }

    private CameraEntity camera() {
        if (cam == null || cam.world != mc.world) cam = new CameraEntity(mc.world);
        return cam;
    }

    private int portalRd() {
        return Math.max(2, Math.min(PortalConfig.portalRenderDistance, mc.gameSettings.renderDistanceChunks));
    }

    private RenderGlobal renderGlobal(int slot, CameraEntity c) {
        int rd = portalRd();
        if (rgs[slot] != null && rgWorlds[slot] == mc.world && rgRd[slot] == rd) return rgs[slot];
        if (rgs[slot] != null) rgs[slot].setWorldAndLoadRenderers(null);
        GameSettings gs = mc.gameSettings;
        int oldRd = gs.renderDistanceChunks;
        Entity oldView = mc.getRenderViewEntity();
        gs.renderDistanceChunks = rd;
        mc.setRenderViewEntity(c);
        RenderGlobal r = new RenderGlobal(mc);
        r.setWorldAndLoadRenderers(mc.world);
        mc.setRenderViewEntity(oldView);
        gs.renderDistanceChunks = oldRd;
        rgs[slot] = r;
        rgWorlds[slot] = mc.world;
        rgRd[slot] = rd;
        return r;
    }

    private void renderView(int slot, EntityPortal portal, Vec3d eye, float yaw, float pitch, float pt) {
        Vec3d a = portal.base();
        Vec3d b = portal.destBase();
        int q = PortalMath.quarter(portal.facing(), portal.destFacing());
        Vec3d e2 = PortalMath.transform(eye, a, b, q);

        CameraEntity c = camera();
        c.eye = mc.player.getEyeHeight();
        c.pose(e2.x, e2.y - c.eye, e2.z, yaw + q * 90.0F, pitch);

        // keep only geometry on the exit side of the destination portal (hides stuff between camera and portal)
        double side = PortalMath.normal(portal.facing()).dotProduct(eye.subtract(a)) >= 0 ? 1.0D : -1.0D;
        Vec3d nb = PortalMath.normal(portal.destFacing());
        clip[0] = nb.x * side;
        clip[1] = 0.0D;
        clip[2] = nb.z * side;
        clip[3] = clip[0] * (c.posX - b.x) + clip[2] * (c.posZ - b.z);

        int w = Math.max(16, (int) (mc.displayWidth * PortalConfig.resolutionScale));
        int h = Math.max(16, (int) (mc.displayHeight * PortalConfig.resolutionScale));
        Framebuffer fb = fbos[slot];
        if (fb == null) {
            fb = new Framebuffer(w, h, true);
            fbos[slot] = fb;
            fb.setFramebufferFilter(GL11.GL_LINEAR);
        } else if (fb.framebufferWidth != w || fb.framebufferHeight != h) {
            fb.createBindFramebuffer(w, h);
            fb.setFramebufferFilter(GL11.GL_LINEAR);
        }

        RenderGlobal rg = renderGlobal(slot, c);

        Entity oldView = mc.getRenderViewEntity();
        RenderGlobal oldRg = mc.renderGlobal;
        int oldRd = mc.gameSettings.renderDistanceChunks;
        RayTraceResult oldMouse = mc.objectMouseOver;
        Entity oldPointed = mc.pointedEntity;

        mc.renderGlobal = rg;
        mc.gameSettings.renderDistanceChunks = rgRd[slot];
        mc.setRenderViewEntity(c);
        rendering = true;
        clipOn = true;
        fb.bindFramebuffer(true);
        try {
            mc.entityRenderer.renderWorld(pt, System.nanoTime() + 4000000L);
        } finally {
            rendering = false;
            clipOn = false;
            GL11.glDisable(GL11.GL_CLIP_PLANE0);
            mc.getFramebuffer().bindFramebuffer(true);
            mc.setRenderViewEntity(oldView);
            mc.renderGlobal = oldRg;
            mc.gameSettings.renderDistanceChunks = oldRd;
            mc.objectMouseOver = oldMouse;
            mc.pointedEntity = oldPointed;
        }
    }

    /** Applies the clip plane right before terrain is drawn in the portal pass (modelview = camera transform). */
    @SubscribeEvent
    public void onFog(EntityViewRenderEvent.RenderFogEvent e) {
        if (!clipOn || e.getFogMode() != 0) return;
        clipBuf.clear();
        clipBuf.put(clip);
        clipBuf.flip();
        GL11.glClipPlane(GL11.GL_CLIP_PLANE0, clipBuf);
        GL11.glEnable(GL11.GL_CLIP_PLANE0);
    }

    @SubscribeEvent
    public void onRenderPlayer(RenderPlayerEvent.Pre e) {
        if (rendering && e.getEntityPlayer() == mc.player) e.setCanceled(true);
    }

    // ------------------------------------------------------------------ 2) paste into the main view
    @SubscribeEvent
    public void onRenderLast(RenderWorldLastEvent e) {
        if (rendering || active.isEmpty()) return;
        for (int i = 0; i < active.size(); i++) {
            if (fbos[i] != null) drawPortal(active.get(i), fbos[i]);
        }
    }

    private void drawPortal(EntityPortal p, Framebuffer fb) {
        RenderManager rm = mc.getRenderManager();
        Vec3d t = PortalMath.tangent(p.facing());
        double hw = PortalMath.W / 2.0D, h = PortalMath.H;
        double bx = p.posX - rm.renderPosX, by = p.posY - rm.renderPosY, bz = p.posZ - rm.renderPosZ;

        mc.entityRenderer.disableLightmap();
        GlStateManager.disableLighting();
        GlStateManager.disableFog();
        GlStateManager.disableAlpha();
        GlStateManager.disableBlend();
        GlStateManager.disableCull();
        GlStateManager.disableTexture2D();

        // pass 1: mark the portal rectangle in the stencil buffer (where it is not occluded)
        GL11.glEnable(GL11.GL_STENCIL_TEST);
        GL11.glStencilMask(0xFF);
        GL11.glClearStencil(0);
        GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);
        GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0xFF);
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);
        GlStateManager.colorMask(false, false, false, false);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex3d(bx - t.x * hw, by, bz - t.z * hw);
        GL11.glVertex3d(bx + t.x * hw, by, bz + t.z * hw);
        GL11.glVertex3d(bx + t.x * hw, by + h, bz + t.z * hw);
        GL11.glVertex3d(bx - t.x * hw, by + h, bz - t.z * hw);
        GL11.glEnd();
        GlStateManager.colorMask(true, true, true, true);

        // pass 2: screen-aligned quad with the portal view, only inside the stencil mask
        GL11.glStencilFunc(GL11.GL_EQUAL, 1, 0xFF);
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
        GL11.glStencilMask(0);
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GlStateManager.enableTexture2D();
        GlStateManager.bindTexture(fb.framebufferTexture);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0.0F, 0.0F); GL11.glVertex2f(-1.0F, -1.0F);
        GL11.glTexCoord2f(1.0F, 0.0F); GL11.glVertex2f(1.0F, -1.0F);
        GL11.glTexCoord2f(1.0F, 1.0F); GL11.glVertex2f(1.0F, 1.0F);
        GL11.glTexCoord2f(0.0F, 1.0F); GL11.glVertex2f(-1.0F, 1.0F);
        GL11.glEnd();
        GlStateManager.popMatrix();
        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.popMatrix();
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);

        GL11.glStencilMask(0xFF);
        GL11.glDisable(GL11.GL_STENCIL_TEST);
        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
        GlStateManager.enableCull();
        GlStateManager.enableAlpha();
        GlStateManager.enableFog();
        GlStateManager.disableBlend();
    }

    // ------------------------------------------------------------------ 3) walking through
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || mc.world == null || e.player != mc.player) return;
        EntityPlayer p = e.player;
        double eh = p.getEyeHeight();
        Vec3d a = new Vec3d(p.prevPosX, p.prevPosY + eh, p.prevPosZ);
        Vec3d b = new Vec3d(p.posX, p.posY + eh, p.posZ);
        if (a.squareDistanceTo(b) < 1.0E-10D) return;

        for (Entity en : new ArrayList<Entity>(mc.world.loadedEntityList)) {
            if (!(en instanceof EntityPortal)) continue;
            EntityPortal pt = (EntityPortal) en;
            if (!pt.isLinked() || !PortalMath.crosses(pt.base(), pt.facing(), a, b)) continue;

            int q = PortalMath.quarter(pt.facing(), pt.destFacing());
            float dyaw = q * 90.0F;
            Vec3d nb = PortalMath.transform(b, pt.base(), pt.destBase(), q);
            double[] m = PortalMath.rot(p.motionX, p.motionZ, q);

            p.setLocationAndAngles(nb.x, nb.y - eh, nb.z, p.rotationYaw + dyaw, p.rotationPitch);
            p.motionX = m[0];
            p.motionZ = m[1];
            p.rotationYawHead += dyaw;
            p.prevRotationYawHead = p.rotationYawHead;
            p.renderYawOffset += dyaw;
            p.prevRenderYawOffset = p.renderYawOffset;
            Net.CH.sendToServer(new PacketTeleport(pt.getEntityId(), p.posX, p.posY, p.posZ,
                    p.rotationYaw, p.rotationPitch, p.motionX, p.motionZ));
            break;
        }
    }
}
