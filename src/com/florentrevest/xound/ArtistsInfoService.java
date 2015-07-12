package com.florentrevest.xound;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.BinaryHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;

public class ArtistsInfoService extends Service
{
    private List<String> artists;
    private static ArtistsInfoService sInstance;
    private int i = 0;

    @Override
    public void onCreate()
    {
        super.onCreate();
        sInstance = this;
        artists = new ArrayList<String>();
 
        Cursor cur = getApplicationContext().getContentResolver().query(MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI, new String[] {MediaStore.Audio.Albums._ID, MediaStore.Audio.Artists.ARTIST}, null, null, null);

        if(cur != null)
        {
            if(cur.moveToFirst())
            {
                do
                {
                    String name = cur.getString(cur.getColumnIndex(MediaStore.Audio.Artists.ARTIST));
                    if(name != null && !name.equals("<unknown>"))
                        artists.add(name.replace("/", ""));
                } while (cur.moveToNext());
            }
            cur.close();
        }
        
        if(artists.size() > 0)
            nextDownload();
    }
    
    private void nextDownload()
    {
        final String artistName = artists.get(i);
        
        File file = getApplicationContext().getFileStreamPath(artistName + " - Biography");
        if(!file.exists())
        {
            String path = "http://api.discogs.com/artist/" + URLEncoder.encode(artistName);

            AsyncHttpClient client = new AsyncHttpClient();
            client.get(path, new JsonHttpResponseHandler()
            {
                @Override
                public void onFailure(Throwable t, JSONObject o)
                {
                    i++;
                    if(i < artists.size())
                        nextDownload();
                    else
                        ArtistsInfoService.this.stopSelf();
                }
                
                @Override
                public void onSuccess(JSONObject root)
                {
                    try
                    {
                        JSONObject resp = root.getJSONObject("resp");
                        if(resp.getBoolean("status"))
                        {
                            JSONObject artist = resp.getJSONObject("artist");

                            try
                            {
                                FileOutputStream fos = openFileOutput(artistName + " - Biography", Context.MODE_PRIVATE);
                                PrintStream printStream = new PrintStream(fos);
                                printStream.println (artist.getString("profile"));
                                printStream.close();
                            }
                            catch(FileNotFoundException e1)
                            {
                                Log.d("ArtistsInfoService", artistName + "'s biography can't be written on internal storage.");
                            }
                            catch(JSONException e)
                            {
                                Log.d("ArtistsInfoService", artistName + "'s biography can't be downloaded.");
                            }
                            try
                            {
                                JSONArray images = artist.getJSONArray("images");
                                JSONObject firstImage = (JSONObject)images.get(0);
                               
                                final String photoUrl = firstImage.getString("uri");
                              
                                AsyncHttpClient photoClient = new AsyncHttpClient();
                                photoClient.get(photoUrl, new BinaryHttpResponseHandler() {
                                    @Override
                                    public void onSuccess(byte[] fileData) {
                                        try
                                        {
                                            FileOutputStream fos = openFileOutput(artistName + " - Photo", Context.MODE_PRIVATE);
                                            if(fileData != null)
                                                fos.write(fileData);
                                            fos.close();
                                            if(m_callbacks != null)
                                                for(OnArtistsInfoListener listener : m_callbacks)
                                                    listener.onArtistInfoListener(artistName);
                                        }
                                        catch(IOException e)
                                        { 
                                            Log.d("ArtistsInfoService", artistName + "'s photo can't be written on internal storage.");
                                        }
                                    }
                                });
                            }
                            catch(JSONException e)
                            {
                                Log.d("ArtistsInfoService", artistName + "'s photo can't be download.");
                            }
                        }
                    }
                    catch(JSONException e)
                    {
                        Log.d("ArtistsInfoService", artistName + "'s infos can't be downloaded.");
                    }
                    i++;
                    if(i < artists.size())
                        nextDownload();
                    else
                        ArtistsInfoService.this.stopSelf();
                }
            });
        }
    }

    @Override
    public void onDestroy()
    {
        sInstance = null;

        super.onDestroy();
    }
    
    public static ArtistsInfoService get()
    {
        return sInstance;
    }
    
    public static boolean hasInstance()
    {
        return sInstance != null;
    }
    
    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }
    
    private static List<OnArtistsInfoListener> m_callbacks = null;

    public interface OnArtistsInfoListener
    {
        public void onArtistInfoListener(String modifiedArtist);
    }
    
    public static void addOnArtistsInfoListener(OnArtistsInfoListener listener)
    {
        if(m_callbacks == null)
            m_callbacks = new ArrayList<OnArtistsInfoListener>();
        
        m_callbacks.add(listener);
    }
}
