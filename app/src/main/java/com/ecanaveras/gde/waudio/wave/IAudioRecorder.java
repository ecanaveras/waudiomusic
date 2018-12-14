package com.ecanaveras.gde.waudio.wave;

interface IAudioRecorder {

    void startRecord();
    void finishRecord();
    boolean isRecording();
}
