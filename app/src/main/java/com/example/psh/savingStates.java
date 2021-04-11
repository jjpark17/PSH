package com.example.psh;

import java.io.Serializable;

public class savingStates implements Serializable {
    public String name;
    public String description;
    public int id;

    public boolean cache_save;
    public String cache;

    public savingStates() {
        id = -1;
        name = "";
        description = "";
        cache_save = false;
        cache = "";
    }
}
