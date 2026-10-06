/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.far;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.concurrent.Executor;

final class FarColours {
    private static volatile HashMap<String, Integer> table;
    private static volatile boolean loading;

    static boolean ready() {
        return table != null;
    }

    static void load(Executor executor) {
        if (loading) {
            return;
        }
        loading = true;
        executor.execute(() -> {
            HashMap<String, Integer> hashMap = new HashMap<String, Integer>(65536);
            try (InputStream inputStream = FarColours.class.getResourceAsStream("/viewpoint/far-colours.txt");){
                if (inputStream == null) {
                    System.out.println("[Viewpoint] far-colours.txt missing from the jar: the far world takes the map's colours");
                } else {
                    String string;
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                    while ((string = bufferedReader.readLine()) != null) {
                        String[] stringArray = string.split(" ");
                        if (stringArray.length != 4) continue;
                        hashMap.putIfAbsent(stringArray[0], Integer.parseInt(stringArray[1]) << 16 | Integer.parseInt(stringArray[2]) << 8 | Integer.parseInt(stringArray[3]));
                    }
                }
            }
            catch (Exception exception) {
                System.out.println("[Viewpoint] far-colours.txt unreadable: " + String.valueOf(exception));
            }
            table = hashMap;
        });
    }

    static int of(String string) {
        HashMap<String, Integer> hashMap = table;
        Integer n = hashMap == null ? null : hashMap.get(string);
        return n == null ? -1 : n;
    }

    private FarColours() {
    }
}

