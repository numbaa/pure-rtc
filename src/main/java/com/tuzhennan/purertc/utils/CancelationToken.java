package com.tuzhennan.purertc.utils;

public class CancelationToken {

    private boolean cancel = false;

    public void set() {
        cancel = true;
    }

    public boolean hasSet() {
        return cancel;
    }
}
