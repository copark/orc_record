package com.cosooki.orc;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.content.Context;
import android.os.Environment;

public class RecordOption {

	private static final String DEFAULT_DATE_FORAMT					= "yyyyMMdd_HHmmss";

	private static final String DEFAULT_FILE_EXT					= ".mp4";
	
    public static final int RECORD_SIZE_FULL                        = 1;
    public static final int RECORD_SIZE_HALF                        = 2;
    public static final int RECORD_SIZE_QUATER                      = 4;

    public static final int RECORD_FRAME_HIGH                       = 30;
    public static final int RECORD_FRAME_MEDIUM                     = 20;
    public static final int RECORD_FRAME_LOW                        = 10;
    
    public static final int DEFAULT_RECORD_SIZE                     = RECORD_SIZE_FULL;
    public static final int DEFAULT_FRAME_RATE                      = RECORD_FRAME_HIGH;    
    public static final int DEFAULT_BIT_RATE                        = 8 * 1000 * 1000;
    
    public static final int MAX_RECORD_TIME                         = 60 * 1000;
    
    private static String getDefaultFileName(Context context) {
        File movieDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MOVIES);
        String packageName = context.getPackageName();
        File outRoot = new File(movieDir, packageName);
        outRoot.mkdirs();
        File file = new File(outRoot, getFileName(packageName));
        
        return file.getAbsolutePath();
    }

    private static String getFileName(String prefix) {
        String curDate = new SimpleDateFormat(DEFAULT_DATE_FORAMT, Locale.US).format(new Date());
        return String.format("%s_%s%s", prefix, curDate, DEFAULT_FILE_EXT);
    }

    public static RecordOption getDefaultOption(Context context) {
        return new RecordOption(RECORD_SIZE_FULL,
                RECORD_FRAME_HIGH,
                getDefaultFileName(context));
    }
    
    final int size;
    final int frame;
    final String output;

    public RecordOption(int size, int frame, String output) {
        this.size = size;
        this.frame = frame;
        this.output = output;
    }

    /*
     * package access level
     */
    int getScreenSize() {
        return isValidSize(size) ? (size) : (DEFAULT_RECORD_SIZE);
    }
    
    int getFrameRate() {
        return isValidFrameRate(frame) ? (frame) : (DEFAULT_FRAME_RATE);
    }
    
    private boolean isValidSize(int size) {
        switch (size) {
            case RECORD_SIZE_FULL:
            case RECORD_SIZE_HALF:
            case RECORD_SIZE_QUATER:
                return true;
            default:
                break;
        }
        return false;
    }

    private boolean isValidFrameRate(int frame) {
        switch (frame) {
            case RECORD_FRAME_HIGH:
            case RECORD_FRAME_MEDIUM:
            case RECORD_FRAME_LOW:
                return true;
            default:
                break;
        }
        return false;
    }
}