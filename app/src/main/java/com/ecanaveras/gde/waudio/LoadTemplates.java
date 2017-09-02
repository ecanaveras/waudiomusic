package com.ecanaveras.gde.waudio;

import android.provider.MediaStore;

import com.ecanaveras.gde.waudio.models.WaudioModel;
import com.ecanaveras.gde.waudio.task.FindVideoThumbnail;
import com.ecanaveras.gde.waudio.util.Mp4Filter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ecanaveras on 05/08/2017.
 */

public class LoadTemplates {

    private List<WaudioModel> waudioModelList = new ArrayList<WaudioModel>();
    private String extension;

    public static void main(String[] args) {
        //getExternalFilesDir(null);
        File dir = new File("D:\\DEV\\tmp\\mp4parser");
        WaudioModel waudioModel;
        if (dir.exists()) {
            for (String name : dir.list(new Mp4Filter(".mp4"))) {
                boolean category = name.split("_").length > 1;
                System.out.println("Split:" + name.split("_")[0]);
                String path = dir.getAbsolutePath() + "\\" + name;
                waudioModel = new WaudioModel(name.split("_")[0], path, category ? name.split("_")[1] : "General");

                /*if (name.contains("_")) {
                    System.out.println("Name:" + name.split("_")[0] + " Category:" + name.split("_")[1].replace(".mp4", "") + " filename:" + name + " path:" + dir.getAbsolutePath() + "\\" + name);
                }*/


            }
        }
    }

    public LoadTemplates(String extension, String directoryPath) {
        this.extension = extension;
        loads(directoryPath);
    }

    private void loads(String directoryPath) {
        File dir = new File(directoryPath);
        WaudioModel waudioModel;
        if (dir.exists()) {
            for (String name : dir.list(new Mp4Filter(".mp4"))) {
                boolean category = name.split("_").length > 1;
                String path = dir.getAbsolutePath() + "/" + name;
                waudioModel = new WaudioModel(name.split("_")[0], path, category ? name.split("_")[1].replace(".mp4", "") : "General");
                waudioModelList.add(waudioModel);
                /*if (name.contains("_")) {
                    System.out.println("Name:" + name.split("_")[0] + " Category:" + name.split("_")[1].replace(".mp4", "") + " filename:" + name + " path:" + dir.getAbsolutePath() + "\\" + name);
                }*/
            }
        }
    }

    public void clearTemplates() {
        waudioModelList.clear();
    }


    public List<WaudioModel> getWaudioModelList() {
        return waudioModelList;
    }
}
