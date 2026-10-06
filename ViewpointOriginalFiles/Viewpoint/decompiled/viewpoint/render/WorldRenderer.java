/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 *  org.lwjgl.opengl.GL11
 *  org.lwjgl.opengl.GL30
 *  org.lwjgl.opengl.GL33
 */
package viewpoint.render;

import java.util.function.LongSupplier;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL33;
import viewpoint.platform.Gl;
import viewpoint.platform.GlDebug;
import viewpoint.platform.GlProgram;
import viewpoint.platform.GpuParts;
import viewpoint.platform.PackPass;
import viewpoint.platform.Pipeline;
import viewpoint.platform.Profile;
import viewpoint.platform.Settings;
import viewpoint.platform.SettingsWindow;
import viewpoint.platform.Threads;
import viewpoint.platform.Tuning;
import viewpoint.render.BlurPass;
import viewpoint.render.FarPass;
import viewpoint.render.FarShadow;
import viewpoint.render.FloorBakes;
import viewpoint.render.FloorPreview;
import viewpoint.render.FrameContext;
import viewpoint.render.FrameStream;
import viewpoint.render.FrameUniforms;
import viewpoint.render.IndirectPass;
import viewpoint.render.IrisMode;
import viewpoint.render.Meshes;
import viewpoint.render.ModelPass;
import viewpoint.render.MousePick;
import viewpoint.render.PackLinks;
import viewpoint.render.PackModels;
import viewpoint.render.PackStages;
import viewpoint.render.PostPass;
import viewpoint.render.SceneData;
import viewpoint.render.ShadowPass;
import viewpoint.render.ShellGround;
import viewpoint.render.SkyPass;
import viewpoint.render.SurfacePass;
import viewpoint.render.TargetOutline;
import viewpoint.render.Targets;
import viewpoint.render.TemporalPass;
import viewpoint.render.TranslucentPass;
import viewpoint.render.TreeBaker;
import viewpoint.render.VolumePass;
import viewpoint.render.WeatherMap;
import viewpoint.render.WeatherPass;
import viewpoint.render.Wireframe;

public final class WorldRenderer {
    public static final float NEAR = 0.05f;
    public static final float FAR = 400.0f;
    public static final int CASCADES = 3;
    public static volatile int debugView;
    public static volatile int isolate;
    public static final String[] ISOLATE_NAMES;
    static final int ISO_TAA = 1;
    static final int ISO_GI = 2;
    static final int ISO_LAMPS = 3;
    static final int ISO_VOLUME = 4;
    static final int ISO_SHADOWS = 5;
    static final int ISO_CLOUDS = 6;
    static final int ISO_FLASH = 7;
    static final int ISO_LAMP_SHADOWS = 8;
    static final int ISO_FAR_LAMPS = 9;
    static LongSupplier clock;
    private static final FrameContext frame;
    private static Targets targets;
    private static ShadowPass shadows;
    private static WeatherMap weatherMap;
    private static SkyPass sky;
    private static SurfacePass surfaces;
    private static TranslucentPass translucent;
    private static IndirectPass indirect;
    private static WeatherPass weather;
    private static VolumePass volume;
    private static PostPass post;
    private static PackStages stages;
    private static TemporalPass temporal;
    private static FarPass far;
    private static ModelPass models;
    private static TargetOutline target;
    private static FloorBakes floors;
    private static ShellGround ground;
    public static volatile boolean cullFromPlayer;
    public static final Matrix4f cullView;
    public static volatile boolean freeCamera;
    public static volatile boolean handFromPlayer;
    public static final Matrix4f handView;
    private static boolean ready;
    private static boolean debugInstalled;
    private static boolean irisDrawn;
    private static long nextShaderPoll;

    public static float cascadeRadius(int n, SceneData sceneData) {
        return ShadowPass.cascadeRadius(n, sceneData);
    }

    public static float farShadowRadius(float f) {
        return ShadowPass.farShadowRadius(f);
    }

    static boolean off(int n) {
        int n2 = Wireframe.mode();
        Pipeline pipeline = WorldRenderer.frame.pipeline;
        return isolate == n || n2 != 0 && n == 1 || n2 == 2 && (n == 2 || n == 4 || n == 6) || n == 1 && !pipeline.taa || n == 2 && !pipeline.indirect || n == 4 && !pipeline.volumetrics || n == 5 && !pipeline.shadows || n == 6 && !pipeline.clouds || n == 8 && (pipeline.lampShadows == 0 || isolate == 3) || n == 9 && !pipeline.farLamps;
    }

    public static boolean begin(SceneData sceneData, Matrix4f matrix4f, float f, float f2, float f3) {
        Threads.render();
        if (!ready && !WorldRenderer.init()) {
            return false;
        }
        WorldRenderer.developer();
        PackLinks.follow();
        GlDebug.listen(true);
        GlDebug.push("Viewpoint frame");
        WorldRenderer.frame.outputDrawFbo = GL11.glGetInteger((int)36006);
        WorldRenderer.frame.outputReadFbo = GL11.glGetInteger((int)36010);
        WorldRenderer.frame.outputScissor = GL11.glIsEnabled((int)3089);
        GL11.glGetIntegerv((int)2978, (int[])WorldRenderer.frame.viewport);
        int n = WorldRenderer.frame.viewport[2];
        int n2 = WorldRenderer.frame.viewport[3];
        int n3 = GlProgram.pack().pipeline.cloudResolution;
        if (WorldRenderer.targets.gbufferFbo == 0 || n != WorldRenderer.targets.width || n2 != WorldRenderer.targets.height || n3 != WorldRenderer.targets.cloudShare) {
            boolean bl = targets.ensure(n, n2, n3);
            WorldRenderer.temporal.historyValid = false;
            GL30.glBindFramebuffer((int)36009, (int)WorldRenderer.frame.outputDrawFbo);
            GL30.glBindFramebuffer((int)36008, (int)WorldRenderer.frame.outputReadFbo);
            if (!bl) {
                GlDebug.popAll();
                return false;
            }
        }
        WorldRenderer.frame.width = WorldRenderer.targets.width;
        WorldRenderer.frame.height = WorldRenderer.targets.height;
        WorldRenderer.frame.halfWidth = WorldRenderer.targets.halfWidth;
        WorldRenderer.frame.halfHeight = WorldRenderer.targets.halfHeight;
        Profile.mark(0);
        FrameStream.frameStarted();
        ++WorldRenderer.frame.index;
        Gl.resetState();
        GL11.glClearDepth((double)1.0);
        WorldRenderer.frame.scene = sceneData;
        WorldRenderer.frame.pipeline = GlProgram.pack().pipeline;
        WorldRenderer.frame.view = matrix4f;
        WorldRenderer.frame.viewRotation.set((Matrix4fc)matrix4f).transpose();
        WorldRenderer.frame.eyeX = f;
        WorldRenderer.frame.eyeY = f2;
        WorldRenderer.frame.eyeZ = f3;
        temporal.begin(frame);
        FrameUniforms.set(frame, targets);
        WorldRenderer.frame.sun = sceneData.sunStrength > 0.02f;
        floors.update(sceneData);
        ground.update(sceneData);
        Profile.mark(1);
        PackModels.upload(System.nanoTime());
        Meshes.prepare(sceneData);
        models.prepare(sceneData);
        irisDrawn = IrisMode.draw(frame, shadows, models, far);
        if (!irisDrawn) {
            WorldRenderer.drawWorld();
        }
        GlDebug.push("models");
        return true;
    }

    private static void drawWorld() {
        GlDebug.push("far shadow");
        far.prepare(frame, true);
        GlDebug.pop();
        shadows.draw(frame);
        Profile.mark(2);
        GlDebug.push("g-buffer");
        Wireframe.begin();
        GpuParts.begin(0);
        surfaces.gbuffer(frame);
        GpuParts.end(0);
        GpuParts.begin(1);
        models.gbuffer(frame);
        GpuParts.end(1);
        GpuParts.begin(2);
        far.gbuffer(frame);
        GpuParts.end(2);
        Wireframe.end();
        GlDebug.pop();
        MousePick.read(frame, WorldRenderer.targets.gbufferFbo, WorldRenderer.frame.invViewProjection);
        Profile.mark(3);
        if (WorldRenderer.frame.pipeline.clouds) {
            GlDebug.push("weather map");
            GpuParts.begin(20);
            weatherMap.draw(frame);
            GpuParts.end(20);
            GlDebug.pop();
        }
        GlDebug.push("sky");
        GL30.glBindFramebuffer((int)36160, (int)WorldRenderer.targets.lightFbo);
        GL11.glViewport((int)0, (int)0, (int)WorldRenderer.frame.width, (int)WorldRenderer.frame.height);
        if (Wireframe.mode() == 2) {
            GL11.glClearColor((float)0.0f, (float)0.0f, (float)0.0f, (float)1.0f);
            GL11.glClear((int)16384);
        } else {
            if (!WorldRenderer.off(6)) {
                GpuParts.begin(12);
                sky.clouds(frame);
                GpuParts.end(12);
                GL30.glBindFramebuffer((int)36160, (int)WorldRenderer.targets.lightFbo);
                GL11.glViewport((int)0, (int)0, (int)WorldRenderer.frame.width, (int)WorldRenderer.frame.height);
            }
            sky.sky(frame);
        }
        GlDebug.pop();
        GlDebug.push("lighting");
        GpuParts.begin(13);
        surfaces.light(frame);
        GpuParts.end(13);
        GlDebug.pop();
        Profile.mark(4);
        GlDebug.push("far world");
        Wireframe.begin();
        far.draw(frame);
        Wireframe.end();
        GlDebug.pop();
        WorldRenderer.bindTarget();
        Profile.mark(5);
    }

    public static void drawOutlines() {
        GlDebug.push("outlines");
        if (!irisDrawn) {
            models.outlines(frame);
        }
        target.mask(frame, models);
        GlDebug.pop();
    }

    public static void drawTranslucent(SceneData sceneData) {
        GlDebug.pop();
        Gl.resetState();
        if (irisDrawn) {
            return;
        }
        FrameUniforms.bind(targets);
        WorldRenderer.bindTarget();
        GlDebug.push("translucent");
        if (Wireframe.mode() == 0) {
            translucent.draw(frame, surfaces, models, shadows);
        }
        GlDebug.pop();
        WorldRenderer.runPasses(PackPass.Stage.TRANSLUCENT, WorldRenderer.targets.hdr);
        WorldRenderer.bindTarget();
        Profile.mark(7);
    }

    private static int runPasses(PackPass.Stage stage, int n) {
        if (!stages.any(stage)) {
            return stages.run(stage, frame, n);
        }
        GlDebug.push("pack passes");
        GpuParts.begin(21 + stage.ordinal());
        int n2 = stages.run(stage, frame, n);
        GpuParts.end(21 + stage.ordinal());
        GlDebug.pop();
        return n2;
    }

    public static void drawIndirect(SceneData sceneData) {
        if (!irisDrawn && !WorldRenderer.off(2)) {
            GlDebug.push("bounce light");
            indirect.draw(frame);
            GlDebug.pop();
            WorldRenderer.bindTarget();
        }
        Profile.mark(8);
    }

    public static void drawWeather(SceneData sceneData, float f, float f2, float f3) {
        if (irisDrawn || !WeatherPass.any(sceneData)) {
            Profile.mark(9);
            return;
        }
        GlDebug.push("weather");
        WorldRenderer.bindTarget();
        weather.draw(frame);
        GlDebug.pop();
        WorldRenderer.bindTarget();
        Profile.mark(9);
    }

    public static void finish(SceneData sceneData) {
        if (irisDrawn) {
            target.composite(frame);
        } else {
            WorldRenderer.passes();
        }
        models.endFrame();
        GL11.glDepthMask((boolean)true);
        Profile.mark(12);
        Profile.endFrame();
        if (WorldRenderer.frame.outputScissor) {
            GL11.glEnable((int)3089);
        }
        GlDebug.popAll();
        GlDebug.listen(false);
    }

    private static void passes() {
        if (!WorldRenderer.off(4)) {
            GlDebug.push("volumetrics");
            volume.draw(frame);
            GlDebug.pop();
        }
        Profile.mark(10);
        if (WorldRenderer.frame.pipeline.bloom) {
            GlDebug.push("bloom");
            post.bloom(frame);
            GlDebug.pop();
        }
        Profile.mark(11);
        WorldRenderer.runPasses(PackPass.Stage.COMPOSITE, WorldRenderer.targets.hdr);
        boolean bl = !WorldRenderer.off(1);
        boolean bl2 = stages.any(PackPass.Stage.FINAL);
        if (bl || bl2) {
            GL30.glBindFramebuffer((int)36160, (int)WorldRenderer.targets.taaInputFbo);
            GL11.glViewport((int)0, (int)0, (int)WorldRenderer.frame.width, (int)WorldRenderer.frame.height);
        } else {
            GL30.glBindFramebuffer((int)36009, (int)WorldRenderer.frame.outputDrawFbo);
            GL30.glBindFramebuffer((int)36008, (int)WorldRenderer.frame.outputReadFbo);
            GL11.glViewport((int)WorldRenderer.frame.viewport[0], (int)WorldRenderer.frame.viewport[1], (int)WorldRenderer.frame.viewport[2], (int)WorldRenderer.frame.viewport[3]);
        }
        GlDebug.push("composite");
        GpuParts.begin(15);
        post.composite(frame);
        GpuParts.end(15);
        GlDebug.pop();
        int n = WorldRenderer.targets.taaInput;
        if (bl) {
            GlDebug.push("taa");
            GpuParts.begin(16);
            n = temporal.resolve(frame);
            GpuParts.end(16);
            GlDebug.pop();
        } else {
            WorldRenderer.temporal.historyValid = false;
            temporal.remember(frame);
        }
        if (bl || bl2) {
            n = WorldRenderer.runPasses(PackPass.Stage.FINAL, n);
            temporal.present(frame, n, bl ? Tuning.sharpen : 0.0f);
        }
        GlDebug.push("rain over");
        weather.overlay();
        GlDebug.pop();
        GlDebug.push("target outline");
        target.composite(frame);
        GlDebug.pop();
    }

    static void resetState() {
        Gl.resetState();
    }

    public static void restoreOutput() {
        GlDebug.popAll();
        GlDebug.listen(false);
        if (!ready) {
            return;
        }
        GL33.glBindSampler((int)0, (int)0);
        GL30.glBindVertexArray((int)0);
        GL30.glBindFramebuffer((int)36009, (int)WorldRenderer.frame.outputDrawFbo);
        GL30.glBindFramebuffer((int)36008, (int)WorldRenderer.frame.outputReadFbo);
        GL11.glViewport((int)WorldRenderer.frame.viewport[0], (int)WorldRenderer.frame.viewport[1], (int)WorldRenderer.frame.viewport[2], (int)WorldRenderer.frame.viewport[3]);
    }

    public static void bindTarget() {
        GL30.glBindFramebuffer((int)36160, (int)WorldRenderer.targets.hdrFbo);
        GL11.glViewport((int)0, (int)0, (int)WorldRenderer.frame.width, (int)WorldRenderer.frame.height);
    }

    public static int targetFbo() {
        return WorldRenderer.targets.hdrFbo;
    }

    static boolean historyValid() {
        return temporal != null && WorldRenderer.temporal.historyValid;
    }

    static Targets targets() {
        return targets;
    }

    static WeatherMap weatherMap() {
        return weatherMap;
    }

    private static boolean init() {
        Gl.init();
        Meshes.init();
        targets = new Targets();
        targets.createFixed();
        models = new ModelPass(targets);
        target = new TargetOutline();
        Meshes.floors = floors = new FloorBakes();
        Profile.floorsReport = floors::report;
        Profile.packsReport = PackModels::report;
        SettingsWindow.panel("World/Floor preview", new FloorPreview(floors)::draw);
        ground = new ShellGround();
        Profile.shellGroundReport = ground::report;
        SettingsWindow.panel("World/Levels of detail", ground::drawFigures);
        TreeBaker treeBaker = new TreeBaker();
        FarShadow farShadow = new FarShadow(treeBaker);
        weatherMap = new WeatherMap(WorldRenderer.targets.noise);
        shadows = new ShadowPass(models, farShadow, weatherMap);
        BlurPass blurPass = new BlurPass();
        sky = new SkyPass(targets, weatherMap);
        surfaces = new SurfacePass(targets, shadows);
        translucent = new TranslucentPass(targets);
        indirect = new IndirectPass(targets, shadows, blurPass);
        weather = new WeatherPass(targets, shadows, weatherMap);
        volume = new VolumePass(targets, shadows, blurPass);
        post = new PostPass(targets);
        stages = new PackStages(targets, weatherMap);
        temporal = new TemporalPass(targets);
        far = new FarPass(targets, shadows, farShadow, treeBaker);
        GlProgram.linkPasses();
        PackLinks.start();
        if (!GlProgram.allLinked() || !shadows.complete()) {
            return false;
        }
        ready = true;
        return true;
    }

    private static void developer() {
        if (!Settings.dev) {
            return;
        }
        if (!debugInstalled) {
            debugInstalled = true;
            GlDebug.install();
        }
        long l = System.nanoTime();
        Tuning.poll(l);
        if (l >= nextShaderPoll) {
            nextShaderPoll = l + 1000000000L;
            GlProgram.reloadChanged();
        }
    }

    private WorldRenderer() {
    }

    static {
        ISOLATE_NAMES = new String[]{"all effects on", "TAA off", "GI and occlusion off", "per-pixel lamps off", "volumetrics off", "sun and moon shadows off", "clouds off", "flashlight off", "lamp shadows off", "far lamps off"};
        clock = System::nanoTime;
        frame = new FrameContext();
        cullView = new Matrix4f();
        handView = new Matrix4f();
    }
}

