package com.ecanaveras.gde.waudio;


import org.mp4parser.Container;
import org.mp4parser.IsoFile;
import org.mp4parser.boxes.iso14496.part12.TrackBox;
import org.mp4parser.boxes.iso14496.part15.AvcConfigurationBox;
import org.mp4parser.muxer.FileRandomAccessSourceImpl;
import org.mp4parser.muxer.Movie;
import org.mp4parser.muxer.Sample;
import org.mp4parser.muxer.Track;
import org.mp4parser.muxer.builder.DefaultMp4Builder;
import org.mp4parser.muxer.container.mp4.MovieCreator;
import org.mp4parser.muxer.container.mp4.Mp4SampleList;
import org.mp4parser.muxer.tracks.AppendTrack;
import org.mp4parser.muxer.tracks.ChangeTimeScaleTrack;
import org.mp4parser.muxer.tracks.ClippedTrack;
import org.mp4parser.tools.IsoTypeReaderVariable;
import org.mp4parser.tools.Path;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;


/**
 * Created by ecanaveras on 19/07/2017.
 */

public class TestMp4Parser {

    public static void main(String[] args) throws IOException {
        try {
            Movie movie = new Movie();

            Movie listAudio = MovieCreator.build("D:\\DEV\\tmp\\mp4parser\\ArroyitoEditado.m4a");
            Movie listMp4 = MovieCreator.build("D:\\DEV\\tmp\\mp4parser\\waudio_template5.mp4");

            List listMovieTracks = new ArrayList();

            //Audio
            for (Track audio : listAudio.getTracks()) {
                if (audio.getHandler().equals("soun")) {
                    listMovieTracks.add(audio);
                    System.out.println("AUDIO:" + audio.getHandler() + " Duracion:" + (audio.getDuration() / audio.getTrackMetaData().getTimescale()) + " Scale:" + audio.getTrackMetaData().getTimescale());
                }

            }

            //Video
            //Corta el video, adaptandolo al tamaño del audio
            for (Track video : listMp4.getTracks()) {//getShorTracks("D:\\DEV\\tmp\\mp4parser\\waudio_template5.mp4", 0, (audio.getDuration() / audio.getTrackMetaData().getTimescale()) - 1)) { // Segundos
                if (video.getHandler().equals("vide")) {
                    listMovieTracks.add(video);
                    System.out.println("VIDEO:" + video.getHandler() + " Duracion:" + (video.getDuration() / video.getTrackMetaData().getTimescale()) + " Scale:" + video.getTrackMetaData().getTimescale());
                }
            }

            //Seteamos audio y video
            movie.setTracks(listMovieTracks);

            Container mp4File = new DefaultMp4Builder().build(movie);
            FileChannel fc = new FileOutputStream(new File("D:\\DEV\\tmp\\mp4parser\\Waudio" + new SimpleDateFormat("ddMMyyHHmm").format(new Date()) + ".mp4")).getChannel();
            mp4File.writeContainer(fc);
            fc.close();
            System.out.println("File waudio" + new SimpleDateFormat("ddMMyyHHmm").format(new Date()) + ".mp4 Finalized");

        } catch (
                IOException e)

        {
            e.printStackTrace();
        }

    }

    private static List<Track> getShorTracks(String filepath, double startTime, double endTime) throws IOException {
        Movie movie = MovieCreator.build(filepath);

        List<Track> tracks = movie.getTracks();
        movie.setTracks(new LinkedList<Track>());
        // remove all tracks we will create new tracks from the old

        System.out.println("startTime:" + startTime + " endTime:" + endTime);
        //double startTime1 = 10;
        //double endTime1 = 20;
        //double startTime2 = 30;
        //double endTime2 = 40;

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
                startTime = correctTimeToSyncSample(track, startTime, false);
                endTime = correctTimeToSyncSample(track, endTime, true);
                //startTime2 = correctTimeToSyncSample(track, startTime2, false);
                //endTime2 = correctTimeToSyncSample(track, endTime2, true);
                timeCorrected = true;
            }
        }

        for (Track track : tracks) {
            long currentSample = 0;
            double currentTime = 0;
            double lastTime = -1;
            long startSample1 = -1;
            long endSample1 = -1;
            //long startSample2 = -1;
            //long endSample2 = -1;

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

        return movie.getTracks();
    }

    private static double correctTimeToSyncSample(Track track, double cutHere, boolean next) {
        double[] timeOfSyncSamples = new double[track.getSyncSamples().length];
        long currentSample = 0;
        double currentTime = 0;
        for (int i = 0; i < track.getSampleDurations().length; i++) {
            long delta = track.getSampleDurations()[i];

            if (Arrays.binarySearch(track.getSyncSamples(), currentSample + 1) >= 0) {
                // samples always start with 1 but we start with zero therefore +1
                timeOfSyncSamples[Arrays.binarySearch(track.getSyncSamples(), currentSample + 1)] = currentTime;
            }
            currentTime += (double) delta / (double) track.getTrackMetaData().getTimescale();
            currentSample++;

        }
        double previous = 0;
        for (double timeOfSyncSample : timeOfSyncSamples) {
            if (timeOfSyncSample > cutHere) {
                if (next) {
                    return timeOfSyncSample;
                } else {
                    return previous;
                }
            }
            previous = timeOfSyncSample;
        }
        return timeOfSyncSamples[timeOfSyncSamples.length - 1];
    }
}
