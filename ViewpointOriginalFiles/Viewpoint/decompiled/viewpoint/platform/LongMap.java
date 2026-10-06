/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.platform;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Predicate;

public final class LongMap<V> {
    private static final Object TOMBSTONE = new Object();
    private long[] keys;
    private Object[] values;
    private int size;
    private int used;
    private int mask;

    public LongMap() {
        this(16);
    }

    public LongMap(int n) {
        int n2 = Integer.highestOneBit(Math.max(8, n * 2 - 1)) << 1;
        this.keys = new long[n2];
        this.values = new Object[n2];
        this.mask = n2 - 1;
    }

    public int size() {
        return this.size;
    }

    public boolean isEmpty() {
        return this.size == 0;
    }

    public Iterable<V> values() {
        return () -> new Iterator<V>(){
            private int next;
            private int last;
            {
                this.next = LongMap.this.advance(0);
                this.last = -1;
            }

            @Override
            public boolean hasNext() {
                return this.next < LongMap.this.keys.length;
            }

            @Override
            public V next() {
                if (this.next >= LongMap.this.keys.length) {
                    throw new NoSuchElementException();
                }
                this.last = this.next;
                this.next = LongMap.this.advance(this.next + 1);
                return LongMap.this.valueAt(this.last);
            }

            @Override
            public void remove() {
                LongMap.this.removeAt(this.last);
            }
        };
    }

    public void removeIf(Predicate<V> predicate) {
        for (int i = 0; i < this.keys.length; ++i) {
            V v = this.valueAt(i);
            if (v == null || !predicate.test(v)) continue;
            this.removeAt(i);
        }
    }

    private int advance(int n) {
        while (n < this.keys.length && this.valueAt(n) == null) {
            ++n;
        }
        return n;
    }

    public V get(long l) {
        int n = this.index(l);
        Object object;
        while ((object = this.values[n]) != null) {
            if (object != TOMBSTONE && this.keys[n] == l) {
                return (V)object;
            }
            n = n + 1 & this.mask;
        }
        return null;
    }

    public V put(long l, V v) {
        if (v == null) {
            throw new IllegalArgumentException("null value");
        }
        int n = -1;
        int n2 = this.index(l);
        while (true) {
            Object object;
            if ((object = this.values[n2]) == null) {
                if (n < 0) {
                    n = n2;
                    ++this.used;
                }
                this.keys[n] = l;
                this.values[n] = v;
                ++this.size;
                if (this.used * 2 > this.keys.length) {
                    this.rehash(this.size * 2 > this.keys.length / 2 ? this.keys.length * 2 : this.keys.length);
                }
                return null;
            }
            if (object == TOMBSTONE) {
                n = n < 0 ? n2 : n;
            } else if (this.keys[n2] == l) {
                this.values[n2] = v;
                return (V)object;
            }
            n2 = n2 + 1 & this.mask;
        }
    }

    public V remove(long l) {
        int n = this.index(l);
        Object object;
        while ((object = this.values[n]) != null) {
            if (object != TOMBSTONE && this.keys[n] == l) {
                this.values[n] = TOMBSTONE;
                --this.size;
                return (V)object;
            }
            n = n + 1 & this.mask;
        }
        return null;
    }

    public void clear() {
        Arrays.fill(this.values, null);
        this.used = 0;
        this.size = 0;
    }

    public int capacity() {
        return this.keys.length;
    }

    public V valueAt(int n) {
        Object object = this.values[n];
        return (V)(object == TOMBSTONE ? null : object);
    }

    public long keyAt(int n) {
        return this.keys[n];
    }

    public void removeAt(int n) {
        if (this.values[n] != null && this.values[n] != TOMBSTONE) {
            this.values[n] = TOMBSTONE;
            --this.size;
        }
    }

    private int index(long l) {
        long l2 = l;
        l2 ^= l2 >>> 33;
        l2 *= -49064778989728563L;
        l2 ^= l2 >>> 33;
        l2 *= -4265267296055464877L;
        l2 ^= l2 >>> 33;
        return (int)l2 & this.mask;
    }

    private void rehash(int n) {
        long[] lArray = this.keys;
        Object[] objectArray = this.values;
        this.keys = new long[n];
        this.values = new Object[n];
        this.mask = n - 1;
        this.used = 0;
        this.size = 0;
        for (int i = 0; i < lArray.length; ++i) {
            if (objectArray[i] == null || objectArray[i] == TOMBSTONE) continue;
            this.insertFresh(lArray[i], objectArray[i]);
        }
    }

    private void insertFresh(long l, Object object) {
        int n = this.index(l);
        while (this.values[n] != null) {
            n = n + 1 & this.mask;
        }
        this.keys[n] = l;
        this.values[n] = object;
        ++this.size;
        ++this.used;
    }
}

