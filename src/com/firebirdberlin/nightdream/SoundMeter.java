/*
 * Copyright (C) 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.firebirdberlin.nightdream;

import android.media.MediaRecorder;
import android.os.Handler;
import android.util.Log;
import de.greenrobot.event.EventBus;
import java.io.IOException;
import java.lang.RuntimeException;


public class SoundMeter {
    static final private double EMA_FILTER = 0.6;
    static String TAG = "com.firebirdberlin.nightdream.SoundMeter";
    private MediaRecorder mRecorder = null;
    private double mEMA = 0.0;
    private boolean debug;
    private Handler handler;
    private int interval = 60000;
    private boolean running = false;


    public SoundMeter(boolean debuggable){
        debug = debuggable;
        mRecorder = null;
        handler = new Handler();
        if (debug) Log.d(TAG,"SoundMeter()");
    }

    public boolean start() {
        if (debug) Log.d(TAG,"SoundMeter.start()");
        if (mRecorder == null) {
            mRecorder = new MediaRecorder();
            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mRecorder.setOutputFile("/dev/null");
            mRecorder.setOnErrorListener(errorListener);
            mRecorder.setOnInfoListener(infoListener);
        }
        try{
            mRecorder.prepare();
        }catch (IOException e) {
            if (debug) Log.e(TAG," > IOEXCEPTION, when preparing SoundMeter: " + e.toString());
            this.release();
            return false;
        } catch (IllegalStateException e) {
            if (debug) Log.e(TAG," > IllegalStateException, when preparing SoundMeter: " + e.toString());
            this.release();
            return false;
        }

        try{
            mRecorder.start();
            mRecorder.getMaxAmplitude(); // init
            mEMA = 0.0;
            if (debug) Log.d(TAG," > SoundMeter started,");
        } catch (IllegalStateException e) {
            if (debug) Log.e(TAG," > IllegalStateException, when starting SoundMeter: " + e.toString());
            this.release();
            return false;
        }

        return true;
    }

    public void stop() {
        if (mRecorder != null) {
            try{
                mRecorder.stop();
            } catch (IllegalStateException e) {
                if (debug) Log.e(TAG,"Error, when stopping SoundMeter: " + e.toString());
            } catch (RuntimeException e) {
                if (debug) Log.e(TAG,"RuntimeException when stopping SoundMeter: " + e.toString());
            }

            mRecorder.reset();

            try{
                mRecorder.release();
            } catch (Exception e) {
                if (debug) Log.e(TAG,"Error, when releasing SoundMeter: " +e.toString());
            }
            mRecorder = null;
            if (debug) Log.d(TAG,"SoundMeter stopped,");
        }

        running = false;
    }

    public void release() {
        handler.removeCallbacks(listenToAmbientNoise);
        stop();
        if (mRecorder != null) {
            mRecorder.reset();
            mRecorder.release();
            mRecorder = null;
            if (debug) Log.i(TAG,"SoundMeter released,");
        }

    }

    public double getAmplitude() {
        if (mRecorder != null)
            return  (mRecorder.getMaxAmplitude());
        else
            return -1.0;
    }

    public double getAmplitudeEMA() {
        double amp = getAmplitude();
        mEMA = EMA_FILTER * amp + (1.0 - EMA_FILTER) * mEMA;
        return mEMA;
    }

    private MediaRecorder.OnErrorListener errorListener = new MediaRecorder.OnErrorListener() {
        @Override
        public void onError(MediaRecorder mr, int what, int extra) {
            Log.e(TAG,"Error: " + what + ", " + extra);
        }
    };

    private MediaRecorder.OnInfoListener infoListener = new MediaRecorder.OnInfoListener() {
        @Override
        public void onInfo(MediaRecorder mr, int what, int extra) {
            Log.e(TAG,"Warning: " + what + ", " + extra);
        }
    };

    public void startMeasurement(int interval_millis) {
        this.interval = interval_millis;
        stopMeasurement();
        running = true;
        start();
        handler.postDelayed(listenToAmbientNoise, interval_millis);
    }

    public void stopMeasurement() {
        handler.removeCallbacks(listenToAmbientNoise);
        stop();
    }

    public boolean isRunning() {
        return running;
    }

    private Runnable listenToAmbientNoise = new Runnable() {
       @Override
       public void run() {
            double amp = getAmplitude();
            Log.i(TAG, "amplitude : " + String.valueOf(amp));
            stop();
            EventBus.getDefault().post(new OnNewAmbientNoiseValue(amp));
       }
    };
}

