package com.ecanaveras.gde.waudio.editor;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;

import com.ecanaveras.gde.waudio.MainApp;
import com.ecanaveras.gde.waudio.R;
import com.ecanaveras.gde.waudio.firebase.DataFirebaseHelper;

import org.mp4parser.Container;
import org.mp4parser.muxer.Movie;
import org.mp4parser.muxer.Track;
import org.mp4parser.muxer.builder.DefaultMp4Builder;
import org.mp4parser.muxer.container.mp4.MovieCreator;
import org.mp4parser.muxer.tracks.AppendTrack;
import org.mp4parser.muxer.tracks.ClippedTrack;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.io.StringWriter;
import java.nio.channels.FileChannel;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by ecanaveras on 04/08/2017.
 */

public class GeneratorWaudio implements Serializable {

    private static final String PATH_MEDIA = "/Waudio/Media/";
    private static final String PATH_VIDEOS = "/Waudio Videos/";

    private DataFirebaseHelper mDataFirebaseHelper;

    private final Context context;
    private Thread mSaveSoundFileThread;
    private Handler mHandler;
    private SoundFile mSoundFile;
    private CharSequence title;
    private File outFileM4a;
    private File outFileWaudio;
    private String pathTemplate;

    private double startTime;
    private double endTime;
    private int startFrame;
    private int endFrame;
    private int duration;
    private Thread mLoadSoundFileThread;

    public GeneratorWaudio(Context context, SoundFile soundFile, CharSequence title, double startTime, double endTime, final int startFrame, final int endFrame) {
        this.mSoundFile = soundFile;
        this.title = title;
        this.context = context;
        this.startTime = startTime;
        this.endTime = endTime;
        this.startFrame = startFrame;
        this.endFrame = endFrame;
        this.duration = (int) (endTime - startTime + 0.5);
        mDataFirebaseHelper = new DataFirebaseHelper();
    }


    public void generateWaudio(final String pathTemplate) {
        this.pathTemplate = pathTemplate;
        // Save the sound file in a background thread
        mSaveSoundFileThread = new Thread() {
            public void run() {
                // Try AAC first.
                String outPath = makeM4aFilename(title, ".m4a");
                if (outPath == null) {
                    Runnable runnable = new Runnable() {
                        public void run() {
                            showFinalAlert(new Exception(), R.string.no_unique_filename);
                        }
                    };
                    mHandler.post(runnable);
                    return;
                }
                File outFile = new File(outPath);
                Boolean fallbackToWAV = false;
                try {
                    // Write the new file
                    outFileM4a = outFile;
                    mSoundFile.WriteFile(outFile, startFrame, endFrame - startFrame);
                    //TODO COdigo Elder
                    if (pathTemplate != null) {
                        createWaudio(outFile.getAbsolutePath(), pathTemplate);
                    } else {
                        Runnable runnable = new Runnable() {
                            public void run() {
                                showFinalAlert(new Exception(), "No se ha enviado un plantilla de video");
                            }
                        };
                        mHandler.post(runnable);
                        System.out.println("No se ha enviado un plantilla de video");
                    }
                    //Intent intent = new Intent(context, ListTemplateActivity.class);
                    //intent.putExtra("fileM4a", outFile.getAbsolutePath());
                    //context.startActivity(intent);
                } catch (Exception e) {
                    // log the error and try to create a .wav file instead
                    if (outFile.exists()) {
                        outFile.delete();
                    }
                    StringWriter writer = new StringWriter();
                    e.printStackTrace(new PrintWriter(writer));
                    Log.e("Waudio", "Error: Failed to create " + outPath);
                    Log.e("Waudio", writer.toString());
                    fallbackToWAV = true;
                }

                // Try to create a .wav file if creating a .m4a file failed.
                if (fallbackToWAV) {
                    outPath = makeM4aFilename(title, ".wav");
                    if (outPath == null) {
                        Runnable runnable = new Runnable() {
                            public void run() {
                                showFinalAlert(new Exception(), R.string.no_unique_filename);
                            }
                        };
                        mHandler.post(runnable);
                        return;
                    }
                    outFile = new File(outPath);
                    try {
                        // create the .wav file
                        mSoundFile.WriteWAVFile(outFile, startFrame, endFrame - startFrame);
                    } catch (Exception e) {
                        // Creating the .wav file also failed. Stop the progress dialog, show an
                        // error message and exit.
                        //mProgressDialog.dismiss();
                        if (outFile.exists()) {
                            outFile.delete();
                        }
                        /*mInfoContent = e.toString();
                        runOnUiThread(new Runnable() {
                            public void run() {
                                mInfo.setText(mInfoContent);
                            }
                        });
                        */

                        CharSequence errorMessage;
                        if (e.getMessage() != null
                                && e.getMessage().equals("No space left on device")) {
                            errorMessage = context.getResources().getText(R.string.no_space_error);
                            e = null;
                        } else {
                            errorMessage = context.getResources().getText(R.string.write_error);
                        }
                        final CharSequence finalErrorMessage = errorMessage;
                        final Exception finalException = e;
                        Runnable runnable = new Runnable() {
                            public void run() {
                                showFinalAlert(finalException, finalErrorMessage);
                            }
                        };
                        mHandler.post(runnable);
                        return;
                    }
                }

                // Try to load the new file to make sure it worked
                try {
                    final SoundFile.ProgressListener listener =
                            new SoundFile.ProgressListener() {
                                public boolean reportProgress(double frac) {
                                    // Do nothing - we're not going to try to
                                    // estimate when reloading a saved sound
                                    // since it's usually fast, but hard to
                                    // estimate anyway.
                                    return true;  // Keep going
                                }
                            };
                    SoundFile.create(outPath, listener);
                    //Eliminar Audio
                    if (outFile.exists()) {
                        outFile.delete();
                        System.out.println("Sonido Eliminado...");
                    }
                } catch (final Exception e) {
                    //mProgressDialog.dismiss();
                    e.printStackTrace();
                    /*mInfoContent = e.toString();
                    runOnUiThread(new Runnable() {
                        public void run() {
                            mInfo.setText(mInfoContent);
                        }
                    });*/

                    Runnable runnable = new Runnable() {
                        public void run() {
                            showFinalAlert(e, context.getResources().getText(R.string.write_error));
                        }
                    };
                    mHandler.post(runnable);
                    return;
                }

                //mProgressDialog.dismiss();

                /*final String finalOutPath = outPath;
                Runnable runnable = new Runnable() {
                    public void run() {
                        afterSavingRingtone(title,
                                finalOutPath,
                                duration);
                    }
                };
                mHandler.post(runnable);*/
            }
        };
        mSaveSoundFileThread.start();
    }

    private String makeM4aFilename(CharSequence title, String extension) {
        String subdir;
        String externalRootDir = Environment.getExternalStorageDirectory().getPath();
        if (!externalRootDir.endsWith("/")) {
            externalRootDir += "/";
        }
        subdir = "Waudio/Media/Waudio Audio/";
        String parentdir = externalRootDir + subdir;

        // Create the parent directory
        File parentDirFile = new File(parentdir);
        parentDirFile.mkdirs();

        // If we can't write to that special path, try just writing
        // directly to the sdcard
        if (!parentDirFile.isDirectory()) {
            parentdir = externalRootDir;
        }

        // Turn the title into a filename
        String filename = "";
        for (int i = 0; i < title.length(); i++) {
            if (Character.isLetterOrDigit(title.charAt(i))) {
                filename += title.charAt(i);
            }
        }

        return getUniqueFileName(parentdir, filename, extension);//path;
    }


    /***
     * Crea un Waudio
     * @param audioInFile
     */
    private void createWaudio(String audioInFile, String videoInFile) {
        try {
            Movie listAudio = MovieCreator.build(audioInFile);
            Movie movie = MovieCreator.build(videoInFile);

            //Audio
            long audioDuration = 0;
            for (Track audio : listAudio.getTracks()) {
                if (audio.getHandler().equals("soun")) {
                    movie.addTrack(audio);
                    audioDuration = (audio.getDuration() / audio.getTrackMetaData().getTimescale());
                    System.out.println("AUDIO:" + audio.getHandler() + " Duracion:" + audioDuration + " Scale:" + audio.getTrackMetaData().getTimescale());
                }
            }

            if (audioDuration < 30)
                movie = getShorTracks(movie, 0, audioDuration + 0.8);

            Container mp4File = new DefaultMp4Builder().build(movie);
            File pathDir = getSDPathToFile(PATH_MEDIA, PATH_VIDEOS);
            pathDir.mkdirs();
            String nameWaudio = ((title.length() < 30 ? title : title.subSequence(0, 30)).toString().toUpperCase());
            outFileWaudio = getSDPathToFile(PATH_MEDIA + PATH_VIDEOS, getUniqueFileName(pathDir.getAbsolutePath(), nameWaudio, ".mp4", true));
            FileChannel fc = new FileOutputStream(outFileWaudio).getChannel();
            mp4File.writeContainer(fc);
            fc.close();
            mDataFirebaseHelper.incrementWaudioCreated();
            MainApp app = (MainApp) context.getApplicationContext();
            app.addNewWaudio(new CompareWaudio(getTitle().toString(), getPathTemplate(), getOutFileWaudio(), getEndTime()));
            app.incrementCountWaudioCreated();
            scanWaudioVideos(outFileWaudio);
            Log.i("Waudio", outFileWaudio.getName() + " creado exitosamente");
        } catch (IOException e) {
            e.printStackTrace();
            if (outFileWaudio.exists()) {
                outFileWaudio.delete();
            }
        }
    }

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /* Checks if external storage is available to at least read */
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }

    private File getSDPathToFile(String filePatho, String fileName) {
        String extBaseDir = Environment.getExternalStorageDirectory().getPath();
        if (!extBaseDir.endsWith("/")) {
            extBaseDir += "/";
        }
        if (filePatho == null || filePatho.length() == 0 || filePatho.charAt(0) != '/')
            filePatho = "/" + filePatho;

        //makeDirectory(filePatho);
        File file = new File(extBaseDir + filePatho);
        return new File(file.getAbsolutePath() + "/" + fileName);// file;
    }

    private String getUniqueFileName(String parentdir, String filename, String extension) {
        return getUniqueFileName(parentdir, filename, extension, false);
    }

    /**
     * Devuelve un nombre de archivo unico, para no sobreescribir
     *
     * @param parentdir
     * @param filename
     * @param extension
     * @return
     */
    private String getUniqueFileName(String parentdir, String filename, String extension, Boolean returnOnlyName) {
        String path = null;
        String onlyName = null;
        if (!parentdir.endsWith("/")) {
            parentdir += "/";
        }
        for (int i = 0; i < 100; i++) {
            String testPath;
            if (i > 0) {
                testPath = parentdir + filename + "_" + i + extension;
                onlyName = filename + "_" + i + extension;
            } else {
                testPath = parentdir + filename + extension;
                onlyName = filename + extension;
            }

            try {
                RandomAccessFile f = new RandomAccessFile(new File(testPath), "r");
                f.close();
            } catch (Exception e) {
                // Good, the file didn't exist
                path = testPath;
                if (returnOnlyName)
                    path = onlyName;
                break;
            }
        }
        return path;
    }


    /**
     * Cortar Video
     *
     * @param movie
     * @param startTime
     * @param endTime
     * @return
     * @throws IOException
     */
    private static Movie getShorTracks(Movie movie, double startTime, double endTime) throws IOException {

        List<Track> tracks = movie.getTracks();
        movie.setTracks(new LinkedList<Track>());
        // remove all tracks we will create new tracks from the old

        System.out.println("startTime:" + startTime + " endTime:" + endTime);

        boolean timeCorrected = false;

        // Here we try to find a track that has sync samples. Since we can only start decoding
        // at such a sample we SHOULD make sure that the start of the new fragment is exactly
        // such a frame
        for (Track track : tracks) {
            if (track.getSyncSamples() != null && track.getSyncSamples().length > 0) {
                System.out.println("Video, Duracion real:" + track.getDuration() / track.getTrackMetaData().getTimescale());
                if (timeCorrected) {
                    // This exception here could be a false positive in case we have multiple tracks
                    // with sync samples at exactly the same positions. E.g. a single movie containing
                    // multiple qualities of the same video (Microsoft Smooth Streaming file)

                    throw new RuntimeException("The startTime has already been corrected by another track with SyncSample. Not Supported.");
                }
                //startTime = correctTimeToSyncSample(track, startTime, false);
                //endTime = correctTimeToSyncSample(track, endTime, true);
                System.out.println("TImES: start=" + startTime + " end=" + endTime);
                timeCorrected = true;
            }
        }

        for (Track track : tracks) {
            long currentSample = 0;
            double currentTime = 0;
            double lastTime = -1;
            long startSample1 = -1;
            long endSample1 = -1;

            for (int i = 0; i < track.getSampleDurations().length; i++) {
                long delta = track.getSampleDurations()[i];


                if (currentTime > lastTime && currentTime <= startTime) {
                    // current sample is still before the new starttime
                    startSample1 = currentSample;
                }
                if (currentTime > lastTime && currentTime <= endTime) {
                    // current sample is after the new start time and still before the new endtime
                    endSample1 = currentSample;
                }
                lastTime = currentTime;
                currentTime += (double) delta / (double) track.getTrackMetaData().getTimescale();
                currentSample++;
            }
            movie.addTrack(new AppendTrack(new ClippedTrack(track, startSample1, endSample1)));//, new ClippedTrack(track, startSample2, endSample2)));
        }
        return movie;
    }


    private void scanWaudioVideos(File outFileWaudio) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            Uri contentUri = Uri.fromFile(outFileWaudio);
            mediaScanIntent.setData(contentUri);
            context.sendBroadcast(mediaScanIntent);
        } else {
            context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.fromFile(outFileWaudio)));
        }
    }

    /**
     * Show a "final" alert dialog that will exit the activity
     * after the user clicks on the OK button.  If an exception
     * is passed, it's assumed to be an error condition, and the
     * dialog is presented as an error, and the stack trace is
     * logged.  If there's no exception, it's a success message.
     */
    private void showFinalAlert(Exception e, CharSequence message) {
        CharSequence title;
        if (e != null) {
            Log.e("Waudio", "Error: " + message);
            Log.e("Waudio", getStackTrace(e));
            title = context.getResources().getText(R.string.alert_title_failure);
            //setResult(RESULT_CANCELED, new Intent());
        } else {
            Log.v("Waudio", "Success: " + message);
            title = context.getResources().getText(R.string.alert_title_success);
        }

        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(
                        R.string.alert_ok_button,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int whichButton) {
                                //finish();
                            }
                        })
                .setCancelable(false)
                .show();
    }

    private void showFinalAlert(Exception e, int messageResourceId) {
        showFinalAlert(e, context.getResources().getText(messageResourceId));
    }


    private String getStackTrace(Exception e) {
        StringWriter writer = new StringWriter();
        e.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }

    public boolean isWaudioCreated(String titleAudio, String template, double endTime) {
        if (getPathTemplate() == null || getTitle() == null || getEndTime() == 0.0) {
            return false;
        }
        return (titleAudio.equals(getTitle().toString()) && template.equals(getPathTemplate()) && endTime == this.getEndTime());
    }

    public double getEndTime() {
        return endTime;
    }

    public CharSequence getTitle() {
        return title;
    }

    public String getPathTemplate() {
        return pathTemplate;
    }

    public void setPathTemplate(String pathTemplate) {
        this.pathTemplate = pathTemplate;
    }

    public File getOutFileM4a() {
        return outFileM4a;
    }

    public File getOutFileWaudio() {
        return outFileWaudio;
    }

    public void setOutFileWaudio(File outFileWaudio) {
        this.outFileWaudio = outFileWaudio;
    }
}
