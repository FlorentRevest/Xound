package com.florentrevest.xound;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.RemoteViews;

public class LongWidget extends AppWidgetProvider
{
    private static boolean sEnabled;

    @Override
    public void onEnabled(Context context)
    {
        sEnabled = true;
    }

    @Override
    public void onDisabled(Context context)
    {
        sEnabled = false;
    }
    
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds)
    {
        if(!sEnabled)
            return;

        Song song = null;
        boolean isPlaying = false;
        if(PlaybackService.hasInstance())
        {
            PlaybackService service = PlaybackService.get();
            song = service.getSong(0);
            isPlaying = service.isPlaying();
        }
        sEnabled = true;
        updateWidget(context, appWidgetManager, song, isPlaying);
    }
    
    public static void updateWidget(Context context, AppWidgetManager appWidgetManager, Song song, boolean isPlaying)
    {        
        RemoteViews controlButtons = new RemoteViews(context.getPackageName(), R.layout.long_widget);
        if(song != null)
        {
            controlButtons.setViewVisibility(R.id.title, View.VISIBLE);
            controlButtons.setTextViewText(R.id.title, song.getTitle());
            controlButtons.setTextViewText(R.id.artist, song.getArtist());
            controlButtons.setViewVisibility(R.id.next_button, View.VISIBLE);
            controlButtons.setViewVisibility(R.id.play_button, View.VISIBLE);
        }
        else
        {
            controlButtons.setViewVisibility(R.id.title, View.GONE);
            controlButtons.setTextViewText(R.id.title, "");
            controlButtons.setTextViewText(R.id.artist, context.getString(R.string.app_name));
            controlButtons.setViewVisibility(R.id.next_button, View.GONE);
            controlButtons.setViewVisibility(R.id.play_button, View.GONE);
        }

        ComponentName service = new ComponentName(context, PlaybackService.class);
        
        Intent playIntent;
        
        if(isPlaying)
        {
            controlButtons.setImageViewResource(R.id.play_button, R.drawable.pause);
            playIntent = new Intent(PlaybackService.ACTION_PAUSE).setComponent(service);
        }
        else
        {
            controlButtons.setImageViewResource(R.id.play_button, R.drawable.play);
            playIntent = new Intent(PlaybackService.ACTION_PLAY).setComponent(service);
        }
        
        Intent nextIntent = new Intent(PlaybackService.ACTION_NEXT_SONG).setComponent(service);
        Intent mainIntent = new Intent(context, MainActivity.class);
        mainIntent.setAction(Intent.ACTION_MAIN);

        PendingIntent playPendingIntent = PendingIntent.getService(context, 0, playIntent, 0);
        PendingIntent nextPendingIntent = PendingIntent.getService(context, 0, nextIntent, 0);
        PendingIntent mainPendingIntent = PendingIntent.getActivity(context, 0, mainIntent, 0);

        controlButtons.setOnClickPendingIntent(R.id.text_layout, mainPendingIntent);
        controlButtons.setOnClickPendingIntent(R.id.play_button, playPendingIntent);
        controlButtons.setOnClickPendingIntent(R.id.next_button, nextPendingIntent);

        appWidgetManager.updateAppWidget(new ComponentName(context, LongWidget.class), controlButtons);
    }
    
    public static void checkEnabled(Context context, AppWidgetManager manager)
    {
        sEnabled = manager.getAppWidgetIds(new ComponentName(context, LongWidget.class)).length != 0;
    }
}
