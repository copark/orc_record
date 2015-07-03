package com.cosooki.orc;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.media.MediaRecorder.AudioEncoder;
import android.media.MediaRecorder.AudioSource;
import android.media.MediaRecorder.OutputFormat;
import android.media.MediaRecorder.VideoEncoder;
import android.media.MediaRecorder.VideoSource;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;
import android.util.DisplayMetrics;
import android.view.Surface;
import android.view.WindowManager;

public class Recorder {

    private Context _context;
    private LocalBroadcastManager _localManager;

    private RecordListener _listener;

    private RecordOption _option;

    private MediaRecorder _recorder;
    private MediaProjection _projection;
    private VirtualDisplay _display;

    private Timer _timeout;
    private WindowManager _windowManager;
    private MediaProjectionManager _projectionManager;

    private AtomicBoolean _running;

    private Handler _handler;

    /**
     * Create Recorder instance
     * @param context
     */
    public Recorder(Context context) {
        _context = context.getApplicationContext();
        _handler = new Handler(Looper.getMainLooper());

        _running = new AtomicBoolean();

        _windowManager = (WindowManager) context.getSystemService(
                Context.WINDOW_SERVICE);
        _projectionManager =  (MediaProjectionManager) context.getSystemService(
                Context.MEDIA_PROJECTION_SERVICE);
    }

    private boolean isRunning() {
        return _running.get();
    }

    private void setRunning(boolean value) {
        _running.set(value);
    }

    private void registerReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Commands.ACTION_RECORD_START);
        _localManager = LocalBroadcastManager.getInstance(_context);
        _localManager.registerReceiver(_localReceiver, intentFilter);
    }

    private void unRegisterReceiver() {
        if (_localManager != null) {
            _localManager.unregisterReceiver(_localReceiver);
        }

        _localManager = null;
    }

    private volatile BroadcastReceiver _localReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            final String action = intent.getAction();
            if (!Commands.ACTION_RECORD_START.equals(action)) {
                return;
            }

            unRegisterReceiver();

            int resultCode = Commands.parseResultCode(intent);
            Intent data = Commands.parseIntent(intent);
            onStartRecordIntent(resultCode, data);
        }
    };

    private void startRecordIntent() {
        try {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(_context, RecordHelperActivity.class));            
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            _context.startActivity(intent);
        } catch (Exception e) {
            throw new RuntimeException("Not Found RecordHelper Activity");
        }

        registerReceiver();
    }

    private boolean checkRecordIntent(int resultCode, Intent data) {

        if (resultCode == Activity.RESULT_CANCELED) {
            callOnErrorListener(RecordError.USER_CANCELED, "User canceled.");
            return false;
        }

        return true;
    }

    private void onStartRecordIntent(int resultCode, Intent data) {

        if (!checkRecordIntent(resultCode, data)) {
            return;
        }

        callOnCreateListener();

        prepareRecorder();

        createVirtualDisplay(resultCode, data);

        startRecorder();
    }

    private void checkOption(RecordOption option) {
        if (checkNull(option)) {
            throwIAE("RecordOption is null");
        }
        // FIXME
        _option = option;
    }

    /**
     * register record listener
     * @param listener
     */
    public void setListener(RecordListener listener) {
        _listener = listener;
    }

    /**
     * start record
     * @param option
     */
    public void start(RecordOption option) {
        if (isRunning()) {
            throwISE("Recorder is already running");
        }

        checkOption(option);

        startRecordIntent();
    }

    /**
     * stop record
     * @param option
     */
    public void stop() {
        if (!isRunning()) {
            throwISE("Recorder is not running");
        }

        stopRecorder();
    }

    private void createVirtualDisplay(int resultCode, Intent data) {

        initProjection(resultCode, data);

        DisplayInfo info = getDisplayInfo(_option);
        Surface surface = _recorder.getSurface();
        _display = _projection.createVirtualDisplay(
                _context.getPackageName(),
                info.width,
                info.height,
                info.density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION,
                surface, null, null);
    }

    private void initProjection(int resultCode, Intent data) {
        _projection = _projectionManager.getMediaProjection(resultCode, data);

        if (checkNull(_projection)) {
            callOnErrorListener(RecordError.UNKNOWN,
                    "Can not get MediaProjection");
        }
    }

    private boolean checkRecordAudio() {
        if (_context.checkCallingOrSelfPermission(Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            return true;
        }

        return false;
    }

    private void prepareRecorder() {
        _recorder = new MediaRecorder();

        if (checkRecordAudio()) {
            _recorder.setAudioSource(AudioSource.MIC);
        }        
        _recorder.setVideoSource(VideoSource.SURFACE);        
        _recorder.setOutputFormat(OutputFormat.MPEG_4);
        if (checkRecordAudio()) {
            _recorder.setAudioEncoder(AudioEncoder.AMR_NB);
        }
        _recorder.setVideoEncoder(VideoEncoder.H264);

        DisplayInfo info = getDisplayInfo(_option);
        int frameRate = (_option != null) ? _option.getFrameRate() : 
            RecordOption.DEFAULT_FRAME_RATE;

        _recorder.setVideoSize(info.width, info.height);
        _recorder.setVideoFrameRate(frameRate);
        _recorder.setVideoEncodingBitRate(RecordOption.DEFAULT_BIT_RATE);        
        _recorder.setOutputFile(_option.output);

        try {
            _recorder.prepare();
        } catch (Exception e) {
            callOnErrorListener(RecordError.PREPARE_ERROR,
                    "Occured " + e.toString());
        }
    }

    private void startRecorder() {
        if (isRunning()) {
            return;
        }

        _recorder.start();        
        setRunning(true);

        callOnStartListener();        
        startTimer();
    }

    private void stopRecorder() {
        if (!isRunning()) {
            return;
        }

        setRunning(false);
        stopTimer();

        _projection.stop();
        _recorder.stop();

        callOnStopListener();

        _recorder.release();
        _display.release();

        callOnCompletListener(_option.output);
    }

    private DisplayInfo getDisplayInfo(RecordOption option) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        _windowManager.getDefaultDisplay().getRealMetrics(displayMetrics);

        int width = displayMetrics.widthPixels;
        int height = displayMetrics.heightPixels;
        int density = displayMetrics.densityDpi;

        Configuration configuration = _context.getResources().getConfiguration();
        boolean isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE;

        CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
        int cameraWidth = profile != null ? profile.videoFrameWidth : -1;
        int cameraHeight = profile != null ? profile.videoFrameHeight : -1;

        if (option != null) {
            width = width * (100 / option.getScreenSize()) / 100;
            height = height * ( 100 / option.getScreenSize()) / 100;
        }

        if (cameraWidth == -1 && cameraHeight == -1) {
            return new DisplayInfo(width, height, density);
        }

        int frameWidth = isLandscape ? cameraWidth : cameraHeight;
        int frameHeight = isLandscape ? cameraHeight : cameraWidth;
        if (frameWidth >= width && frameHeight >= height) {
            return new DisplayInfo(width, height, density);
        }

        if (isLandscape) {
            frameWidth = width * frameHeight / height;
        } else {
            frameHeight = height * frameWidth / width;
        }

        return new DisplayInfo(frameWidth, frameHeight, density);
    }

    private void callOnCreateListener() {
        if (_listener != null) {
            _handler.post(new Runnable() {
                @Override
                public void run() {
                    _listener.onCreate();
                }
            });
        }
    }

    private void callOnStartListener() {
        if (_listener != null) {
            _handler.post(new Runnable() {
                @Override
                public void run() {
                    _listener.onStart();
                }
            });
        }
    }

    private void callOnStopListener() {
        if (_listener != null) {
            _handler.post(new Runnable() {
                @Override
                public void run() {
                    _listener.onStop();
                }
            });
        }
    }

    private void callOnErrorListener(final int errorCode, final String reason) {
        if (_listener != null) {
            _handler.post(new Runnable() {
                @Override
                public void run() {
                    _listener.onError(errorCode, reason);
                }
            });
        }
    }

    private void callOnCompletListener(final String fileName) {
        if (_listener != null) {
            _handler.post(new Runnable() {
                @Override
                public void run() {
                    _listener.onCompleted(fileName);
                }
            });
        }
    }

    private void startTimer() {
        stopTimer();

        _timeout = new Timer();
        _timeout.schedule(new TimeoutTask(), RecordOption.MAX_RECORD_TIME);
    }

    private void stopTimer() {
        if (_timeout != null) {
            _timeout.cancel();
            _timeout = null;
        }
    }

    private class TimeoutTask extends TimerTask {
        @Override
        public void run() {
            if (isRunning()) {
                stopRecorder();
            }
        }
    }

    private void throwISE(String message) {
        throw new IllegalStateException(message);
    }

    private void throwIAE(String message) {
        throw new IllegalArgumentException(message);
    }

    private boolean checkNull(Object o) {
        return (o == null);
    }

    private static final class DisplayInfo {
        final int width;
        final int height;
        final int density;

        DisplayInfo(int width, int height, int density) {
            this.width = width;
            this.height = height;
            this.density = density;
        }
    }    
}
