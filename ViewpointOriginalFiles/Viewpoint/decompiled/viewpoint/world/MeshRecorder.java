/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.lwjgl.system.MemoryUtil
 *  zombie.ZomboidFileSystem
 *  zombie.iso.IsoCell
 */
package viewpoint.world;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.CallSite;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.lwjgl.system.MemoryUtil;
import viewpoint.packs.ModelPacks;
import viewpoint.platform.Keys;
import viewpoint.world.Cook;
import viewpoint.world.Recipe;
import viewpoint.world.RecipeCodec;
import viewpoint.world.WorldMesher;
import zombie.ZomboidFileSystem;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkMap;

public final class MeshRecorder {
    static final String FOLDER = "viewpoint-recordings";
    static final String SUFFIX = ".meshrec";
    private static final int RING = 2;
    private static final int MAGIC = 1348095314;
    private static final int FORMAT = 1;

    public static void poll(IsoCell isoCell, int n, float f, float f2, float f3) {
        boolean bl = Keys.MESH_VERIFY.pressed();
        if (!bl && !Keys.MESH_RECORD.pressed()) {
            return;
        }
        try {
            if (bl) {
                MeshRecorder.verify(isoCell, n);
            } else {
                MeshRecorder.record(isoCell, n, f, f2, f3);
            }
        }
        catch (IOException | RuntimeException exception) {
            System.out.println("[Viewpoint] mesh " + (bl ? "verify" : "recording") + " failed: " + String.valueOf(exception));
        }
    }

    private static void record(IsoCell isoCell, int n, float f, float f2, float f3) throws IOException {
        IsoChunkMap isoChunkMap = isoCell.getChunkMap(n);
        int n2 = Math.floorDiv((int)Math.floor(f), 8);
        int n3 = Math.floorDiv((int)Math.floor(f2), 8);
        ArrayList<Entry> arrayList = new ArrayList<Entry>();
        int n4 = 0;
        for (int i = n3 - 2; i <= n3 + 2; ++i) {
            for (int j = n2 - 2; j <= n2 + 2; ++j) {
                IsoChunk isoChunk = MeshRecorder.loadedChunk(isoChunkMap, j, i);
                if (isoChunk == null) continue;
                for (int k = isoChunk.minLevel; k <= isoChunk.maxLevel; ++k) {
                    Entry entry = MeshRecorder.capture(isoChunk, k, n);
                    if (entry == null) {
                        ++n4;
                        continue;
                    }
                    arrayList.add(entry);
                }
            }
        }
        Path path = MeshRecorder.folder().resolve("recording-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + SUFFIX);
        MeshRecorder.write(path, new Recording(f, f2, f3, arrayList));
        System.out.println("[Viewpoint] mesh recording: " + arrayList.size() + " levels around " + (int)f + "," + (int)f2 + "," + (int)f3 + " into " + String.valueOf(path) + (String)(n4 > 0 ? "; " + n4 + " levels still loading were left out" : ""));
    }

    private static void verify(IsoCell isoCell, int n) throws IOException {
        Path path = MeshRecorder.newest(MeshRecorder.folder());
        if (path == null) {
            System.out.println("[Viewpoint] mesh verify: no recording in " + String.valueOf(MeshRecorder.folder()) + " yet (" + Keys.MESH_RECORD.display() + " records one)");
            return;
        }
        Recording recording = MeshRecorder.read(path);
        IsoChunkMap isoChunkMap = isoCell.getChunkMap(n);
        ArrayList<CallSite> arrayList = new ArrayList<CallSite>();
        int n2 = 0;
        int n3 = 0;
        int n4 = 0;
        for (Entry entry : recording.entries()) {
            IsoChunk isoChunk = MeshRecorder.loadedChunk(isoChunkMap, entry.chunkX(), entry.chunkY());
            if (isoChunk == null || entry.level() < isoChunk.minLevel || entry.level() > isoChunk.maxLevel) {
                ++n3;
                continue;
            }
            Recipe recipe = WorldMesher.gather(isoChunk, entry.level(), n, true, MeshRecorder.packs());
            if (recipe.pending) {
                ++n4;
                continue;
            }
            if (RecipeCodec.recipeDigest(recipe, MeshRecorder.pageNames(recipe)).equals(entry.recipeDigest())) {
                ++n2;
                continue;
            }
            arrayList.add((CallSite)((Object)(entry.chunkX() + "," + entry.chunkY() + "," + entry.level())));
        }
        System.out.println("[Viewpoint] mesh verify against " + String.valueOf(path.getFileName()) + ": " + n2 + " levels match, " + arrayList.size() + " differ" + (String)(arrayList.isEmpty() ? "" : " " + String.valueOf(arrayList)) + (String)(n3 > 0 ? ", " + n3 + " not loaded here" : "") + (String)(n4 > 0 ? ", " + n4 + " still loading (press again)" : ""));
    }

    private static boolean packs() {
        return ModelPacks.ring() >= 2;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static Entry capture(IsoChunk isoChunk, int n, int n2) throws IOException {
        String string;
        Recipe recipe = WorldMesher.gather(isoChunk, n, n2, true, MeshRecorder.packs());
        if (recipe.pending) {
            return null;
        }
        List<String> list = MeshRecorder.pageNames(recipe);
        Cook.Cooked cooked = Cook.vertices(recipe);
        try {
            string = RecipeCodec.cookDigest(cooked, list);
        }
        finally {
            if (cooked != null) {
                MemoryUtil.memFree((FloatBuffer)cooked.verts());
            }
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);){
            RecipeCodec.write(recipe, dataOutputStream);
        }
        return new Entry(isoChunk.wx, isoChunk.wy, n, RecipeCodec.recipeDigest(recipe, list), string, byteArrayOutputStream.toByteArray());
    }

    private static IsoChunk loadedChunk(IsoChunkMap isoChunkMap, int n, int n2) {
        IsoChunk isoChunk = isoChunkMap.getChunkForGridSquare(n * 8, n2 * 8);
        boolean bl = isoChunk != null && isoChunk.loaded && isoChunk.wx == n && isoChunk.wy == n2;
        return bl ? isoChunk : null;
    }

    private static List<String> pageNames(Recipe recipe) {
        ArrayList<String> arrayList = new ArrayList<String>(recipe.batches.size());
        for (Recipe.Batch batch : recipe.batches) {
            arrayList.add(RecipeCodec.pageName(batch.page));
        }
        return arrayList;
    }

    private static Path folder() throws IOException {
        Path path = new File(ZomboidFileSystem.instance.getCacheDir(), FOLDER).toPath();
        Files.createDirectories(path, new FileAttribute[0]);
        return path;
    }

    static void write(Path path, Recording recording) throws IOException {
        try (DataOutputStream dataOutputStream = new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(Files.newOutputStream(path, new OpenOption[0]))));){
            dataOutputStream.writeInt(1348095314);
            dataOutputStream.writeInt(1);
            dataOutputStream.writeUTF("E1A69EB743EDE60B213A0FE7F8B83D4FCAB773036D256CC4543A336F3B058A33");
            dataOutputStream.writeFloat(recording.x());
            dataOutputStream.writeFloat(recording.y());
            dataOutputStream.writeFloat(recording.z());
            dataOutputStream.writeInt(recording.entries().size());
            for (Entry entry : recording.entries()) {
                dataOutputStream.writeInt(entry.chunkX());
                dataOutputStream.writeInt(entry.chunkY());
                dataOutputStream.writeInt(entry.level());
                dataOutputStream.writeUTF(entry.recipeDigest());
                dataOutputStream.writeUTF(entry.cookDigest());
                dataOutputStream.writeInt(entry.recipe().length);
                dataOutputStream.write(entry.recipe());
            }
        }
    }

    static Recording read(Path path) throws IOException {
        try (DataInputStream dataInputStream = new DataInputStream(new BufferedInputStream(new GZIPInputStream(Files.newInputStream(path, new OpenOption[0]))));){
            if (dataInputStream.readInt() != 1348095314 || dataInputStream.readInt() != 1) {
                throw new IOException(String.valueOf(path) + " is not a mesh recording this build reads");
            }
            dataInputStream.readUTF();
            float f = dataInputStream.readFloat();
            float f2 = dataInputStream.readFloat();
            float f3 = dataInputStream.readFloat();
            int n = dataInputStream.readInt();
            ArrayList<Entry> arrayList = new ArrayList<Entry>(n);
            for (int i = 0; i < n; ++i) {
                int n2 = dataInputStream.readInt();
                int n3 = dataInputStream.readInt();
                int n4 = dataInputStream.readInt();
                String string = dataInputStream.readUTF();
                String string2 = dataInputStream.readUTF();
                byte[] byArray = new byte[dataInputStream.readInt()];
                dataInputStream.readFully(byArray);
                arrayList.add(new Entry(n2, n3, n4, string, string2, byArray));
            }
            Recording recording = new Recording(f, f2, f3, arrayList);
            return recording;
        }
    }

    static List<Path> recordings(Path path2) throws IOException {
        if (!Files.isDirectory(path2, new LinkOption[0])) {
            return List.of();
        }
        try (Stream<Path> stream = Files.list(path2);){
            List<Path> list = stream.filter(path -> path.getFileName().toString().endsWith(SUFFIX)).sorted(Comparator.comparing(Path::getFileName)).toList();
            return list;
        }
    }

    private static Path newest(Path path) throws IOException {
        List<Path> list = MeshRecorder.recordings(path);
        return list.isEmpty() ? null : list.get(list.size() - 1);
    }

    private MeshRecorder() {
    }

    record Entry(int chunkX, int chunkY, int level, String recipeDigest, String cookDigest, byte[] recipe) {
    }

    record Recording(float x, float y, float z, List<Entry> entries) {
    }
}

