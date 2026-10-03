package com.frierenflight.zoltraakcinematic.client.renderer;

import com.frierenflight.zoltraakcinematic.ZoltraakCinematicMod;
import com.frierenflight.zoltraakcinematic.entity.GargantuaEntity;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🌌 GargantuaPostProcessor
 * Executes screen-space general relativistic geodesic raymarching for active Gargantua singularities.
 * Features:
 * - Thorne/Interstellar geodesic integration (d2u/dphi2 + u = 3Mu^2)
 * - Volumetric 3-octave rotating accretion disk with Doppler beaming
 * - Kali's starNest celestial background for lensed escape rays
 * - Full scene depth occlusion testing against terrain and blocks
 */
public final class GargantuaPostProcessor {
    private static final ResourceLocation NOISE_TEX =
            ResourceLocation.fromNamespaceAndPath(ZoltraakCinematicMod.MODID, "textures/effect/noise_tile.png");

    private static final Set<GargantuaEntity> ACTIVE_CLIENT_INSTANCES =
            Collections.newSetFromMap(new ConcurrentHashMap<>());

    private static TextureTarget sceneCopy;
    private static TextureTarget currentDepth;
    private static Matrix4f levelViewMatrix;

    public static void registerClientInstance(GargantuaEntity entity) {
        if (entity != null) {
            ACTIVE_CLIENT_INSTANCES.add(entity);
        }
    }

    public static void unregisterClientInstance(GargantuaEntity entity) {
        if (entity != null) {
            ACTIVE_CLIENT_INSTANCES.remove(entity);
        }
    }

    public static List<GargantuaEntity> getActiveClientEntities(Minecraft mc) {
        List<GargantuaEntity> active = new ArrayList<>();
        ACTIVE_CLIENT_INSTANCES.removeIf(e -> e.isRemoved() || !e.isAlive() || e.level() != mc.level);
        active.addAll(ACTIVE_CLIENT_INSTANCES);
        if (mc.level != null) {
            for (Entity e : mc.level.entitiesForRendering()) {
                if (e instanceof GargantuaEntity g && g.isAlive() && !active.contains(g)) {
                    active.add(g);
                }
            }
        }
        return active;
    }

    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (ShaderCompatibility.isShadowPass()) {
            return;
        }

        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_WEATHER) {
            if (event.getPoseStack() != null) {
                levelViewMatrix = new Matrix4f(event.getPoseStack().last().pose());
            }
            return;
        }

        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return;

            Camera camera = event.getCamera() != null ? event.getCamera() : (mc.gameRenderer != null ? mc.gameRenderer.getMainCamera() : null);
            Matrix4f view = event.getModelViewMatrix();
            if (view == null) {
                view = levelViewMatrix;
            }
            if (view == null && event.getPoseStack() != null) {
                view = event.getPoseStack().last().pose();
            }
            if (view == null && camera != null) {
                view = new Matrix4f().rotation(camera.rotation().conjugate());
            }
            if (view == null) {
                view = RenderSystem.getModelViewMatrix();
            }
            if (view == null) return;
            levelViewMatrix = null;

            List<GargantuaEntity> active = getActiveClientEntities(mc);

            if (!active.isEmpty()) {
                float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
                Matrix4f projection = event.getProjectionMatrix() != null ? event.getProjectionMatrix() : RenderSystem.getProjectionMatrix();
                renderGargantua(active, projection, view, camera, partial);
            }
        }
    }

    public static void renderGargantua(List<GargantuaEntity> entities, Matrix4f projection, Matrix4f view, Camera camera, float partialTicks) {
        if (entities == null || entities.isEmpty() || projection == null || view == null || camera == null) {
            return;
        }

        ShaderInstance shader = ZoltraakCinematicShaders.gargantua;
        if (shader == null || !RenderSystem.isOnRenderThread()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        RenderTarget main = mc.getMainRenderTarget();
        if (mc.level == null || main == null || main.width <= 0 || main.height <= 0) {
            return;
        }

        int prevReadFbo = GL30.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int prevDrawFbo = GL30.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        boolean prevDepthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean prevCull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        boolean prevBlend = GL11.glIsEnabled(GL11.GL_BLEND);

        try {
            ensureTargets(main.width, main.height);

            // Copy color buffer for scene sampling
            int readSourceFbo = (prevDrawFbo != 0 && ShaderCompatibility.useFullDetailPass()) ? prevDrawFbo : main.frameBufferId;
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readSourceFbo);
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, sceneCopy.frameBufferId);
            GlStateManager._glBlitFrameBuffer(0, 0, main.width, main.height, 0, 0, sceneCopy.width, sceneCopy.height, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);

            // Copy depth buffer for physical world occlusion testing (or use Iris depth texture directly)
            int irisDepthTex = ShaderCompatibility.getShaderDepthTexture();
            int depthSamplerId;
            if (irisDepthTex > 0) {
                depthSamplerId = irisDepthTex;
            } else {
                GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readSourceFbo);
                GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, currentDepth.frameBufferId);
                GlStateManager._glBlitFrameBuffer(0, 0, main.width, main.height, 0, 0, currentDepth.width, currentDepth.height, GL11.GL_DEPTH_BUFFER_BIT, GL11.GL_NEAREST);
                depthSamplerId = currentDepth.getDepthTextureId();
            }

            // Switch to main framebuffer for drawing
            main.bindWrite(true);
            RenderSystem.disableBlend();
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

            int noiseId = getNoiseTextureId(mc);

            Matrix4f invProj = new Matrix4f(projection).invert();
            Matrix4f viewRotation = new Matrix4f(view).setTranslation(0.0f, 0.0f, 0.0f);
            Vec3 camPos = camera.getPosition();

            for (GargantuaEntity entity : entities) {
                if (entity.isRemoved()) continue;

                float rg = entity.gravitationalRadius(partialTicks);
                float brightness = entity.brightness(partialTicks);
                if (rg < 0.01f || brightness <= 0.0f) continue;

                Vec3 center = entity.centre(partialTicks);
                Vector3f relPos = new Vector3f(
                        (float) (center.x - camPos.x),
                        (float) (center.y - camPos.y),
                        (float) (center.z - camPos.z)
                );
                Vector3f eyeCenter = viewRotation.transformPosition(relPos, new Vector3f());

                Vec3 axis = entity.spinAxis();
                Vector3f eyeAxis = viewRotation.transformDirection(
                        new Vector3f((float) axis.x, (float) axis.y, (float) axis.z),
                        new Vector3f()
                ).normalize();

                shader.setSampler("SceneSampler", sceneCopy.getColorTextureId());
                shader.setSampler("DepthSampler", depthSamplerId);
                shader.setSampler("NoiseSampler", noiseId);

                shader.safeGetUniform("HoleCentre").set(eyeCenter.x, eyeCenter.y, eyeCenter.z, rg);
                shader.safeGetUniform("SpinAxis").set(eyeAxis.x, eyeAxis.y, eyeAxis.z, 0.6f);

                float opened = entity.opened(partialTicks);
                float crit = entity.criticality(partialTicks);
                float timeSec = (entity.level().getGameTime() + partialTicks) / 20.0f;
                float blast = entity.blastFlash(partialTicks);
                shader.safeGetUniform("HoleState").set(opened, crit, timeSec, blast);

                shader.safeGetUniform("DiskShape").set(2.20f, 22.5f, 0.004f, brightness);
                shader.safeGetUniform("InverseProjectionMat").set(invProj);

                RenderSystem.setShader(() -> shader);
                drawFullscreenQuad();
            }
        } catch (Throwable t) {
            System.err.println("[ZoltraakCinematic] Gargantua post-processing error: " + t.getMessage());
        } finally {
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, prevReadFbo);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, prevDrawFbo);
            if (prevDepthTest) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
            if (prevCull) RenderSystem.enableCull(); else RenderSystem.disableCull();
            if (prevBlend) RenderSystem.enableBlend(); else RenderSystem.disableBlend();
            RenderSystem.depthMask(true);
            RenderSystem.defaultBlendFunc();
        }
    }

    private static void ensureTargets(int width, int height) {
        if (sceneCopy == null || sceneCopy.width != width || sceneCopy.height != height) {
            if (sceneCopy != null) sceneCopy.destroyBuffers();
            sceneCopy = new TextureTarget(width, height, false, Minecraft.ON_OSX);
            sceneCopy.setFilterMode(GL11.GL_LINEAR);
        }

        if (currentDepth == null || currentDepth.width != width || currentDepth.height != height) {
            if (currentDepth != null) currentDepth.destroyBuffers();
            currentDepth = new TextureTarget(width, height, true, Minecraft.ON_OSX);
            currentDepth.setFilterMode(GL11.GL_NEAREST);
        }
    }

    private static int getNoiseTextureId(Minecraft mc) {
        AbstractTexture tex = mc.getTextureManager().getTexture(NOISE_TEX);
        if (tex == null) {
            mc.getTextureManager().bindForSetup(NOISE_TEX);
            tex = mc.getTextureManager().getTexture(NOISE_TEX);
        }
        return tex != null ? tex.getId() : 0;
    }

    private static void drawFullscreenQuad() {
        ByteBufferBuilder byteBuffer = new ByteBufferBuilder(256);
        BufferBuilder builder = new BufferBuilder(byteBuffer, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        builder.addVertex(-1.0f, -1.0f, 0.0f).setUv(0.0f, 0.0f);
        builder.addVertex( 1.0f, -1.0f, 0.0f).setUv(1.0f, 0.0f);
        builder.addVertex( 1.0f,  1.0f, 0.0f).setUv(1.0f, 1.0f);
        builder.addVertex(-1.0f,  1.0f, 0.0f).setUv(0.0f, 1.0f);
        MeshData meshData = builder.build();
        if (meshData != null) {
            BufferUploader.drawWithShader(meshData);
        }
        byteBuffer.close();
    }

    public static void release() {
        ACTIVE_CLIENT_INSTANCES.clear();
        if (sceneCopy != null) {
            sceneCopy.destroyBuffers();
            sceneCopy = null;
        }
        if (currentDepth != null) {
            currentDepth.destroyBuffers();
            currentDepth = null;
        }
        levelViewMatrix = null;
    }

    private GargantuaPostProcessor() {}
}
