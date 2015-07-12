package com.florentrevest.xound;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;
import android.widget.Toast;

public class RemoteControlReceiver extends BroadcastReceiver
{
    private static long m_lastClickTime = 0;
    
    @Override
    public void onReceive(Context context, Intent intent)
    {
        if(Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction()))
        {
            KeyEvent event = (KeyEvent)intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);

            if(KeyEvent.KEYCODE_HEADSETHOOK == event.getKeyCode() && event.getEventTime() - m_lastClickTime < 300)
            {
                if(!PlaybackService.hasInstance())
                {
                    Intent playbackIntent = new Intent(context, PlaybackService.class);
                    context.startService(playbackIntent);
                    return;
                }
                PlaybackService p = PlaybackService.get();
                p.playPause();
                m_lastClickTime = 0;
            }
            else
                m_lastClickTime = event.getEventTime();
        }
        else if(intent.getAction().equals(Intent.ACTION_UMS_CONNECTED))
        {
            Toast.makeText(context, "Please, disable the USB mode to use Xound", Toast.LENGTH_SHORT).show();
            System.exit(0);
        }
    }
}