package com.ecanaveras.gde.waudio.models;

import android.graphics.Bitmap;

/**
 * Created by ecanaveras on 04/08/2017.
 */

public class Template {

    private String name;
    private int thumbnail;
    private Bitmap img_thumbnail;
    private String category;
    private String pathTemplateMp4;

    public Template(String name, String pathTemplateMp4) {
        this.name = name;
        this.pathTemplateMp4 = pathTemplateMp4;
    }

    public Template(String name, String filePathMp4, String category) {
        this.name = name;
        this.pathTemplateMp4 = filePathMp4;
        this.category = category;
    }

    public Template(String name, int thumbnail) {
        this.name = name;
        this.thumbnail = thumbnail;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(int thumbnail) {
        this.thumbnail = thumbnail;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getPathTemplateMp4() {
        return pathTemplateMp4;
    }

    public void setPathTemplateMp4(String pathTemplateMp4) {
        this.pathTemplateMp4 = pathTemplateMp4;
    }
}
