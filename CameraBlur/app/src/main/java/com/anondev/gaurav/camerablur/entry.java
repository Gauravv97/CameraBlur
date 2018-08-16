package com.anondev.gaurav.camerablur;

import java.io.Serializable;

public class entry implements Serializable {
    int _id;
    String path;

    public int get_id() {
        return _id;
    }

    public void set_id(int _id) {
        this._id = _id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
