package com.example.psh;

import java.io.Serializable;

public class savingStates implements Serializable {
    public String name;
    public String description;
    public int id;

    public boolean sound_save;
    public String sound;
    public boolean cache_save;
    public String cache;

    public savingStates() {
        id = -1;
        name = "";
        description = "";
        sound_save = false;
        sound = "";
        cache_save = false;
        cache = "";
    }
}
