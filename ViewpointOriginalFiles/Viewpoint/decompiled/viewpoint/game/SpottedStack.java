/*
 * Decompiled with CFR 0.152.
 */
package viewpoint.game;

import java.util.HashSet;
import java.util.Stack;
import viewpoint.platform.LiveSettings;
import zombie.characters.IsoPlayer;
import zombie.iso.IsoMovingObject;

public final class SpottedStack<E>
extends Stack<E> {
    public static final LiveSettings.Toggle ON = LiveSettings.toggle("game.spottedStack", "Characters seen lately found at once (line of sight)", "Game/Fixes", true);
    private static final int STALE = -1;
    private HashSet<Object> held = new HashSet();
    private int indexed = -1;

    public static void use(IsoPlayer isoPlayer) {
        Stack<IsoMovingObject> stack = isoPlayer.getLastSpotted();
        boolean bl = ON.get();
        if (bl == stack instanceof SpottedStack) {
            return;
        }
        Stack stack2 = bl ? new SpottedStack() : new Stack();
        stack2.addAll(stack);
        isoPlayer.setLastSpotted(stack2);
    }

    @Override
    public synchronized boolean contains(Object object) {
        if (this.indexed != this.modCount) {
            this.held.clear();
            this.held.addAll(this);
            this.indexed = this.modCount;
        }
        return this.held.contains(object);
    }

    @Override
    public synchronized boolean add(E e) {
        boolean bl = this.indexed == this.modCount;
        super.add(e);
        if (bl) {
            this.held.add(e);
            this.indexed = this.modCount;
        }
        return true;
    }

    @Override
    public synchronized E set(int n, E e) {
        this.indexed = -1;
        return super.set(n, e);
    }

    @Override
    public synchronized void setElementAt(E e, int n) {
        this.indexed = -1;
        super.setElementAt(e, n);
    }

    @Override
    public synchronized Object clone() {
        SpottedStack spottedStack = (SpottedStack)super.clone();
        spottedStack.held = new HashSet();
        spottedStack.indexed = -1;
        return spottedStack;
    }
}

