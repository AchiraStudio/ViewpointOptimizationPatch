/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.iso.IsoLot
 *  zombie.iso.LotHeader
 *  zombie.iso.MapFiles
 */
package viewpoint.far;

import java.io.File;
import java.nio.file.Files;
import java.util.concurrent.ConcurrentLinkedQueue;
import viewpoint.far.BiomeTrees;
import viewpoint.far.FarCell;
import viewpoint.far.FarMesher;
import viewpoint.far.FarTiles;
import viewpoint.far.FarWorkers;
import viewpoint.far.FarWorld;
import viewpoint.far.Pyramid;
import viewpoint.platform.Caches;
import viewpoint.render.FarGpu;
import zombie.iso.IsoLot;
import zombie.iso.LotHeader;
import zombie.iso.MapFiles;

final class CellJobs {
    private static final ConcurrentLinkedQueue<Done> DONE = new ConcurrentLinkedQueue();

    static Done poll() {
        return DONE.poll();
    }

    static boolean start(FarWorld.Slot slot, long l) {
        String string;
        int n = FarWorld.detail(slot.distance);
        if (slot.height != null) {
            CellJobs.remesh(slot, n);
            return true;
        }
        MapFiles mapFiles = CellJobs.mapFiles(slot.cellX, slot.cellY);
        LotHeader lotHeader = mapFiles == null ? null : (LotHeader)mapFiles.infoHeaders.get(slot.cellX + "_" + slot.cellY + ".lotheader");
        String string2 = string = mapFiles == null ? null : (String)mapFiles.infoFileNames.get("world_" + slot.cellX + "_" + slot.cellY + ".lotpack");
        if (lotHeader == null || string == null) {
            slot.busy = true;
            return true;
        }
        FarTiles.Tile[] tileArray = FarTiles.table(lotHeader, l);
        if (tileArray == null) {
            return false;
        }
        Pyramid pyramid = Pyramid.of(mapFiles.mapDirectoryAbsolutePath);
        BiomeTrees.Table table = BiomeTrees.table();
        int n2 = lotHeader.minLevel;
        int n3 = lotHeader.maxLevel;
        slot.lotpack = string;
        slot.minLevel = n2;
        slot.maxLevel = n3;
        slot.tiles = tileArray;
        slot.legend = BiomeTrees.legend(lotHeader);
        slot.busy = true;
        slot.used = Caches.tick();
        FarWorkers.jobs.incrementAndGet();
        FarWorkers.POOL.execute(() -> {
            try {
                byte[] byArray = Files.readAllBytes(new File(string).toPath());
                FarCell farCell = FarCell.read(slot.cellX, slot.cellY, byArray, n2, n3, tileArray, pyramid.image(slot.cellX, slot.cellY), BiomeTrees.image(mapFiles.mapDirectoryAbsolutePath, slot.cellX, slot.cellY), table, slot.legend);
                DONE.add(Done.read(slot, farCell, new FarGpu.Mesh(n, FarMesher.mesh(farCell.height, n))));
            }
            catch (Throwable throwable) {
                System.out.println("[Viewpoint] far world: reading cell " + slot.cellX + "," + slot.cellY + " failed: " + String.valueOf(throwable));
                DONE.add(Done.failed(slot));
            }
            finally {
                FarWorkers.jobs.decrementAndGet();
            }
        });
        return true;
    }

    private static void remesh(FarWorld.Slot slot, int n) {
        short[] sArray = slot.height;
        slot.busy = true;
        slot.used = Caches.tick();
        FarWorkers.jobs.incrementAndGet();
        FarWorkers.POOL.execute(() -> {
            try {
                DONE.add(Done.meshed(slot, new FarGpu.Mesh(n, FarMesher.mesh(sArray, n))));
            }
            catch (Throwable throwable) {
                System.out.println("[Viewpoint] far world: meshing cell " + slot.cellX + "," + slot.cellY + " failed: " + String.valueOf(throwable));
                DONE.add(Done.failed(slot));
            }
            finally {
                FarWorkers.jobs.decrementAndGet();
            }
        });
    }

    static MapFiles mapFiles(int n, int n2) {
        String string = "world_" + n + "_" + n2 + ".lotpack";
        for (MapFiles mapFiles : IsoLot.MapFiles) {
            if (!mapFiles.infoFileNames.containsKey(string)) continue;
            return mapFiles;
        }
        return null;
    }

    private CellJobs() {
    }

    record Done(FarWorld.Slot slot, FarCell cell, FarGpu.Mesh mesh, FarGpu.Colours colours, FarGpu.Trees trees, boolean failed) {
        static Done read(FarWorld.Slot slot, FarCell farCell, FarGpu.Mesh mesh) {
            return new Done(slot, farCell, mesh, new FarGpu.Colours(farCell.top, farCell.side), new FarGpu.Trees(farCell.trees, farCell.treesBelow), false);
        }

        static Done meshed(FarWorld.Slot slot, FarGpu.Mesh mesh) {
            return new Done(slot, null, mesh, null, null, false);
        }

        static Done failed(FarWorld.Slot slot) {
            return new Done(slot, null, null, null, null, true);
        }

        boolean uploaded() {
            return !(!this.mesh.uploaded() || this.colours != null && !this.colours.uploaded() || this.trees != null && !this.trees.uploaded());
        }
    }
}

