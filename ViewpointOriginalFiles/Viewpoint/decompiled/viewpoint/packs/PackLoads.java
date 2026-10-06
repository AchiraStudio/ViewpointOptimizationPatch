/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.packs;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import viewpoint.packs.ArtReader;
import viewpoint.packs.ObjReader;
import viewpoint.render.PackData;
import viewpoint.render.PackModel;
import viewpoint.render.PackModels;

public final class PackLoads {
    private static final int IN_FLIGHT = 8;
    private static final long RETRY_NANOS = 2000000000L;
    private static final long STALE_NANOS = 5000000000L;
    private static final ThreadPoolExecutor WORKER = new ThreadPoolExecutor(1, 1, 30L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), runnable -> {
        Thread thread = new Thread(runnable, "Viewpoint model packs");
        thread.setDaemon(true);
        thread.setPriority(1);
        return thread;
    });
    private static final ArrayDeque<PackModel> asked = new ArrayDeque();
    private static int reading;

    public static boolean want(PackModel packModel, long l) {
        packModel.wanted(l);
        int n = packModel.state();
        if (n == 5 && !PackModels.full() && l - packModel.refused() > 2000000000L) {
            packModel.retry();
            n = 0;
        }
        if (n == 0) {
            packModel.reading();
            asked.add(packModel);
        }
        return n == 3;
    }

    public static void frame() {
        reading = PackModels.onTheirWay();
        long l = System.nanoTime();
        while (reading < 8 && !asked.isEmpty() && !PackModels.full()) {
            PackModel packModel = asked.poll();
            if (l - packModel.wanted() > 5000000000L) {
                PackModels.dropped(packModel);
                continue;
            }
            PackModels.onItsWay();
            ++reading;
            WORKER.execute(() -> PackLoads.read(packModel));
        }
    }

    public static void clear() {
        for (PackModel packModel : asked) {
            packModel.retry();
        }
        asked.clear();
    }

    private static void read(PackModel packModel) {
        try {
            packModel.read(PackLoads.load(packModel.obj));
        }
        catch (IOException | RuntimeException exception) {
            System.out.println("[Viewpoint] model pack: " + String.valueOf(packModel.obj) + " unreadable, its sprites keep their art: " + String.valueOf(exception));
            packModel.failed();
            PackModels.offItsWay();
        }
    }

    static PackData load(File file) throws IOException {
        ObjReader.Mesh mesh = ObjReader.parse(PackLoads.lines(file));
        if (mesh.indices().length == 0) {
            throw new IOException("no triangles");
        }
        if (mesh.mtllib() == null) {
            throw new IOException("no mtllib");
        }
        File file2 = new File(file.getParentFile(), mesh.mtllib());
        String string = ObjReader.texture(PackLoads.lines(file2));
        if (string == null) {
            throw new IOException("no map_Kd in " + file2.getName());
        }
        ArtReader.Art art = ArtReader.read(new File(file2.getParentFile(), string));
        return new PackData(mesh.vertices(), mesh.indices(), art.width(), art.height(), art.paddedWidth(), art.paddedHeight(), art.levels());
    }

    private static List<String> lines(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8).lines().toList();
    }

    private PackLoads() {
    }
}

