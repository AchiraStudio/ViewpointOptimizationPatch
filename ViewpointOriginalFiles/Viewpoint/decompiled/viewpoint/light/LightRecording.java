/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.light;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

final class LightRecording {
    static final String SUFFIX = ".lightrec";
    private static final int MAGIC = 1348095058;
    private static final int FORMAT = 1;
    static final int OUTSIDE = 1;
    static final int ROOFED = 2;
    static final int OPEN_AIR = 4;
    static final int FLOOR = 8;
    static final int STAIRS = 16;
    static final int ELEVATED = 32;
    static final int SEEN = 64;
    static final int CAN_SEE = 128;
    static final int COULD_SEE = 256;
    static final int DOOR_OPEN_N = 512;
    static final int DOOR_OPEN_W = 1024;
    static final int WINDOW_N = 2048;
    static final int WINDOW_W = 4096;
    static final int WINDOW_OPEN_N = 8192;
    static final int WINDOW_OPEN_W = 16384;
    static final int CURTAIN_W = 32768;
    static final int CURTAIN_N = 65536;
    static final int CURTAIN_E = 131072;
    static final int CURTAIN_S = 262144;
    static final int EDGE_N_SHIFT = 20;
    static final int EDGE_W_SHIFT = 22;

    static void write(Path path, Recording recording) throws IOException {
        try (DataOutputStream dataOutputStream = new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(Files.newOutputStream(path, new OpenOption[0]))));){
            LightRecording.write(dataOutputStream, recording);
        }
    }

    static Recording read(Path path) throws IOException {
        try (DataInputStream dataInputStream = new DataInputStream(new BufferedInputStream(new GZIPInputStream(Files.newInputStream(path, new OpenOption[0]))));){
            Recording recording = LightRecording.read(dataInputStream);
            if (recording == null) {
                throw new IOException(String.valueOf(path) + " is not a light recording this build reads");
            }
            Recording recording2 = recording;
            return recording2;
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

    static void write(DataOutputStream dataOutputStream, Recording recording) throws IOException {
        dataOutputStream.writeInt(1348095058);
        dataOutputStream.writeInt(1);
        dataOutputStream.writeUTF("E1A69EB743EDE60B213A0FE7F8B83D4FCAB773036D256CC4543A336F3B058A33");
        dataOutputStream.writeFloat(recording.x());
        dataOutputStream.writeFloat(recording.y());
        dataOutputStream.writeFloat(recording.z());
        dataOutputStream.writeInt(recording.world().size());
        for (Map.Entry<String, Float> object : recording.world().entrySet()) {
            dataOutputStream.writeUTF(object.getKey());
            dataOutputStream.writeFloat(object.getValue().floatValue());
        }
        LightRecording.writeLamps(dataOutputStream, recording.lamps());
        LightRecording.writeRooms(dataOutputStream, recording.rooms());
        LightRecording.writeTorches(dataOutputStream, recording.torches());
        dataOutputStream.writeInt(recording.squares().size());
        for (Square square : recording.squares()) {
            LightRecording.writeSquare(dataOutputStream, square);
        }
    }

    static Recording read(DataInputStream dataInputStream) throws IOException {
        if (dataInputStream.readInt() != 1348095058 || dataInputStream.readInt() != 1) {
            return null;
        }
        dataInputStream.readUTF();
        float f = dataInputStream.readFloat();
        float f2 = dataInputStream.readFloat();
        float f3 = dataInputStream.readFloat();
        int n = dataInputStream.readInt();
        LinkedHashMap<String, Float> linkedHashMap = new LinkedHashMap<String, Float>();
        for (int i = 0; i < n; ++i) {
            linkedHashMap.put(dataInputStream.readUTF(), Float.valueOf(dataInputStream.readFloat()));
        }
        List<Lamp> list = LightRecording.readLamps(dataInputStream);
        List<RoomLight> list2 = LightRecording.readRooms(dataInputStream);
        List<Torch> list3 = LightRecording.readTorches(dataInputStream);
        int n2 = dataInputStream.readInt();
        ArrayList<Square> arrayList = new ArrayList<Square>(n2);
        for (int i = 0; i < n2; ++i) {
            arrayList.add(LightRecording.readSquare(dataInputStream));
        }
        return new Recording(f, f2, f3, linkedHashMap, list, list2, list3, arrayList);
    }

    private static void writeLamps(DataOutputStream dataOutputStream, List<Lamp> list) throws IOException {
        dataOutputStream.writeInt(list.size());
        for (Lamp lamp : list) {
            dataOutputStream.writeInt(lamp.id());
            dataOutputStream.writeInt(lamp.x());
            dataOutputStream.writeInt(lamp.y());
            dataOutputStream.writeInt(lamp.z());
            dataOutputStream.writeInt(lamp.radius());
            dataOutputStream.writeFloat(lamp.r());
            dataOutputStream.writeFloat(lamp.g());
            dataOutputStream.writeFloat(lamp.b());
            dataOutputStream.writeBoolean(lamp.active());
            dataOutputStream.writeInt(lamp.life());
            dataOutputStream.writeBoolean(lamp.hydroPowered());
            dataOutputStream.writeLong(lamp.building());
            dataOutputStream.writeBoolean(lamp.streetLight());
            dataOutputStream.writeInt(lamp.switches());
        }
    }

    private static List<Lamp> readLamps(DataInputStream dataInputStream) throws IOException {
        int n = dataInputStream.readInt();
        ArrayList<Lamp> arrayList = new ArrayList<Lamp>(n);
        for (int i = 0; i < n; ++i) {
            arrayList.add(new Lamp(dataInputStream.readInt(), dataInputStream.readInt(), dataInputStream.readInt(), dataInputStream.readInt(), dataInputStream.readInt(), dataInputStream.readFloat(), dataInputStream.readFloat(), dataInputStream.readFloat(), dataInputStream.readBoolean(), dataInputStream.readInt(), dataInputStream.readBoolean(), dataInputStream.readLong(), dataInputStream.readBoolean(), dataInputStream.readInt()));
        }
        return arrayList;
    }

    private static void writeRooms(DataOutputStream dataOutputStream, List<RoomLight> list) throws IOException {
        dataOutputStream.writeInt(list.size());
        for (RoomLight roomLight : list) {
            dataOutputStream.writeInt(roomLight.id());
            dataOutputStream.writeInt(roomLight.x());
            dataOutputStream.writeInt(roomLight.y());
            dataOutputStream.writeInt(roomLight.z());
            dataOutputStream.writeInt(roomLight.width());
            dataOutputStream.writeInt(roomLight.height());
            dataOutputStream.writeBoolean(roomLight.active());
            dataOutputStream.writeLong(roomLight.room());
            dataOutputStream.writeLong(roomLight.building());
        }
    }

    private static List<RoomLight> readRooms(DataInputStream dataInputStream) throws IOException {
        int n = dataInputStream.readInt();
        ArrayList<RoomLight> arrayList = new ArrayList<RoomLight>(n);
        for (int i = 0; i < n; ++i) {
            arrayList.add(new RoomLight(dataInputStream.readInt(), dataInputStream.readInt(), dataInputStream.readInt(), dataInputStream.readInt(), dataInputStream.readInt(), dataInputStream.readInt(), dataInputStream.readBoolean(), dataInputStream.readLong(), dataInputStream.readLong()));
        }
        return arrayList;
    }

    private static void writeTorches(DataOutputStream dataOutputStream, List<Torch> list) throws IOException {
        dataOutputStream.writeInt(list.size());
        for (Torch torch : list) {
            dataOutputStream.writeInt(torch.id());
            dataOutputStream.writeFloat(torch.x());
            dataOutputStream.writeFloat(torch.y());
            dataOutputStream.writeFloat(torch.z());
            dataOutputStream.writeFloat(torch.r());
            dataOutputStream.writeFloat(torch.g());
            dataOutputStream.writeFloat(torch.b());
            dataOutputStream.writeFloat(torch.angleX());
            dataOutputStream.writeFloat(torch.angleY());
            dataOutputStream.writeFloat(torch.dist());
            dataOutputStream.writeFloat(torch.strength());
            dataOutputStream.writeBoolean(torch.cone());
            dataOutputStream.writeFloat(torch.dot());
            dataOutputStream.writeInt(torch.focusing());
        }
    }

    private static List<Torch> readTorches(DataInputStream dataInputStream) throws IOException {
        int n = dataInputStream.readInt();
        ArrayList<Torch> arrayList = new ArrayList<Torch>(n);
        for (int i = 0; i < n; ++i) {
            arrayList.add(new Torch(dataInputStream.readInt(), dataInputStream.readFloat(), dataInputStream.readFloat(), dataInputStream.readFloat(), dataInputStream.readFloat(), dataInputStream.readFloat(), dataInputStream.readFloat(), dataInputStream.readFloat(), dataInputStream.readFloat(), dataInputStream.readFloat(), dataInputStream.readFloat(), dataInputStream.readBoolean(), dataInputStream.readFloat(), dataInputStream.readInt()));
        }
        return arrayList;
    }

    private static void writeSquare(DataOutputStream dataOutputStream, Square square) throws IOException {
        dataOutputStream.writeInt(square.x());
        dataOutputStream.writeInt(square.y());
        dataOutputStream.writeInt(square.z());
        dataOutputStream.writeInt(square.flags());
        dataOutputStream.writeInt(square.passes());
        dataOutputStream.writeLong(square.building());
        dataOutputStream.writeLong(square.room());
        dataOutputStream.writeInt(square.lightLevel());
        dataOutputStream.writeInt(square.light());
        dataOutputStream.writeFloat(square.dark());
        dataOutputStream.writeFloat(square.targetDark());
        for (int n : square.corners()) {
            dataOutputStream.writeInt(n);
        }
        dataOutputStream.writeByte(square.reached().length);
        for (Reached reached : square.reached()) {
            dataOutputStream.writeInt(reached.id());
            dataOutputStream.writeInt(reached.x());
            dataOutputStream.writeInt(reached.y());
            dataOutputStream.writeInt(reached.z());
            dataOutputStream.writeInt(reached.radius());
            dataOutputStream.writeFloat(reached.r());
            dataOutputStream.writeFloat(reached.g());
            dataOutputStream.writeFloat(reached.b());
            dataOutputStream.writeInt(reached.flags());
        }
        dataOutputStream.writeInt(square.ours());
    }

    private static Square readSquare(DataInputStream dataInputStream) throws IOException {
        int n = dataInputStream.readInt();
        int n2 = dataInputStream.readInt();
        int n3 = dataInputStream.readInt();
        int n4 = dataInputStream.readInt();
        int n5 = dataInputStream.readInt();
        long l = dataInputStream.readLong();
        long l2 = dataInputStream.readLong();
        int n6 = dataInputStream.readInt();
        int n7 = dataInputStream.readInt();
        float f = dataInputStream.readFloat();
        float f2 = dataInputStream.readFloat();
        int[] nArray = new int[8];
        for (int i = 0; i < nArray.length; ++i) {
            nArray[i] = dataInputStream.readInt();
        }
        Reached[] reachedArray = new Reached[dataInputStream.readUnsignedByte()];
        for (int i = 0; i < reachedArray.length; ++i) {
            reachedArray[i] = new Reached(dataInputStream.readInt(), dataInputStream.readInt(), dataInputStream.readInt(), dataInputStream.readInt(), dataInputStream.readInt(), dataInputStream.readFloat(), dataInputStream.readFloat(), dataInputStream.readFloat(), dataInputStream.readInt());
        }
        return new Square(n, n2, n3, n4, n5, l, l2, n6, n7, f, f2, nArray, reachedArray, dataInputStream.readInt());
    }

    private LightRecording() {
    }

    record Recording(float x, float y, float z, Map<String, Float> world, List<Lamp> lamps, List<RoomLight> rooms, List<Torch> torches, List<Square> squares) {
    }

    record Square(int x, int y, int z, int flags, int passes, long building, long room, int lightLevel, int light, float dark, float targetDark, int[] corners, Reached[] reached, int ours) {
    }

    record Lamp(int id, int x, int y, int z, int radius, float r, float g, float b, boolean active, int life, boolean hydroPowered, long building, boolean streetLight, int switches) {
    }

    record RoomLight(int id, int x, int y, int z, int width, int height, boolean active, long room, long building) {
    }

    record Torch(int id, float x, float y, float z, float r, float g, float b, float angleX, float angleY, float dist, float strength, boolean cone, float dot, int focusing) {
    }

    record Reached(int id, int x, int y, int z, int radius, float r, float g, float b, int flags) {
    }
}

