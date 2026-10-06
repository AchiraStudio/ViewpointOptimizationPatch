/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.ZomboidFileSystem
 */
package viewpoint.platform;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import zombie.ZomboidFileSystem;

public final class LiveSettings {
    public static final String FILE = "viewpoint-live.properties";
    private static final List<Setting> ALL = Collections.synchronizedList(new ArrayList());
    private static final Map<String, String> pending = new HashMap<String, String>();
    private static final Map<String, String> carried = new HashMap<String, String>();
    private static volatile boolean loaded;
    private static volatile Properties saved;

    public static Number number(String string, String string2, String string3, float f, float f2, float f3, float f4) {
        return LiveSettings.add(new Number(string, string2, string3, f, f2, f3, f4));
    }

    public static Toggle toggle(String string, String string2, String string3, boolean bl) {
        return LiveSettings.add(new Toggle(string, string2, string3, bl));
    }

    public static Choice choice(String string, String string2, String string3, String[] stringArray, int n) {
        return LiveSettings.choice(string, string2, string3, stringArray, new String[0], n);
    }

    public static Choice choice(String string, String string2, String string3, String[] stringArray, String[] stringArray2, int n) {
        return LiveSettings.add(new Choice(string, string2, string3, stringArray, stringArray2, n));
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static <T extends Setting> T add(T t) {
        String string;
        List<Setting> list = ALL;
        synchronized (list) {
            for (Setting setting : ALL) {
                if (!setting.key.equals(t.key)) continue;
                System.out.println("[Viewpoint] setting " + t.key + " declared twice: the window lists both");
            }
            ALL.add(t);
            string = pending.remove(t.key);
        }
        if (loaded) {
            LiveSettings.apply(t, saved);
        }
        if (string != null) {
            t.read(string);
        }
        return t;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void remove(Setting setting) {
        List<Setting> list = ALL;
        synchronized (list) {
            ALL.remove(setting);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static List<Setting> all() {
        List<Setting> list = ALL;
        synchronized (list) {
            return new ArrayList<Setting>(ALL);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static Setting find(String string) {
        List<Setting> list = ALL;
        synchronized (list) {
            for (Setting setting : ALL) {
                if (!setting.key.equals(string)) continue;
                return setting;
            }
            return null;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void put(String string, String string2) {
        Setting setting;
        List<Setting> list = ALL;
        synchronized (list) {
            setting = LiveSettings.find(string);
            if (setting == null) {
                pending.put(string, string2);
                return;
            }
        }
        setting.read(string2);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static String value(String string) {
        List<Setting> list = ALL;
        synchronized (list) {
            Setting setting = LiveSettings.find(string);
            if (setting != null) {
                return setting.text();
            }
            String string2 = pending.get(string);
            String string3 = string2 != null ? string2 : saved.getProperty(string);
            return string3;
        }
    }

    static String saved(String string) {
        return saved.getProperty(string);
    }

    static boolean forget(Predicate<String> predicate) {
        Properties properties = new Properties();
        saved.forEach((BiConsumer<? super Object, ? super Object>)((BiConsumer<Object, Object>)(object, object2) -> {
            if (!predicate.test((String)object)) {
                properties.put(object, object2);
            }
        }));
        boolean bl = properties.size() < saved.size();
        saved = properties;
        return bl;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void carryOver(String string, String string2) {
        List<Setting> list = ALL;
        synchronized (list) {
            carried.put(string, string2);
        }
        if (loaded && saved.getProperty(string) == null) {
            LiveSettings.put(string, string2);
        }
    }

    static boolean load() {
        if (loaded) {
            return false;
        }
        LiveSettings.load(LiveSettings.read());
        return true;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void load(Properties properties) {
        HashMap<String, String> hashMap;
        saved = properties;
        loaded = true;
        for (Setting iterator2 : LiveSettings.all()) {
            LiveSettings.apply(iterator2, saved);
        }
        List<Setting> list = ALL;
        synchronized (list) {
            hashMap = new HashMap<String, String>(carried);
        }
        for (Map.Entry entry : hashMap.entrySet()) {
            if (saved.getProperty((String)entry.getKey()) != null) continue;
            LiveSettings.put((String)entry.getKey(), (String)entry.getValue());
        }
    }

    private static void apply(Setting setting, Properties properties) {
        String string = properties.getProperty(setting.key);
        if (string != null) {
            setting.read(string);
        }
    }

    private static Properties read() {
        Properties properties = new Properties();
        File file = LiveSettings.file();
        if (file.isFile()) {
            try (FileInputStream fileInputStream = new FileInputStream(file);){
                properties.load(fileInputStream);
            }
            catch (Exception exception) {
                System.out.println("[Viewpoint] could not read " + String.valueOf(file) + ": " + String.valueOf(exception));
            }
        }
        return properties;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void save() {
        Properties properties = new Properties();
        properties.putAll((Map<?, ?>)saved);
        Object object = ALL;
        synchronized (object) {
            properties.putAll(pending);
        }
        for (Setting setting : LiveSettings.all()) {
            properties.setProperty(setting.key, setting.text());
        }
        saved = properties;
        try {
            object = new FileOutputStream(LiveSettings.file());
            try {
                properties.store((OutputStream)object, "Viewpoint's live settings: the settings window (Delete) writes this file. Edit it by hand only with the game closed.");
            }
            finally {
                ((OutputStream)object).close();
            }
        }
        catch (Exception exception) {
            System.out.println("[Viewpoint] could not write " + String.valueOf(LiveSettings.file()) + ": " + String.valueOf(exception));
        }
    }

    private static File file() {
        return new File(ZomboidFileSystem.instance.getCacheDir(), FILE);
    }

    private LiveSettings() {
    }

    static {
        saved = new Properties();
    }

    public static final class Number
    extends Setting {
        final float min;
        final float max;
        final float step;
        final float fallback;
        private volatile float value;

        Number(String string, String string2, String string3, float f, float f2, float f3, float f4) {
            super(string, string2, string3);
            this.min = f;
            this.max = f2;
            this.step = f3;
            this.fallback = f4;
            this.value = f4;
        }

        public float get() {
            return this.value;
        }

        public int getInt() {
            return Math.round(this.value);
        }

        public void set(float f) {
            this.value = this.snap(f);
        }

        private float snap(float f) {
            float f2 = this.step > 0.0f ? (float)Math.round((f - this.min) / this.step) * this.step + this.min : f;
            return Math.max(this.min, Math.min(this.max, f2));
        }

        @Override
        String text() {
            return this.step >= 1.0f ? Integer.toString(this.getInt()) : Float.toString(this.value);
        }

        @Override
        void read(String string) {
            try {
                this.set(Float.parseFloat(string.trim()));
            }
            catch (NumberFormatException numberFormatException) {
                System.out.println("[Viewpoint] viewpoint-live.properties: " + this.key + " is not a number: " + string);
            }
        }

        @Override
        boolean matches(String string) {
            try {
                return Math.abs(this.snap(Float.parseFloat(string.trim())) - this.value) <= 1.0E-5f * Math.max(1.0f, this.value);
            }
            catch (NumberFormatException numberFormatException) {
                return false;
            }
        }
    }

    /*
     * Uses 'sealed' constructs - enablewith --sealed true
     */
    public static abstract class Setting {
        final String key;
        final String label;
        final String section;
        private volatile BooleanSupplier shown = () -> true;
        private volatile String tooltip = "";
        private volatile List<String> also = List.of();

        Setting(String string, String string2, String string3) {
            this.key = string;
            this.label = string2;
            this.section = string3;
        }

        abstract String text();

        abstract void read(String var1);

        abstract boolean matches(String var1);

        public void shownWhen(BooleanSupplier booleanSupplier) {
            this.shown = booleanSupplier;
        }

        boolean shown() {
            return this.shown.getAsBoolean();
        }

        public void describe(String string) {
            this.tooltip = string;
        }

        String tooltip() {
            return this.tooltip;
        }

        public void alsoIn(String string) {
            ArrayList<String> arrayList = new ArrayList<String>(this.also);
            arrayList.add(string);
            this.also = List.copyOf(arrayList);
        }

        List<String> also() {
            return this.also;
        }
    }

    public static final class Toggle
    extends Setting {
        final boolean fallback;
        private volatile boolean value;

        Toggle(String string, String string2, String string3, boolean bl) {
            super(string, string2, string3);
            this.fallback = bl;
            this.value = bl;
        }

        public boolean get() {
            return this.value;
        }

        public void set(boolean bl) {
            this.value = bl;
        }

        @Override
        String text() {
            return Boolean.toString(this.value);
        }

        @Override
        void read(String string) {
            this.value = Boolean.parseBoolean(string.trim());
        }

        @Override
        boolean matches(String string) {
            return Boolean.parseBoolean(string.trim()) == this.value;
        }
    }

    public static final class Choice
    extends Setting {
        final String[] options;
        final int fallback;
        private final String[] former;
        private volatile int value;
        private volatile String[] labels;
        private volatile boolean slider;

        Choice(String string, String string2, String string3, String[] stringArray, String[] stringArray2, int n) {
            super(string, string2, string3);
            this.options = (String[])stringArray.clone();
            this.former = (String[])stringArray2.clone();
            this.fallback = n;
            this.value = n;
        }

        public int get() {
            return this.value;
        }

        public void shownAs(String[] stringArray) {
            this.labels = stringArray.length == this.options.length ? (String[])stringArray.clone() : null;
        }

        public void asSlider() {
            this.slider = true;
        }

        boolean slider() {
            return this.slider;
        }

        String[] labels() {
            String[] stringArray = this.labels;
            return stringArray != null ? stringArray : this.options;
        }

        void set(int n) {
            if (n >= 0 && n < this.options.length) {
                this.value = n;
            }
        }

        @Override
        String text() {
            return this.options[this.value];
        }

        @Override
        void read(String string) {
            int n = Choice.index(this.options, string);
            if (n < 0) {
                n = Choice.index(this.former, string);
            }
            if (n >= 0) {
                this.value = n;
                return;
            }
            System.out.println("[Viewpoint] viewpoint-live.properties: " + this.key + " is none of its options: " + string);
        }

        private static int index(String[] stringArray, String string) {
            for (int i = 0; i < stringArray.length; ++i) {
                if (!stringArray[i].equalsIgnoreCase(string.trim())) continue;
                return i;
            }
            return -1;
        }

        @Override
        boolean matches(String string) {
            return this.options[this.value].equalsIgnoreCase(string.trim());
        }
    }
}

