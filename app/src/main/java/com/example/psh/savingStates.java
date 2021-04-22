package com.example.psh;

import java.io.Serializable;

public class savingStates implements Serializable {
    public String name = "";
    public String description= "";

    public int id;
    public boolean sound_save = false;
    public String sound_path = null;
    public boolean cache_save = false;
    public String cache_path = null;;
    public boolean is_active = false;

    public savingStates() {
    }
}
