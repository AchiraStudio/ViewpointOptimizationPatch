/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.platform;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntPredicate;
import viewpoint.platform.KeyChord;
import viewpoint.platform.KeyInput;
import viewpoint.platform.LiveSettings;

public final class KeyBind
extends LiveSettings.Setting {
    private static final AtomicInteger changes = new AtomicInteger();
    final Kind kind;
    final boolean dev;
    final List<KeyChord> fallback;
    private volatile List<KeyChord> chords;

    KeyBind(String string, String string2, String string3, Kind kind, boolean bl, String string4) {
        super(string, string2, string3);
        this.kind = kind;
        this.dev = bl;
        this.fallback = KeyChord.parse(string4);
        this.chords = this.fallback;
    }

    public boolean pressed() {
        return KeyInput.pressed(this);
    }

    public boolean down(IntPredicate intPredicate) {
        for (KeyChord keyChord : this.chords) {
            if (!intPredicate.test(keyChord.key()) || !keyChord.heldDown(intPredicate)) continue;
            return true;
        }
        return false;
    }

    public List<KeyChord> chords() {
        return this.chords;
    }

    public boolean bound() {
        return !this.chords.isEmpty();
    }

    public String display() {
        return KeyChord.display(this.chords, KeyChord::gameName);
    }

    String windowText() {
        return KeyChord.display(this.chords, KeyChord::latinName);
    }

    void set(List<KeyChord> list) {
        this.chords = List.copyOf(list);
        changes.incrementAndGet();
    }

    static int changes() {
        return changes.get();
    }

    @Override
    String text() {
        return KeyChord.text(this.chords);
    }

    @Override
    void read(String string) {
        try {
            this.set(KeyChord.parse(string));
        }
        catch (IllegalArgumentException illegalArgumentException) {
            System.out.println("[Viewpoint] key " + this.key + ": " + illegalArgumentException.getMessage() + " in \"" + string + "\"; kept " + this.text());
        }
    }

    @Override
    boolean matches(String string) {
        try {
            return KeyChord.parse(string).equals(this.chords);
        }
        catch (IllegalArgumentException illegalArgumentException) {
            return false;
        }
    }

    public static enum Kind {
        PRESS,
        MENU,
        HOLD;

    }
}

