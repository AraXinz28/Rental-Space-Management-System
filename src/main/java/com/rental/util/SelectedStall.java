package com.rental.util;

import com.rental.model.Stall;

public class SelectedStall {

    private static Stall stall;

    public static void set(Stall s) {
        stall = s;
    }

    public static Stall get() {
        return stall;
    }
}
