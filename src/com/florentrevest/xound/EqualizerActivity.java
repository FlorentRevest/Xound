package com.florentrevest.xound;

import java.util.ArrayList;
import java.util.List;

import org.holoeverywhere.ArrayAdapter;
import org.holoeverywhere.app.Activity;
import org.holoeverywhere.widget.AdapterView;
import org.holoeverywhere.widget.AdapterView.OnItemSelectedListener;
import org.holoeverywhere.widget.Spinner;

import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.audiofx.AudioEffect;
import android.media.audiofx.Equalizer;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

public class EqualizerActivity extends Activity
{
    private Equalizer mEqualizer;
    private LinearLayout mLinearLayout;

    @Override
    public void onCreate(Bundle icicle)
    {
        super.onCreate(icicle);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        mLinearLayout = new LinearLayout(this);
        mLinearLayout.setOrientation(LinearLayout.VERTICAL);
        setContentView(mLinearLayout);

        mEqualizer = new Equalizer(0, getIntent().getExtras().getInt(AudioEffect.EXTRA_AUDIO_SESSION, 0));
        final short bands = mEqualizer.getNumberOfBands();
        final short minEQLevel = mEqualizer.getBandLevelRange()[0];
        final short maxEQLevel = mEqualizer.getBandLevelRange()[1];
        
        Spinner presetsSpinner = new Spinner(this);
        List<String> list = new ArrayList<String>();
        for(short i = 0; i < mEqualizer.getNumberOfPresets() ; i++)
            list.add(mEqualizer.getPresetName(i));
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, list);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        presetsSpinner.setAdapter(dataAdapter);
        
        mLinearLayout.addView(presetsSpinner);

        for (short i = 0; i < bands; i++)
        {
            final short band = i;

            TextView freqTextView = new TextView(this);
            freqTextView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            freqTextView.setGravity(Gravity.CENTER_HORIZONTAL);
            freqTextView.setText((mEqualizer.getCenterFreq(band) / 1000) + " Hz");
            freqTextView.setTypeface(null, Typeface.BOLD);
            mLinearLayout.addView(freqTextView);

            LinearLayout row = new LinearLayout(this);
            row.setTag("layout" + i);
            row.setOrientation(LinearLayout.HORIZONTAL);

            TextView minDbTextView = new TextView(this);
            minDbTextView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            minDbTextView.setText((minEQLevel / 100) + " dB");

            TextView maxDbTextView = new TextView(this);
            maxDbTextView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            maxDbTextView.setText((maxEQLevel / 100) + " dB");

            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            layoutParams.weight = 1;
            SeekBar bar = new SeekBar(this);
            bar.setTag("sliderNumber" + i);
            bar.setLayoutParams(layoutParams);
            bar.setMax(maxEQLevel - minEQLevel);
            bar.setProgress((maxEQLevel - minEQLevel)/2 + mEqualizer.getBandLevel(band));

            bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
            {
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
                {
                    mEqualizer.setBandLevel(band, (short) (progress + minEQLevel));
                }

                public void onStartTrackingTouch(SeekBar seekBar) {}
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });

            row.addView(minDbTextView);
            row.addView(bar);
            row.addView(maxDbTextView);

            mLinearLayout.addView(row);
        }
        mEqualizer.setEnabled(true);
        

        presetsSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int pos,long id)
            {
                mEqualizer.usePreset((short)pos);
                
                for(short i = 0; i < bands; i++)
                {
                    LinearLayout layouti = (LinearLayout)mLinearLayout.findViewWithTag("layout" + i);
                    SeekBar bar = (SeekBar)layouti.findViewWithTag("sliderNumber" + i);
                    bar.setProgress((maxEQLevel - minEQLevel)/2 + mEqualizer.getBandLevel(i));
                }
            }

            public void onNothingSelected(AdapterView<?> arg0)
            {}
        });
    }
    
}