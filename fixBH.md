จากการตรวจสอบโค้ดทั้งหมดในระบบเรนเดอร์ของ **Gargantua** พบว่าโครงสร้างหลักออกแบบมาได้ดีมาก โดยใช้การทำ Post-processing Raymarching คำนวณเส้นทางแสงตามทฤษฎีสัมพัทธภาพทั่วไป (Geodesic Integration) ร่วมกับการอ่านค่า Depth Buffer เพื่อเช็คการบดบังของบล็อกในโลกเกม อย่างไรก็ตาม มี **บั๊กสำคัญ (Critical Bugs)** และจุดที่ทำให้การแสดงผลผิดพลาดหรือเกิดอาการหน้าจอดำ/ค้างได้ ดังรายละเอียดต่อไปนี้ครับ

---

### 1. สรุปบั๊กและปัญหาที่พบ (แยกตามไฟล์)

#### A. `GargantuaPostProcessor.java`

* **บั๊กหน้าจอดำหรือภาพค้างเมื่อข้ามการวาด Entity ทั้งหมด (Critical)**: ในเมธอด `renderGargantua` โค้ดทำการผูก `postTarget` แล้ววนลูปเช็คเงื่อนไข `if (entity.isRemoved()) continue;` และ `if (rg < 0.01f || brightness <= 0.0f) continue;` หากทุก Entity ในลูปถูก `continue` ข้ามไปทั้งหมด (เช่น ช่วงเริ่มเสกหรือช่วงกำลังสลายตัว) `drawFullscreenQuad()` จะไม่ถูกเรียกเลย แต่โค้ดด้านล่างนอกลูปกลับสั่ง `_glBlitFrameBuffer` คัดลอกภาพจาก `postTarget` ทับกลับไปยัง `activeDrawFbo` เสมอ ส่งผลให้ภาพที่ยังไม่ถูกวาด (สีดำว่างเปล่า หรือภาพค้างจากเฟรมเก่าใน `postTarget`) ทับหน้าจอผู้เล่นทันที


* **การส่งค่า `critAndBlast` ทำให้เอฟเฟกต์ระเบิด Supernova ไม่ทำงาน**: ใน Java มีการรวมค่า `float critAndBlast = crit + blast;` ส่งเข้า `HoleState.y` แต่ในไฟล์เชดเดอร์ `gargantua.fsh` ถอดรหัสค่าด้วย `float blast = clamp(HoleState.y - 1.0, 0.0, 1.0);` และเช็ค `if (crit >= 0.99 && blast > 0.001)` ซึ่งหมายความว่าหากช่วงที่หลุมดำระเบิด ค่า `criticality()` ลดลงต่ำกว่า `1.0` (เช่น กลายเป็น `0.0` หลังพ้นระยะ `critEnd` เหมือนในตรรกะของ `GargantuaShake.java`) ค่า `crit + blast` จะไม่เกิน `1.0` ทำให้ `blast` ในเชดเดอร์กลายเป็น `0.0` และเอฟเฟกต์ระเบิด Supernova ไม่แสดงผลเลย


* **ความแม่นยำของเวลา (`timeSec`) ลดลงในโลกที่เปิดมานาน (Float Precision Loss)**: การคำนวณ `(entity.level().getGameTime() + partialTicks) / 20.0f` โดยไม่ทำการ Modulo (`%`) ค่า `getGameTime()` ก่อนแปลงเป็น `float` จะทำให้ค่าเวลาสูญเสียความแม่นยำทศนิยมเมื่อโลกมีค่า GameTime สูงๆ ทำให้แอนิเมชันการหมุนของจานมวลพอกพูน (Accretion Disk) กระตุก (ในคอมเมนต์ของเชดเดอร์ระบุไว้ว่าควรเป็น `wrapped seconds`)


* **สถานะ Framebuffer ของ Blaze3D ไม่ซิงค์กันในบล็อก `finally`**: ในบล็อก `try` มีการใช้ `GlStateManager._glBindFramebuffer` และ `postTarget.bindWrite(true)` แต่ใน `finally` กลับใช้คำสั่ง OpenGL ดิบ `GL30.glBindFramebuffer` ซึ่งอาจทำให้ State Cache ภายใน `GlStateManager` ของ Minecraft เพี้ยนได้ ควรใช้ `GlStateManager._glBindFramebuffer` ให้ตรงกันทั้งหมด


* **การจองหน่วยความจำใหม่ทุกเฟรมใน `drawFullscreenQuad()`**: การสร้าง `new ByteBufferBuilder(256)` แล้ว `.close()` ทุกครั้งที่วาด ทำให้เกิดการจองและคืน Native Memory ซ้ำๆ ทุกเฟรม สามารถใช้ `Tesselator.getInstance()` แทนเพื่อลดภาระของระบบได้



#### B. `gargantua.fsh` และ `gargantua.vsh`

* **Gravitational Lensing ไม่บิดเบี้ยวพื้นผิวโลก/บล็อกที่อยู่ด้านหลังหลุมดำ (Critical)**:
* ในลูป Raymarch เมื่อลำแสงเดินทางไปชนบล็อกในฉาก (`travelledBlocks >= sceneDistance`) ตัวแปร `hitSolid` จะถูกตั้งเป็น `true`
* ต่อมาในบรรทัดที่ 274 มีการเตรียมตัวแปร `vec3 sceneOrCosmic = (sceneDistance < 1.0e8) ? lensedScene : background;` เพื่อให้แสดงภาพฉากที่ถูกเลนส์บิดเบี้ยว (`lensedScene`) เมื่อมีบล็อกอยู่ด้านหลัง
* แต่ในบรรทัดที่ 284 กลับเขียนเงื่อนไขว่า `else if (!hitSolid) { behindEffect = sceneOrCosmic; }` ทำให้เมื่อมีภูเขาหรือบล็อกอยู่หลังหลุมดำในระยะ `GARG_ESCAPE` (`60.0 * rg`) ตัวแปร `hitSolid` จะเป็น `true` เสมอ ส่งผลให้ `behindEffect` ไม่ถูกเปลี่ยนเป็น `lensedScene` แต่กลับไปใช้ `sceneColour` (ภาพปกติที่ไม่ถูกบิดเบี้ยว) แทน
* **วิธีแก้**: ต้องแยกแยะระหว่างบล็อกที่อยู่ **หน้าหลุมดำ** (`sceneDistance < frontHoleDist` ซึ่งบังหลุมดำ) กับบล็อกที่อยู่ **หลังหลุมดำ** (ซึ่งต้องแสดงผล `lensedScene`) และตรวจสอบไม่ให้ `lensedUv` ไปดึงพิกเซลของวัตถุที่อยู่หน้าหลุมดำมาปน (Foreground Bleeding)


* **ลำดับการเช็ค `GARG_HORIZON` กับ `sceneDistance` ในลูป Raymarch**: ในลูปมีการเช็ค `if (travelled < GARG_HORIZON)` ก่อน `if (travelledBlocks >= sceneDistance)` หากมีบล็อกวางอยู่ใกล้ขอบฟ้าเหตุการณ์ (Event Horizon) ในก้าว `dt` เดียวกัน ลำแสงอาจถูกตัดสินว่าถูกดูดเข้าหลุมดำ (`captured = true`) ก่อนชนบล็อก ทำให้เงาดำวาดทับบล็อกที่อยู่ข้างหน้า ควรสลับให้เช็ค `travelledBlocks >= sceneDistance` ก่อน


* **ปัญหาการซ้ำของเท็กซ์เจอร์ (`NoiseSampler` UV Wrapping)**: พิกัด `nebUv` และ `streakUv` มีค่าเกินช่วง `[0.0, 1.0]` หาก `NoiseSampler` ใน Minecraft ถูกตั้งค่าเริ่มต้นเป็น `GL_CLAMP_TO_EDGE` จะทำให้ลวดลายของวงแหวนและเนบิวลายืดเป็นเส้นตรงที่ขอบ ควรใส่ `fract()` ครอบพิกัด UV ก่อนส่งเข้า `textureLod(..., 0.0)`


* **ตัวแปร `Position` ใน `gargantua.vsh` ไม่ถูกใช้งาน**: ใน `gargantua.json` มีการประกาศ Attribute `"Position"` และ `"UV0"` แต่ใน `gargantua.vsh` กลับไม่ได้นำ `Position` มาใช้งานใน `main()` เลย ซึ่งอาจทำให้ GLSL Compiler ตัด Attribute `Position` ทิ้ง (Optimize out) และเกิดปัญหากับลำดับ Vertex Attribute บนการ์ดจอบางรุ่นได้



#### C. `GargantuaRenderer.java` และ `GargantuaShake.java`

* **`getTextureLocation` คืนค่า `null`**: ใน `GargantuaRenderer.java` การคืนค่า `null` อาจทำให้เกิด `NullPointerException` หากมีม็อดอื่นหรือระบบของเกมเรียกเช็คเท็กซ์เจอร์ของ Entity ควรคืนค่า ResourceLocation ของเท็กซ์เจอร์ที่มีอยู่จริงแทน


* **ความเสี่ยงหารด้วยศูนย์ (`Division by Zero`) ใน `GargantuaShake.java`**: ในบรรทัด `float openProgress = visualAge / (float) tearEnd;` หาก `tearEnd` มีค่าเป็น `0` และ `visualAge` เป็น `0.0f` จะได้ค่า `NaN` (Not a Number) ทำให้ค่าความสั่นของกล้องพังทั้งเฟรม ควรเช็ค `if (tearEnd > 0 ...)` ก่อนเสมอ



---

### 2. โค้ดที่แก้ไขแล้ว (Fixed Code)

#### 1) `GargantuaRenderer.java`

เปลี่ยนให้ `getTextureLocation` คืนค่า ResourceLocation ที่ถูกต้องเพื่อป้องกัน `NullPointerException`:

```java
package com.frierenflight.zoltraakcinematic.client.renderer;

import com.frierenflight.zoltraakcinematic.ZoltraakCinematicMod;
import com.frierenflight.zoltraakcinematic.entity.GargantuaEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class GargantuaRenderer extends EntityRenderer<GargantuaEntity> {
    private static final ResourceLocation DUMMY_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ZoltraakCinematicMod.MODID, "textures/effect/noise_tile.png");

    public GargantuaRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(GargantuaEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        GargantuaPostProcessor.registerClientInstance(entity);
    }

    @Override
    public ResourceLocation getTextureLocation(GargantuaEntity entity) {
        return DUMMY_TEXTURE;
    }

    @Override
    public boolean shouldRender(GargantuaEntity entity, Frustum camera, double camX, double camY, double camZ) {
        return true;
    }
}
```

#### 2) `GargantuaShake.java`

ป้องกันการหารด้วยศูนย์เมื่อ `tearEnd <= 0`:

```java
                // 1. Spacetime Rupture Opening Jolt
                float openingShake = 0.0f;
                int tearEnd = gargantua.getTearEndTick();
                if (tearEnd > 0 && visualAge >= 0.0f && visualAge <= (float) tearEnd) {
                    float openProgress = visualAge / (float) tearEnd;
                    float distFactor = (float) Mth.clamp(1.0 - dist / 80.0d, 0.0, 1.0);
                    float jolt = (float) (Math.sin(openProgress * Math.PI) * 2.8f);
                    openingShake = jolt * distFactor;
                }
```

#### 3) `GargantuaPostProcessor.java`

แก้ไขลูปการวาด Ping-pong เพื่อป้องกันหน้าจอดำเมื่อ Entity ถูกข้าม, แก้ไข `critAndBlast` ให้รองรับช่วงระเบิด Supernova เสมอ, ทำการ Wrap เวลา `timeSec`, ตั้งค่า `GL_REPEAT` ให้เท็กซ์เจอร์ Noise และซิงค์สถานะ `GlStateManager`:

```java
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

            boolean isShaderPack = ShaderCompatibility.useFullDetailPass();
            int irisDepthTex = ShaderCompatibility.getShaderDepthTexture();
            int activeDrawFbo = (prevDrawFbo != 0 && isShaderPack) ? prevDrawFbo : main.frameBufferId;

            int depthSamplerId = (irisDepthTex > 0) ? irisDepthTex : main.getDepthTextureId();
            int noiseId = getNoiseTextureId(mc);

            Matrix4f invProj = new Matrix4f(projection).invert();
            Matrix4f viewRotation = new Matrix4f(view).setTranslation(0.0f, 0.0f, 0.0f);
            Vec3 camPos = camera.getPosition();

            int renderedCount = 0;

            for (int i = 0; i < entities.size(); i++) {
                GargantuaEntity entity = entities.get(i);
                if (entity == null || entity.isRemoved()) continue;

                float rg = entity.gravitationalRadius(partialTicks);
                float brightness = entity.brightness(partialTicks);
                if (rg < 0.01f || brightness <= 0.0f) continue;

                if (renderedCount == 0) {
                    // คัดลอกภาพหน้าจอเริ่มต้นเข้าสู่ sceneCopy เฉพาะเมื่อมี Entity ที่ต้องวาดจริง
                    GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, activeDrawFbo);
                    GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, sceneCopy.frameBufferId);
                    GlStateManager._glBlitFrameBuffer(
                            0, 0, main.width, main.height,
                            0, 0, sceneCopy.width, sceneCopy.height,
                            GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST
                    );

                    postTarget.bindWrite(true);
                    RenderSystem.disableBlend();
                    RenderSystem.disableDepthTest();
                    RenderSystem.depthMask(false);
                    RenderSystem.disableCull();
                    RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
                } else {
                    // Ping-pong คัดลอกผลลัพธ์จาก postTarget กลับไปที่ sceneCopy สำหรับหลุมดำลูกถัดไป
                    GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, postTarget.frameBufferId);
                    GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, sceneCopy.frameBufferId);
                    GlStateManager._glBlitFrameBuffer(
                            0, 0, postTarget.width, postTarget.height,
                            0, 0, sceneCopy.width, sceneCopy.height,
                            GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST
                    );
                    postTarget.bindWrite(true);
                }

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

                Vec3 worldTangent = Math.abs(axis.y) < 0.99 ? axis.cross(new Vec3(0, 1, 0)).normalize() : axis.cross(new Vec3(1, 0, 0)).normalize();
                Vector3f eyeDiskU = viewRotation.transformDirection(
                        new Vector3f((float) worldTangent.x, (float) worldTangent.y, (float) worldTangent.z),
                        new Vector3f()
                ).normalize();

                shader.setSampler("SceneSampler", sceneCopy.getColorTextureId());
                shader.setSampler("DepthSampler", depthSamplerId);
                shader.setSampler("NoiseSampler", noiseId);

                float lensMult = com.frierenflight.zoltraakcinematic.config.ZoltraakCinematicClientConfig.isGravitationalLensingEnabled() ? 1.0f : 0.0f;
                shader.safeGetUniform("HoleCentre").set(eyeCenter.x, eyeCenter.y, eyeCenter.z, rg);
                shader.safeGetUniform("SpinAxis").set(eyeAxis.x, eyeAxis.y, eyeAxis.z, lensMult);
                shader.safeGetUniform("DiskU").set(eyeDiskU.x, eyeDiskU.y, eyeDiskU.z, 0.0f);

                float opened = entity.opened(partialTicks);
                float crit = entity.criticality(partialTicks);
                float blast = entity.blastFlash(partialTicks);
                // บังคับให้ฐานเป็น 1.0f เมื่อมีค่า blast เพื่อให้เชดเดอร์คำนวณ HoleState.y - 1.0 ได้ถูกต้องเสมอ
                float critAndBlast = (blast > 0.001f) ? (1.0f + blast) : crit;
                // Wrap เวลาเพื่อป้องกันการสูญเสียความละเอียดของ float ในโลกที่เปิดเล่นนานๆ
                float timeSec = ((entity.level().getGameTime() % 72000L) + partialTicks) / 20.0f;
                float swallowed = (float) Math.min(entity.getSwallowedCount(), 10);
                shader.safeGetUniform("HoleState").set(opened, critAndBlast, timeSec, swallowed);

                float raymarchSteps = (float) Math.max(64, com.frierenflight.zoltraakcinematic.config.ZoltraakCinematicClientConfig.getRaymarchStepCount());
                shader.safeGetUniform("DiskShape").set(3.83f, 13.5f, raymarchSteps, brightness);
                shader.safeGetUniform("InverseProjectionMat").set(invProj);

                RenderSystem.setShader(() -> shader);
                drawFullscreenQuad();
                renderedCount++;
            }

            // Blit กลับไปยัง activeDrawFbo เฉพาะเมื่อมีการวาดจริงอย่างน้อย 1 Entity เท่านั้น
            if (renderedCount > 0) {
                GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, postTarget.frameBufferId);
                GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, activeDrawFbo);
                GlStateManager._glBlitFrameBuffer(
                        0, 0, postTarget.width, postTarget.height,
                        0, 0, main.width, main.height,
                        GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST
                );
            }

        } catch (Throwable t) {
            System.err.println("[ZoltraakCinematic] Gargantua post-processing error: " + t.getMessage());
        } finally {
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, prevReadFbo);
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, prevDrawFbo);
            GlStateManager._viewport(0, 0, main.viewWidth, main.viewHeight);
            if (prevDepthTest) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
            if (prevCull) RenderSystem.enableCull(); else RenderSystem.disableCull();
            if (prevBlend) RenderSystem.enableBlend(); else RenderSystem.disableBlend();
            RenderSystem.depthMask(true);
            RenderSystem.defaultBlendFunc();
        }
    }

    private static int getNoiseTextureId(Minecraft mc) {
        AbstractTexture tex = mc.getTextureManager().getTexture(NOISE_TEX);
        if (tex == null) {
            mc.getTextureManager().bindForSetup(NOISE_TEX);
            tex = mc.getTextureManager().getTexture(NOISE_TEX);
        }
        if (tex != null) {
            tex.bind();
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
            return tex.getId();
        }
        return 0;
    }

    private static void drawFullscreenQuad() {
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder builder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        builder.addVertex(-1.0f, -1.0f, 0.0f).setUv(0.0f, 0.0f);
        builder.addVertex( 1.0f, -1.0f, 0.0f).setUv(1.0f, 0.0f);
        builder.addVertex( 1.0f,  1.0f, 0.0f).setUv(1.0f, 1.0f);
        builder.addVertex(-1.0f,  1.0f, 0.0f).setUv(0.0f, 1.0f);
        BufferUploader.drawWithShader(builder.buildOrThrow());
    }
```

#### 4) `gargantua.vsh`

นำ `Position` มาใช้งานเพื่อไม่ให้ GLSL Compiler ตัด Attribute ทิ้ง:

```glsl
#version 150

in vec3 Position;
in vec2 UV0;

out vec2 vUv;

void main() {
    vUv = UV0;
    gl_Position = vec4(Position.xy, 0.0, 1.0);
}
```

#### 5) `gargantua.fsh`

แก้ไขการเช็ค Depth Occlusion, การแสดงผล Gravitational Lensing บนบล็อกด้านหลังหลุมดำ และใส่ `fract()` ป้องกันปัญหา UV Clamping:

```glsl
    // ในฟังก์ชัน gargCosmicSky ปรับการอ่านค่า NoiseSampler ให้ใช้ fract()
    vec2 nebUv = skyUv * vec2(2.4, 1.2) + vec2(time * 0.003, time * 0.001);
    float n1 = dot(textureLod(NoiseSampler, fract(nebUv), 0.0).rgb, vec3(0.333));
    float n2 = dot(textureLod(NoiseSampler, fract(nebUv * 2.3 + vec2(0.35, 0.72)), 0.0).rgb, vec3(0.333));
    float n3 = dot(textureLod(NoiseSampler, fract(nebUv * 5.2 - vec2(0.48, 0.22)), 0.0).rgb, vec3(0.333));
```

และในฟังก์ชัน `main()` ของ `gargantua.fsh` ปรับส่วนลูปและการผสมภาพด้านหลังดังนี้:

```glsl
        p += d * dt;
        travelledBlocks += dt * rg;
        float travelled = length(p);
        float lensMult = SpinAxis.w >= 0.05 ? 1.0 : 0.0;
        d = normalize(d + (-1.5 * lensMult * h * h * p / pow(travelled, 5.0)) * dt);

        // เช็คการชนบล็อกในฉากก่อนเช็ค GARG_HORIZON เพื่อไม่ให้เงาหลุมดำทะลุบล็อกตรงขอบ
        if (travelledBlocks >= sceneDistance) {
            hitSolid = true;
            break;
        }
        if (travelled < GARG_HORIZON) {
            captured = true;
            break;
        }
        if (travelled > GARG_ESCAPE) {
            break;
        }
```

```glsl
            // ในลูป octave ของ Accretion Disk ใช้ fract(streakUv)
            float sampleVal = dot(textureLod(NoiseSampler, fract(streakUv), 0.0).rgb, vec3(0.299, 0.587, 0.114));
```

```glsl
    // Screen-space gravitational lensing: ป้องกันการดึงพิกเซลของวัตถุที่อยู่หน้าหลุมดำมาเบลอปน
    vec2 lensedUv = vUv;
    if (SpinAxis.w >= 0.05 && openFactor > 0.001) {
        vec2 candidateUv = clamp(vUv + (d.xy - rayDir.xy) * 0.35 * openFactor, 0.001, 0.999);
        float candidateDepth = texture(DepthSampler, candidateUv).r;
        if (candidateDepth >= SL_SKY_DEPTH) {
            lensedUv = candidateUv;
        } else if (candidateDepth > 0.0001) {
            vec4 candClip = vec4(candidateUv * 2.0 - 1.0, candidateDepth * 2.0 - 1.0, 1.0);
            vec4 candEye = InverseProjectionMat * candClip;
            float candDist = (abs(candEye.w) > 0.000001) ? length(candEye.xyz / candEye.w) : 1.0e9;
            if (candDist >= holeDistance - shadowEdge) {
                lensedUv = candidateUv;
            }
        }
    }
    vec3 lensedScene = texture(SceneSampler, lensedUv).rgb;

    // ตรวจสอบว่าถูกบังโดยบล็อกที่อยู่ด้านหน้าหลุมดำหรือไม่
    bool occludedByForeground = hitSolid && (sceneDistance < frontHoleDist && sceneDistance < holeDistance);

    // Background behind the disk
    vec3 sceneOrCosmic = (sceneDistance < 1.0e8) ? lensedScene : background;
    vec3 behindEffect = sceneColour;

    if (captured || isInsideShadow) {
        if (crit >= 0.99 && blast > 0.001) {
            vec3 coreBurst = mix(vec3(1.4, 1.0, 0.6), vec3(4.5, 4.0, 3.2), blast);
            behindEffect = mix(sceneOrCosmic, coreBurst, clamp(blast * 1.6 + openFactor * 0.45, 0.0, 1.0));
        } else {
            behindEffect = vec3(0.0);
        }
    } else if (!occludedByForeground) {
        // แสดงผล lensedScene ทั้งกับท้องฟ้าและกับบล็อกที่อยู่ด้านหลังหลุมดำ
        behindEffect = sceneOrCosmic;
    }
```
