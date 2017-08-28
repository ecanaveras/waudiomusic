package com.ecanaveras.gde.waudio.util;

import java.io.File;
import java.io.FilenameFilter;

/**
 * Created by ecanaveras on 10/08/2017.
 */

public class Mp4Filter implements FilenameFilter {

    String extesion;

    public Mp4Filter(String ext) {
        extesion = ext;
    }

    @Override
    public boolean accept(File file, String name) {
        return name.toLowerCase().endsWith(extesion);// && name.contains("_");
    }
}