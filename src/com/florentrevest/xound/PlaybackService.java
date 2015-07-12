package com.florentrevest.xound;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.widget.RemoteViews;

public class PlaybackService extends Service implements MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener
{
    public static final String ACTION_PLAY = "com.florentrevest.xound.action.PLAY";
    public static final String ACTION_PAUSE = "com.florentrevest.xound.action.PAUSE";
    public static final String ACTION_NEXT_SONG = "com.florentrevest.xound.action.NEXT_SONG";
    public static final String ACTION_PREVIOUS_SONG = "com.florentrevest.xound.action.PREVIOUS_SONG";
    public static final String ACTION_CLOSE_NOTIFICATION = "com.florentrevest.xound.CLOSE_NOTIFICATION";
    private static final int NOTIFICATION_ID = 2;

    private SongTimeline mTimeline;
    private Song mCurrentSong = null;

    private MediaPlayer mMediaPlayer;
    private static PlaybackService sInstance;
    private NotificationManager mNotificationManager;
    
    private boolean m_isRepeat = false, m_isShuffle = false;

    @Override
    public void onCreate()
    {
        super.onCreate();
        mTimeline = new SongTimeline();
        
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setOnCompletionListener(this);
        mMediaPlayer.setOnErrorListener(this);
        sInstance = this;

        setCurrentSong(0, false);

        mNotificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        loadState();
        
        AppWidgetManager manager = AppWidgetManager.getInstance(this);
        LongWidget.checkEnabled(this, manager);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        if (intent != null)
        {
            String action = intent.getAction();

            if (ACTION_NEXT_SONG.equals(action))
            {
                if(m_isShuffle)
                    setCurrentSong(new Random().nextInt(mTimeline.getLength()) - mTimeline.getPosition(), true);
                else
                    setCurrentSong(1, true);
            }
            else if (ACTION_PREVIOUS_SONG.equals(action))
                setCurrentSong(-1, true);
            else if(ACTION_PLAY.equals(action))
                play();
            else if(ACTION_PAUSE.equals(action))
                pause();
            else if(ACTION_CLOSE_NOTIFICATION.equals(action))
            {
                pause();
                stopForeground(true);
                mNotificationManager.cancel(NOTIFICATION_ID);
            }
        }
        return START_NOT_STICKY;
    }
    
    @Override
    public void onDestroy()
    {
        sInstance = null;

        if (mMediaPlayer != null)
        {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }

        saveState();

        super.onDestroy();
    }

    public void play()
    {
        if(mCurrentSong != null && mMediaPlayer != null)
        {
            mMediaPlayer.start();
            startForeground(NOTIFICATION_ID, createNotification(mCurrentSong));
            if(m_callbacks != null)
                for(OnPlaybackChangedListener s : m_callbacks)
                    s.onStateChanged();
            AppWidgetManager manager = AppWidgetManager.getInstance(this);
            LongWidget.updateWidget(this, manager, mCurrentSong, isPlaying());
        }
    }

    public void pause()
    {
        if(mCurrentSong != null && mMediaPlayer != null)
        {
            mMediaPlayer.pause();
            if(Build.VERSION.SDK_INT < 11)
            {
                stopForeground(true);
                mNotificationManager.cancel(NOTIFICATION_ID);
            }
            else
                mNotificationManager.notify(NOTIFICATION_ID, createNotification(mCurrentSong));
            for(OnPlaybackChangedListener s : m_callbacks)
                s.onStateChanged();
            AppWidgetManager manager = AppWidgetManager.getInstance(this);
            LongWidget.updateWidget(this, manager, mCurrentSong, isPlaying());
        }
    }

    public void playPause()
    {
        if(mMediaPlayer != null)
        {
            if(mMediaPlayer.isPlaying())
                pause();
            else
                play();
        }
    }
    
    private static List<OnPlaybackChangedListener> m_callbacks = null;

    public interface OnPlaybackChangedListener
    {
        public void onStateChanged();
        public void onCurrentMusicChanged();
    }
    
    public static void addOnPlaybackChangedListener(OnPlaybackChangedListener listener)
    {
        if(m_callbacks == null)
            m_callbacks = new ArrayList<OnPlaybackChangedListener>();
        
        m_callbacks.add(listener);
    }
    
    @SuppressLint("NewApi")
    public Notification createNotification(Song song)
    {
        RemoteViews views = new RemoteViews(getPackageName(), R.layout.notification);
        RemoteViews bigViews = null;
        
        Bitmap cover = song.getCover(this);
        if(cover == null)
            views.setImageViewResource(R.id.cover, R.drawable.ic_launcher);
        else
            views.setImageViewBitmap(R.id.cover, cover);
        
        if(Build.VERSION.SDK_INT > 10)
        {
            int playButton = mMediaPlayer.isPlaying() ? R.drawable.pause : R.drawable.play;
            views.setImageViewResource(R.id.play_pause, playButton);

            ComponentName service = new ComponentName(this, PlaybackService.class);

            if(mMediaPlayer.isPlaying())
            {
                Intent playPause = new Intent(PlaybackService.ACTION_PAUSE);
                playPause.setComponent(service);
                views.setOnClickPendingIntent(R.id.play_pause, PendingIntent.getService(this, 0, playPause, 0));
            }
            else
            {
                Intent playPause = new Intent(PlaybackService.ACTION_PLAY);
                playPause.setComponent(service);
                views.setOnClickPendingIntent(R.id.play_pause, PendingIntent.getService(this, 0, playPause, 0));
            }

            Intent next = new Intent(PlaybackService.ACTION_NEXT_SONG);
            next.setComponent(service);
            views.setOnClickPendingIntent(R.id.next, PendingIntent.getService(this, 0, next, 0));

            Intent close = new Intent(PlaybackService.ACTION_CLOSE_NOTIFICATION);
            close.setComponent(service);
            views.setOnClickPendingIntent(R.id.close, PendingIntent.getService(this, 0, close, 0));
        }
        
        if(Build.VERSION.SDK_INT > 16)
        {
            bigViews = new RemoteViews(getPackageName(), R.layout.expanded_notification);
            
            if(cover == null)
                bigViews.setImageViewResource(R.id.notification_expanded_base_image, R.drawable.ic_launcher);
            else
                bigViews.setImageViewBitmap(R.id.notification_expanded_base_image, cover);
            
            int playButton = mMediaPlayer.isPlaying() ? R.drawable.pause : R.drawable.play;
            bigViews.setImageViewResource(R.id.notification_expanded_base_play, playButton);

            ComponentName service = new ComponentName(this, PlaybackService.class);

            if(mMediaPlayer.isPlaying())
            {
                Intent playPause = new Intent(PlaybackService.ACTION_PAUSE);
                playPause.setComponent(service);
                bigViews.setOnClickPendingIntent(R.id.notification_expanded_base_play, PendingIntent.getService(this, 0, playPause, 0));
            }
            else
            {
                Intent playPause = new Intent(PlaybackService.ACTION_PLAY);
                playPause.setComponent(service);
                bigViews.setOnClickPendingIntent(R.id.notification_expanded_base_play, PendingIntent.getService(this, 0, playPause, 0));
            }
            
            Intent previous = new Intent(PlaybackService.ACTION_PREVIOUS_SONG);
            previous.setComponent(service);
            bigViews.setOnClickPendingIntent(R.id.notification_expanded_base_previous, PendingIntent.getService(this, 0, previous, 0));

            Intent next = new Intent(PlaybackService.ACTION_NEXT_SONG);
            next.setComponent(service);
            bigViews.setOnClickPendingIntent(R.id.notification_expanded_base_next, PendingIntent.getService(this, 0, next, 0));

            Intent close = new Intent(PlaybackService.ACTION_CLOSE_NOTIFICATION);
            close.setComponent(service);
            bigViews.setOnClickPendingIntent(R.id.notification_expanded_base_collapse, PendingIntent.getService(this, 0, close, 0));

            bigViews.setTextViewText(R.id.notification_expanded_base_line_one, song.getTitle());
            bigViews.setTextViewText(R.id.notification_expanded_base_line_two, song.getArtist());
        }
            
        views.setTextViewText(R.id.title, song.getTitle());
        views.setTextViewText(R.id.artist, song.getArtist());

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification notification = new Notification();
        notification.contentView = views;
        if(Build.VERSION.SDK_INT > 16)
            notification.bigContentView = bigViews;
        notification.icon = R.drawable.ic_launcher;
        notification.flags |= Notification.FLAG_ONGOING_EVENT;
        notification.contentIntent = pendingIntent;
        return notification;
    }

    public Song setCurrentSong(int delta, boolean play)
    {
        boolean isPlaying = play;
        if (mMediaPlayer == null)
            return null;

        if (mMediaPlayer.isPlaying())
        {
            isPlaying = true;
            mMediaPlayer.stop();
        }        
        
        if (delta == 0)
            mCurrentSong = mTimeline.getSong(0);
        else
            mCurrentSong = mTimeline.setCurrentQueuePosition(mTimeline.getPosition() + delta);
        
        if (mCurrentSong == null || mCurrentSong.getPath() == null)
            return null;

        try
        {
            mMediaPlayer.reset();
            mMediaPlayer.setDataSource(mCurrentSong.getPath());
            mMediaPlayer.prepare();
            if(isPlaying)
                play();
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }

        if(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean("scrobble", false))
            scrobble();

        if(m_callbacks != null)
            for(OnPlaybackChangedListener s : m_callbacks)
                s.onCurrentMusicChanged();
        
        AppWidgetManager manager = AppWidgetManager.getInstance(this);
        LongWidget.updateWidget(this, manager, mCurrentSong, isPlaying());
        
        saveState();
        
        return mCurrentSong;
    }

    public class LocalBinder extends Binder {
        PlaybackService getService()
        {
            return PlaybackService.this;
        }
    }
    
    private final IBinder mBinder = new LocalBinder();
    
    @Override
    public IBinder onBind(Intent arg0) {
        return mBinder;
    }

    public int getPosition()
    {
        if(mCurrentSong != null)
            return mMediaPlayer.getCurrentPosition();
        else
            return 0;
    }
    
    public int getDuration()
    {
        if(mCurrentSong != null)
            return mMediaPlayer.getDuration();
        else
            return 0;
    }
    
    public void seekToProgress(int progress)
    {
        if(mCurrentSong != null)
            mMediaPlayer.seekTo(progress);
    }
    
    public boolean onError(MediaPlayer mp, int what, int extra)
    {
        return false;
    }

    public void onCompletion(MediaPlayer mp)
    {
        if(m_isRepeat)
            setCurrentSong(0, true);
        else if(m_isShuffle)
            setCurrentSong(new Random().nextInt(mTimeline.getLength()) - mTimeline.getPosition(), true);
        else
            setCurrentSong(1, true);
    }
    
    public static PlaybackService get()
    {
        return sInstance;
    }
    
    public static boolean hasInstance()
    {
        return sInstance != null;
    }
    
    public int getTimelinePosition()
    {
        return mTimeline.getPosition();
    }

    public int getTimelineLength()
    {
        return mTimeline.getLength();
    }

    public void addSongs(ArrayList<Song> songs)
    {
        mTimeline.addSongs(songs);
        saveState();
    }
    
    public Song getSong(int delta)
    {
        if (mTimeline == null)
            return null;
        if (delta == 0)
            return mCurrentSong;
        return mTimeline.getSong(delta);
    }
    
    public Song getSongByQueuePosition(int id)
    {
        return mTimeline.getSongByQueuePosition(id);
    }

    public void jumpToQueuePosition(int id)
    {
        mTimeline.setCurrentQueuePosition(id);
        setCurrentSong(0, true);
        play();
    }
    
    public void clearQueue()
    {
        mTimeline.clear();
        if(mCurrentSong != null)
        {
            pause();
            stopForeground(true);
            mNotificationManager.cancel(NOTIFICATION_ID);
            mCurrentSong = null;
            if(m_callbacks != null)
                for(OnPlaybackChangedListener s : m_callbacks)
                    s.onCurrentMusicChanged();
            AppWidgetManager manager = AppWidgetManager.getInstance(this);
            LongWidget.updateWidget(this, manager, mCurrentSong, isPlaying());
        }
        mCurrentSong = null;
        saveState();
    }
    
    public boolean isPlaying()
    {
        return mMediaPlayer.isPlaying();
    }

    public void setRepeat(boolean r)
    {
        m_isRepeat = r;
        saveState();
    }
    
    public boolean isRepeat()
    {
        return m_isRepeat;
    }
    
    public void setShuffle(boolean s)
    {
        m_isShuffle = s;
        saveState();
    }
    
    public boolean isShuffle()
    {
        return m_isShuffle;
    }

    private void scrobble()
    {
        Intent intent = new Intent("net.jjc1138.android.scrobbler.action.MUSIC_STATUS");
        intent.putExtra("playing", isPlaying());
        if (mCurrentSong != null)
            intent.putExtra("id", (int)mCurrentSong.getId());
        sendBroadcast(intent);
    }
    
    public void saveState()
    {
        try { 
            DataOutputStream out = new DataOutputStream(openFileOutput("state", 0));
            out.writeBoolean(isShuffle());
            out.writeBoolean(isRepeat());
            mTimeline.writeState(out);
            out.close();
        } catch (IOException e) {
        } 
    } 
    
    public void loadState()
    {
        try {
            DataInputStream in = new DataInputStream(openFileInput("state"));
            m_isShuffle = in.readBoolean();
            m_isRepeat = in.readBoolean();
            mTimeline.readState(in, getApplicationContext());
            mCurrentSong = mTimeline.getSong(0);
            if(mMediaPlayer != null && mCurrentSong != null && mCurrentSong.getPath() != null)
            {
                try
                {
                    mMediaPlayer.reset();
                    mMediaPlayer.setDataSource(mCurrentSong.getPath());
                    mMediaPlayer.prepare();
                }
                catch(IOException e)
                {
                }
                
                if(m_callbacks != null)
                    for(OnPlaybackChangedListener s : m_callbacks)
                        s.onCurrentMusicChanged();
                AppWidgetManager manager = AppWidgetManager.getInstance(this);
                LongWidget.updateWidget(this, manager, mCurrentSong, isPlaying());
            }
            
            in.close();
        } catch (EOFException e) {
        } catch (IOException e) {
        }
    }

    public int getCurrentAudioId()
    {
        if(mMediaPlayer == null)
            return 0;
        
        return mMediaPlayer.getAudioSessionId();
    }
}
