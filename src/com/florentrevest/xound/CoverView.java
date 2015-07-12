package com.florentrevest.xound;

import java.util.Random;

import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

public class CoverView extends ImageView
{
    GestureDetector gdt;
    
    public CoverView(Context context)
    {
        super(context);
        gdt = new GestureDetector(new GestureListener());
        setOnTouchListener(new OnTouchListener() {
            public boolean onTouch(final View view, final MotionEvent event) {
                gdt.onTouchEvent(event);
                return true;
            }
        });
    }

    public CoverView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        gdt = new GestureDetector(new GestureListener());
        setOnTouchListener(new OnTouchListener() {
            public boolean onTouch(final View view, final MotionEvent event) {
                gdt.onTouchEvent(event);
                return true;
            }
        });
    }

    public CoverView(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        gdt = new GestureDetector(new GestureListener());
        setOnTouchListener(new OnTouchListener() {
            public boolean onTouch(final View view, final MotionEvent event) {
                gdt.onTouchEvent(event);
                return true;
            }
        });
    }

    private static final int SWIPE_MIN_DISTANCE = 250;
    private static final int SWIPE_THRESHOLD_VELOCITY = 200;

    private class GestureListener extends SimpleOnGestureListener
    {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY)
        {
            if(!PlaybackService.hasInstance())
            {
                Intent playbackIntent = new Intent(getContext(), PlaybackService.class);
                getContext().startService(playbackIntent);
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
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = width;
        setMeasuredDimension(width, height);
    }
}
