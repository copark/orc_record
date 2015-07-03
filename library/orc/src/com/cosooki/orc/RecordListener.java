package com.cosooki.orc;

public interface RecordListener {
    public void onCreate();
    
    public void onStart();
    
    public void onStop();
    
    public void onError(int errorCode, String reason);
    
    public void onCompleted(String filename);
    
}
