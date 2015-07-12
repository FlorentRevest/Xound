package com.florentrevest.xound;

import java.io.IOException;
import java.util.Locale;

import org.holoeverywhere.widget.SeekBar;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

public class MusicPlayerFragment extends Fragment implements PlaybackService.OnPlaybackChangedListener, SeekBar.OnSeekBarChangeListener
{
    private ImageView m_coverView;
    private ImageButton m_shuffleButton, m_repeatButton;
    private TextView m_lyricsView, m_durationView, m_positionView;
    private View m_mainView;
    private PlaybackService m_ps;
    private SeekBar m_seekBar;
    private Handler mHandler = new Handler();
    private AsyncTask<String, Integer, String> m_previousThread = null;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = m_mainView = inflater.inflate(R.layout.fragment_player, container, false);
        m_coverView = (ImageView)view.findViewById(R.id.cover);
        m_shuffleButton = (ImageButton)view.findViewById(R.id.shuffleButton);
        m_repeatButton = (ImageButton)view.findViewById(R.id.repeatButton);
        m_lyricsView = (TextView)view.findViewById(R.id.lyricsView);
        m_durationView = (TextView)view.findViewById(R.id.totalDuration);
        m_positionView = (TextView)view.findViewById(R.id.currentPosition);
        m_seekBar = (SeekBar)view.findViewById(R.id.seekBar);
        m_seekBar.setOnSeekBarChangeListener(this);
        PlaybackService.addOnPlaybackChangedListener(this);
        if(PlaybackService.hasInstance())
        {
            onCurrentMusicChanged();
            m_ps = PlaybackService.get();
            if(m_ps.isShuffle())
                m_shuffleButton.setImageResource(R.drawable.shuffle_activated);
            if(m_ps.isRepeat())
                m_repeatButton.setImageResource(R.drawable.repeat_activated);
        }
        
        m_shuffleButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View arg0) {
                if(m_ps == null) 
                    m_ps = PlaybackService.get();
                if(m_ps.isShuffle()) 
                {
                    m_ps.setShuffle(false);
                    m_shuffleButton.setImageResource(R.drawable.shuffle);
                }
                else
                {
                    m_ps.setShuffle(true);
                    m_shuffleButton.setImageResource(R.drawable.shuffle_activated);
                }
            }
        });

        m_repeatButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View arg0) {
                if(m_ps == null) 
                    m_ps = PlaybackService.get();
                if(m_ps.isRepeat())
                {
                    m_ps.setRepeat(false);
                    m_repeatButton.setImageResource(R.drawable.repeat);
                }
                else
                {
                    m_ps.setRepeat(true);
                    m_repeatButton.setImageResource(R.drawable.repeat_activated);
                }
            }
        });

        return view;
    } 
    
    public void onStateChanged()
    {
    }
    
    public void onCurrentMusicChanged()
    {
        if(m_ps == null) 
            m_ps = PlaybackService.get();
        final Song song = m_ps.getSong(0);
        if(song != null)
        { 
            Bitmap cover = song.getCover(getActivity());
            if(cover == null)
            {
                m_coverView.setImageResource(R.drawable.no_cover);
                try {
                    m_mainView.setBackgroundDrawable(getView().getResources().getDrawable(R.drawable.default_background));
                } catch (java.lang.NullPointerException e) {
                }
            }
            else
            {
                m_coverView.setImageBitmap(cover);
                m_mainView.setBackgroundDrawable(new BitmapDrawable(Bitmap.createScaledBitmap(cover, 4, 4, true)));
            }
            m_seekBar.setMax(m_ps.getDuration());
            updateProgressBar();
            
            m_lyricsView.setText("Loading...");
            
            if(m_previousThread != null)
                m_previousThread.cancel(true);
            
            m_previousThread = new AsyncTask<String, Integer, String>() {
                protected void onPreExecute()
                {
                }

                protected String doInBackground(String... aParams)
                {
                    try
                    {
                        String lyrics = "";
                        Document doc = Jsoup.connect("http://www.metrolyrics.com/" + song.getTitle().replace(" ", "-").toLowerCase(Locale.getDefault()) + "-lyrics-" + song.getArtist().replace(" ", "-").toLowerCase(Locale.getDefault()) + ".html").get();

                        Elements els = doc.select("div#lyrics-body > p > span, div#lyrics-body > p > br");
                        String eol = System.getProperty("line.separator"); 
                        for(Element el : els)
                        {
                            String tag = el.tagName();
                            if (tag == "br")
                                lyrics += eol;
                            else if (tag == "span")
                                if (el.children().size() == 0)
                                    lyrics += el.text() + eol;
                        }
                        
                        return lyrics;
                    }
                    catch(IOException e1)
                    {
                        return null;
                    }
                }
                
                protected void onPostExecute(String aResult)
                {
                    if(aResult != null && aResult.trim().length() != 0)
                    {
                        while(aResult.endsWith("\n"))
                            aResult = aResult.substring(0, aResult.length() - 1);
                        m_lyricsView.setText(aResult);
                    }
                    else
                    {
                        try {
                            m_lyricsView.setText(getView().getResources().getText(R.string.no_lyrics));
                        } catch (java.lang.NullPointerException e) {
                        }
                    }
                }
            };
            m_previousThread.execute();
        }
        else
        {
            try {
                m_coverView.setImageDrawable(getResources().getDrawable(R.drawable.no_cover));
            } catch (java.lang.IllegalStateException e) {
            }
            m_seekBar.setProgress(0);
            m_seekBar.setMax(0);
            m_lyricsView.setText("");
        }
    }

    public void updateProgressBar()
    {
        mHandler.postDelayed(mUpdateTimeTask, 200);
    } 

    private Runnable mUpdateTimeTask = new Runnable()
    {
        public void run()
        {
            int duration = m_ps.getDuration();
            String durationStr = "";
            
            int hours = (int)(duration / (1000*60*60));
            int minutes = (int)(duration % (1000*60*60)) / (1000*60);
            int seconds = (int)((duration % (1000*60*60)) % (1000*60) / 1000);

            if(hours > 0)
                durationStr += hours + ":";

            durationStr += minutes + ":";
            
            if(seconds < 10)
                durationStr += "0" + seconds;
            else
                durationStr += seconds;

            int position = m_ps.getPosition();
            String positionStr = "";
            
            hours = (int)(position / (1000*60*60));
            minutes = (int)(position % (1000*60*60)) / (1000*60);
            seconds = (int)((position % (1000*60*60)) % (1000*60) / 1000);

            if(hours > 0)
                positionStr += hours + ":";

            positionStr += minutes + ":";
            
            if(seconds < 10)
                positionStr += "0" + seconds;
            else
                positionStr += seconds;

            m_durationView.setText(durationStr);
            m_positionView.setText(positionStr);

            m_seekBar.setProgress(m_ps.getPosition());
            mHandler.postDelayed(this, 200);
        }
    };

    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch)
    {
        
    }

    public void onStartTrackingTouch(SeekBar seekBar)
    {
        mHandler.removeCallbacks(mUpdateTimeTask);
    }

    public void onStopTrackingTouch(SeekBar seekBar)
    {
        mHandler.removeCallbacks(mUpdateTimeTask);
        m_ps.seekToProgress(seekBar.getProgress());
        updateProgressBar();
    }

    @Override
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);

        Intent playbackIntent = new Intent(activity, PlaybackService.class);
        activity.startService(playbackIntent);
    }
}