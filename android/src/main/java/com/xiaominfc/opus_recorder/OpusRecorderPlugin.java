package com.xiaominfc.opus_recorder;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

import android.os.Handler;
import android.os.Looper;
import java.lang.Runnable;
import java.lang.InterruptedException;

/**
 * OpusRecorderPlugin
 */
public class OpusRecorderPlugin implements MethodCallHandler {
    /**
     * Plugin registration.
     */

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = { Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE };

    private static final int MODE_NORMAL = 1;
    private static final int MODE_IN_CALL = 2;

    MethodChannel channel;
    Registrar registrar;
    AudioPlayerHandler audioPlayerHandler;

    public OpusRecorderPlugin(MethodChannel _channel, Registrar _registrar) {
        this.channel = _channel;
        this.registrar = _registrar;
        audioPlayerHandler = AudioPlayerHandler.getInstance();
        audioPlayerHandler.setAudioMode(AudioManager.MODE_NORMAL, this.registrar.activeContext());
    }

    public static void verifyStorageAndMicPermissions(Activity activity) {

        try {
            // 检测是否有写的权限
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                int permission = ContextCompat.checkSelfPermission(activity, PERMISSIONS_STORAGE[1]);
                if (permission != PackageManager.PERMISSION_GRANTED) {

                    ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
                }

                permission = ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO);
                if (permission != PackageManager.PERMISSION_GRANTED) {

                    ActivityCompat.requestPermissions(activity, new String[] { Manifest.permission.RECORD_AUDIO }, 1);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void registerWith(Registrar registrar) {
        final MethodChannel channel = new MethodChannel(registrar.messenger(), "opus_recorder");
        channel.setMethodCallHandler(new OpusRecorderPlugin(channel, registrar));
    }

    private String currentRecordPath = null;
    private AudioRecordHandler currentAudioRecordHandler = null;
    private Thread recThread = null;

    @Override
    public void onMethodCall(final MethodCall call, final Result result) {
        if (call.method.equals("getPlatformVersion")) {
            result.success("Android " + android.os.Build.VERSION.RELEASE);
        } else if ("startRecord".equals(call.method)) {
            verifyStorageAndMicPermissions(this.registrar.activity());
            if (currentAudioRecordHandler != null) {
                currentAudioRecordHandler.setRecording(false);
            }
            currentRecordPath = CommonUtil.getAudioSavePath(0);
            currentAudioRecordHandler = new AudioRecordHandler(currentRecordPath);
            currentAudioRecordHandler.setRecording(true);
            recThread = new Thread(currentAudioRecordHandler);
            recThread.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread t, Throwable e) {
                    final Throwable error = e;
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            final List<Object> arguments = new ArrayList<>();
                            arguments.add(error.getMessage());
                            arguments.add(error.getStackTrace());
                            channel.invokeMethod("onRecordError", arguments);
                        }
                    });
                }
            });
            recThread.start();
            result.success(null);
        } else if ("stopRecord".equals(call.method)) {
            if (currentAudioRecordHandler != null) {
                currentAudioRecordHandler.setRecording(false);
                final List<Object> arguments = new ArrayList<>();
                arguments.add(currentRecordPath);
                arguments.add(Double.valueOf(currentAudioRecordHandler.getRecordTime()));
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            recThread.join();
                        } catch (InterruptedException e) {
                        }

                        channel.invokeMethod("finishedRecord", arguments);
                    }
                });
            }
        } else if ("playFile".equals(call.method)) {
            if (audioPlayerHandler != null) {
                List<Object> arguments = (List<Object>) call.arguments;
                Log.i("xiaominfc", "play:" + arguments.get(0).toString());
                audioPlayerHandler.startPlay(arguments.get(0).toString());
            }
        } else if ("playFileWithMode".equals(call.method)) {
            List<Object> arguments = (List<Object>) call.arguments;
            if (audioPlayerHandler != null && arguments.size() == 2) {
                // List<Object> arguments = (List<Object>) call.arguments;
                String path = arguments.get(0).toString();
                Log.i("xiaominfc", "play:" + arguments.get(0).toString());
                int mode = (int) arguments.get(1);
                if (mode == MODE_IN_CALL) {
                    mode = AudioManager.MODE_IN_CALL;
                } else {
                    mode = AudioManager.MODE_NORMAL;
                }
                audioPlayerHandler.setAudioMode(mode, this.registrar.activeContext());
                audioPlayerHandler.startPlay(path);
            } else {
                Log.i("xiaominfc", "play fail for:" + call.toString());
                result.notImplemented();
            }
        } else {
            result.notImplemented();
        }
    }
}
