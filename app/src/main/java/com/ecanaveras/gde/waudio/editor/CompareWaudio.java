package com.ecanaveras.gde.waudio.editor;

import java.io.File;

/**
 * Created by ecanaveras on 08/08/2017.
 */

public class CompareWaudio {

    private String title;
    private String template;
    private File pathWaudio;
    private double entTime;

    public CompareWaudio(String title, String template, double entTime) {
        this.title = title;
        this.template = template;
        this.entTime = entTime;
    }

    public CompareWaudio(String title, String template, File pathWaudio, double entTime) {
        this.title = title;
        this.template = template;
        this.pathWaudio = pathWaudio;
        this.entTime = entTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CompareWaudio that = (CompareWaudio) o;

        if (Double.compare(that.entTime, entTime) != 0) return false;
        if (title != null ? !title.equals(that.title) : that.title != null) return false;
        return template != null ? template.equals(that.template) : that.template == null;

    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = title != null ? title.hashCode() : 0;
        result = 31 * result + (template != null ? template.hashCode() : 0);
        temp = Double.doubleToLongBits(entTime);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public double getEntTime() {
        return entTime;
    }

    public void setEntTime(double entTime) {
        this.entTime = entTime;
    }

    public File getPathWaudio() {
        return pathWaudio;
    }

    public void setPathWaudio(File pathWaudio) {
        this.pathWaudio = pathWaudio;
    }
}
