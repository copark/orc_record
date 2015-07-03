
View.OnClickListener = clickListener = new View.OnClickListener() {
    @Override
    public void onClick(View v) {
        startRecord(getApplicationContext());
    }
};

Recorder recorder;

public void startRecord(Context context) {
    if (recorder != null) {
        recorder.stop();
    }

    recorder = new Recorder(context);
    recorder.setListener(new RecordListener() {
        @Override
        public void onStop() {
        }
        @Override
        public void onStart() {
        }
        @Override
        public void onCreate() {
        }
        @Override
        public void onError(int errorCode, String reason) {
        }
        @Override
        public void onCompleted(String filename) {
        }                    
    });
    recorder.start(RecordOption.getDefaultOption(mContext));
}
