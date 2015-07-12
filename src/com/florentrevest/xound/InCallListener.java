package com.florentrevest.xound;

import android.content.Context;
import android.content.Intent;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

class InCallListener extends PhoneStateListener
{
    private Context mContext;
    private boolean mWasPlaying;
  
    public InCallListener(Context c)
    {
        mContext = c;
    }
    
    @Override
    public void onCallStateChanged(int state, String incomingNumber)
    {
        if(state == TelephonyManager.CALL_STATE_OFFHOOK)
        {
            if(!PlaybackService.hasInstance())
            {
                Intent playbackIntent = new Intent(mContext, PlaybackService.class);
                mContext.startService(playbackIntent);
                return;
            }
            PlaybackService p = PlaybackService.get();
            mWasPlaying = p.isPlaying();
            if(mWasPlaying)
                p.pause();
        }
        else if(state == TelephonyManager.CALL_STATE_IDLE)
        {
            if(!PlaybackService.hasInstance())
            {
                Intent playbackIntent = new Intent(mContext, PlaybackService.class);
                mContext.startService(playbackIntent);
                return;
            }
            PlaybackService p = PlaybackService.get();
            if(mWasPlaying)
                p.play();
        }
    }
}