package com.ecanaveras.gde.waudio.models;

import android.graphics.Bitmap;

import java.text.DecimalFormat;
import java.util.Date;

/**
 * Created by ecanaveras on 04/08/2017.
 */

public class WaudioModel {

    private String name;
    private String urlThumbnail;
    private Bitmap imgThumbnail;
    private String category;
    private String pathMp4;
    private long dateModified;
    private long size;
    private Date date;
    private String sizeFormat;

    public WaudioModel() {
    }

    public WaudioModel(String name) {
        this.name = name;
    }

    public WaudioModel(String name, String pathMp4) {
        this.name = name;
        this.pathMp4 = pathMp4;
    }

    public WaudioModel(String name, String pathMp4, long dateModified, long size) {
        this.name = name;
        this.pathMp4 = pathMp4;
        this.dateModified = dateModified;
        this.size = size;
    }

    public WaudioModel(String name, String filePathMp4, String category) {
        this.name = name;
        this.pathMp4 = filePathMp4;
        this.category = category;
    }

    public static String readableFileSize(long size) {
        if (size <= 0) return "0";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    public WaudioModel(String name, int thumbnail) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getPathMp4() {
        return pathMp4;
    }

    public void setPathMp4(String pathMp4) {
        this.pathMp4 = pathMp4;
    }

    public long getDateModified() {
        return dateModified;
    }

    public Date getDate() {
        if (dateModified != 0)
            return new Date(dateModified * 1000L);
        return null;
    }

    public void setDateModified(long dateModified) {
        this.dateModified = dateModified;
    }

    public long getSize() {
        return size;
    }

    public String getSizeFormat() {
        return readableFileSize(size);
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getUrlThumbnail() {
        return urlThumbnail;
    }

    public void setUrlThumbnail(String urlThumbnail) {
        this.urlThumbnail = urlThumbnail;
    }

    public Bitmap getImgThumbnail() {
        return imgThumbnail;
    }

    public void setImgThumbnail(Bitmap imgThumbnail) {
        this.imgThumbnail = imgThumbnail;
    }
}
