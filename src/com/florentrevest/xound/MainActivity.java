package com.florentrevest.xound;

import java.util.List;
import java.util.Random;

import org.holoeverywhere.app.Activity;
import org.holoeverywhere.preference.SharedPreferences;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.audiofx.AudioEffect;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.FragmentTransaction;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class MainActivity extends Activity implements PlaybackService.OnPlaybackChangedListener, ArtistsListFragment.OnArtistSelectedListener, AlbumsListFragment.OnAlbumSelectedListener, MusicsListFragment.OnMusicSelectedListener
{
    private ImageButton m_coverButton;
    private ImageButton m_playPauseButton;
    private TextView m_titleView;
    private TextView m_artistView;
    private MusicPlayerFragment m_musicPlayerFragment;
    private GestureDetector gdt;
    private SensorManager mSensorManager;
    private ShakeEventListener mSensorListener;
    public InCallListener mCallListener;
    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        if (Environment.MEDIA_SHARED.equals(Environment.getExternalStorageState()))
        {
            Toast.makeText(this, getString(R.string.disableUSB), Toast.LENGTH_SHORT).show();
            finish();
        }
        else
        {        
            getSupportActionBar().setDisplayShowHomeEnabled(false);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayShowCustomEnabled(true);
        
            View customView = LayoutInflater.from(this).inflate(R.layout.action_bar, null);
            m_coverButton = (ImageButton)customView.findViewById(R.id.cover);
            m_coverButton.setOnClickListener(new OnClickListener() {
                public void onClick(View v)
                {
                    showMusicPlayer();
                }});
            m_playPauseButton = (ImageButton)customView.findViewById(R.id.play_pause);
            m_playPauseButton.setOnClickListener(new OnClickListener() {
                public void onClick(View v)
                {
                    if(!PlaybackService.hasInstance())
                    {
                        Intent playbackIntent = new Intent(MainActivity.this, PlaybackService.class);
                        startService(playbackIntent);
                        return;
                    }
                    
                    PlaybackService ps = PlaybackService.get();
                    
                    ps.playPause();
                }});
            m_titleView = (TextView)customView.findViewById(R.id.title);
            m_artistView = (TextView)customView.findViewById(R.id.artist);
            getSupportActionBar().setCustomView(customView);

            gdt = new GestureDetector(new GestureListener());
            customView.findViewById(R.id.title_artist_layout).setOnTouchListener(new OnTouchListener() {
                public boolean onTouch(final View view, final MotionEvent event) {
                    gdt.onTouchEvent(event);
                    return true;
                }
            });
            
            try {
                mCallListener = new InCallListener(this);
                TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
                telephonyManager.listen(mCallListener, PhoneStateListener.LISTEN_CALL_STATE);
            } catch (SecurityException e) {
                // don't have READ_PHONE_STATE
            }
            
            mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
            mSensorListener = new ShakeEventListener();   

            mSensorListener.setOnShakeListener(new ShakeEventListener.OnShakeListener() {

              public void onShake()
              {
                  if(!PlaybackService.hasInstance())
                  {
                      Intent playbackIntent = new Intent(MainActivity.this, PlaybackService.class);
                      startService(playbackIntent);
                      return;
                  }
                  PlaybackService ps = PlaybackService.get();
                  if(ps.getTimelineLength() != 0)
                      ps.setCurrentSong(new Random().nextInt(ps.getTimelineLength()) - ps.getTimelinePosition(), true);
              }
            });

            setContentView(R.layout.activity_main);

            ArtistsListFragment artistsListFragment = new ArtistsListFragment();
            if(findViewById(R.id.fragment_container) != null) 
            {
                if(savedInstanceState != null)
                    return;

                getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, artistsListFragment).commit();
            }

            PlaybackService.addOnPlaybackChangedListener(this);
            if(PlaybackService.hasInstance())
            {
                onCurrentMusicChanged();
                onStateChanged();
            }
            
            Intent playbackIntent = new Intent(this, PlaybackService.class);
            startService(playbackIntent);

            ArtistsInfoService.addOnArtistsInfoListener(artistsListFragment);
            if(!ArtistsInfoService.hasInstance())
            {
                Intent intent = new Intent(this, ArtistsInfoService.class);
                startService(intent);
            }
        
            setVolumeControlStream(AudioManager.STREAM_MUSIC);
            ((AudioManager)getSystemService(AUDIO_SERVICE)).registerMediaButtonEventReceiver(new ComponentName(this, RemoteControlReceiver.class));
        }
    }

    @Override
    protected void onResume() {
      super.onResume();
      mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
      mSensorManager.unregisterListener(mSensorListener);
      super.onStop();
    }

    private static final int SWIPE_MIN_DISTANCE = 200;
    private static final int SWIPE_THRESHOLD_VELOCITY = 200;

    private class GestureListener extends SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY)
        {
            if(!PlaybackService.hasInstance())
            {
                Intent playbackIntent = new Intent(MainActivity.this, PlaybackService.class);
                startService(playbackIntent);
                return false;
            }
            
            if(e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY)
            {
                PlaybackService ps = PlaybackService.get();
                if(ps.isShuffle() && ps.getTimelineLength() > 0)
                    ps.setCurrentSong(new Random().nextInt(ps.getTimelineLength()) - ps.getTimelinePosition(), true);
                else
                    ps.setCurrentSong(1, true);
                return false;
            }
            else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY)
            {
                PlaybackService ps = PlaybackService.get();
                if(ps.isShuffle())
                    ps.setCurrentSong(new Random().nextInt(ps.getTimelineLength()) - ps.getTimelinePosition(), true);
                else
                    ps.setCurrentSong(-1, true);
                
                return false;
            }

            return false;
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getSupportMenuInflater().inflate(R.menu.activity_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch(item.getItemId())
        {
            case R.id.menu_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case R.id.menu_equalizer:
                Intent i = new Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL);
                i.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, PlaybackService.get().getCurrentAudioId());
                if(getPackageManager().resolveActivity(i, 0) != null)
                    startActivityForResult(i, 0);
                else
                    Toast.makeText(this, R.string.equalizer_needed, Toast.LENGTH_LONG).show();
                return true;
            case R.id.menu_about:
                AboutDialog about = new AboutDialog(this);
                about.setTitle(getString(R.string.about));
                about.show();
                return true;
            case R.id.menu_exit:
                finish();
                System.exit(0);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    public void onArtistSelected(String uri, String name)
    {
        AlbumsListFragment newFragment = new AlbumsListFragment();
        Bundle args = new Bundle();
        if(uri != null)
            args.putString("URI", uri);
        if(name != null)
            args.putString("Name", name);
        newFragment.setArguments(args);
       
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right, android.R.anim.slide_in_left, android.R.anim.slide_out_right);

        transaction.replace(R.id.fragment_container, newFragment);
        transaction.addToBackStack(null);

        transaction.commit();
    }
    
    public void onArtistInfoSelected(String name)
    {
        ArtistInfoFragment newFragment = new ArtistInfoFragment();
        Bundle args = new Bundle();
        if(name != null)
            args.putString("ArtistName", name);
        newFragment.setArguments(args);
       
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right, android.R.anim.slide_in_left, android.R.anim.slide_out_right);

        transaction.replace(R.id.fragment_container, newFragment);
        transaction.addToBackStack(null);

        transaction.commit();
    }

    public void onAlbumSelected(List<Integer> ids, Boolean sort, String name)
    {
        if(ids != null)
        {
            MusicsListFragment newFragment = new MusicsListFragment();
            Bundle args = new Bundle();
        
            long[] idsArray = new long[ids.size()];
            for (int i = 0; i < ids.size(); i++)
                idsArray[i] = ids.get(i);

            args.putLongArray("IDs", idsArray);
            args.putBoolean("Sort", sort);
            args.putString("Name", name);
            newFragment.setArguments(args);
        
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right, android.R.anim.slide_in_left, android.R.anim.slide_out_right);

            transaction.replace(R.id.fragment_container, newFragment);
            transaction.addToBackStack(null);

            transaction.commit();
        }
    }

    public void onStateChanged()
    {
        PlaybackService ps = PlaybackService.get();
        
        int playButton = ps.isPlaying() ? R.drawable.pause : R.drawable.play;
        m_playPauseButton.setImageDrawable(getResources().getDrawable(playButton));
    }
    
    public void onCurrentMusicChanged()
    {
        if(!PlaybackService.hasInstance())
        {
            Intent playbackIntent = new Intent(MainActivity.this, PlaybackService.class);
            startService(playbackIntent);
            return;
        }
        
        PlaybackService ps = PlaybackService.get();

        Song song = ps.getSong(0);
        if(song != null)
        {
            Bitmap cover = song.getCover(this);
            if (cover == null)
                m_coverButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_launcher));
            else
                m_coverButton.setImageBitmap(cover);

            m_titleView.setText(song.getTitle());
            if(song.getArtist().equals("<unknown>"))
                m_artistView.setText(getText(R.string.unknown_artist));
            else
                m_artistView.setText(song.getArtist());
        }
        else
        {
            m_coverButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_launcher));
            m_playPauseButton.setImageDrawable(getResources().getDrawable(R.drawable.play));
            m_titleView.setText(getResources().getString(R.string.app_name));
            m_artistView.setText(getResources().getString(R.string.slogan));
        }
    }
    
    public void showMusicPlayer()
    {
        if(!PlaybackService.hasInstance())
        {
            Intent playbackIntent = new Intent(this, PlaybackService.class);
            startService(playbackIntent);
            return;
        }

        if(m_musicPlayerFragment == null)
            m_musicPlayerFragment = new MusicPlayerFragment();

        if(!m_musicPlayerFragment.isVisible())
        {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right, android.R.anim.slide_in_left, android.R.anim.slide_out_right);

            transaction.replace(R.id.fragment_container, m_musicPlayerFragment);
            transaction.addToBackStack(null);

            transaction.commit();
        }
        SharedPreferences prefs = getSharedPreferences("com.florentrevest.xound.firstrun", 0);
        if(prefs.getBoolean("firstRun", true))
        {
            startActivity(new Intent(this, HelpActivity.class));
            SharedPreferences.Editor edit = prefs.edit();
            edit.putBoolean("firstRun", false);
            edit.commit();
        }
    }
}
