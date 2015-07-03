package com.cosooki.orc;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

public class Commands {

    public static final String ACTION_RECORD_START              = "com.skplanet.orc.record.ACTION_START";

    public static final String EXTRA_RESULT_CODE                = "reult.code";
    public static final String EXTRA_RESULT_DATA                = "result.data";

    
    static void sendLocalBroadCast(Context context, int resultCode, Intent data) {
        Intent intent = new Intent(ACTION_RECORD_START);
        intent.putExtra(EXTRA_RESULT_CODE, resultCode);
        intent.putExtra(EXTRA_RESULT_DATA, data);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);        
    }
    
    static int parseResultCode(Intent intent) {
        if (intent == null) {
            return -1;
        }
        
        return intent.getIntExtra(EXTRA_RESULT_CODE, 0);
    }
    
    static Intent parseIntent(Intent intent) {
        if (intent == null) {
        	return null;
        }

        return intent.getParcelableExtra(EXTRA_RESULT_DATA);
    }
}
