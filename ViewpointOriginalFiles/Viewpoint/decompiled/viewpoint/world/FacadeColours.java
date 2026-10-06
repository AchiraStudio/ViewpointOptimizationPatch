/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.world;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public final class FacadeColours {
    private static HashMap<String, Integer> table;

    public static int of(String string) {
        Integer n;
        if (table == null) {
            FacadeColours.load();
        }
        return (n = table.get(string)) == null ? -1 : n;
    }

    private static void load() {
        table = new HashMap();
        try (InputStream inputStream = FacadeColours.class.getResourceAsStream("/viewpoint/facade-colours.txt");){
            String string;
            if (inputStream == null) {
                System.out.println("[Viewpoint] facade-colours.txt missing from the jar: hidden wall faces get a neutral grey");
                return;
            }
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            while ((string = bufferedReader.readLine()) != null) {
                String[] stringArray = string.split(" ");
                if (stringArray.length != 4) continue;
                table.put(stringArray[0], Integer.parseInt(stringArray[1]) << 16 | Integer.parseInt(stringArray[2]) << 8 | Integer.parseInt(stringArray[3]));
            }
        }
        catch (Exception exception) {
            System.out.println("[Viewpoint] facade-colours.txt unreadable: " + String.valueOf(exception));
        }
    }

    private FacadeColours() {
    }
}

