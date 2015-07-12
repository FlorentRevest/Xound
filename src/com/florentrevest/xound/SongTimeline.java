package com.florentrevest.xound;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import android.content.Context;

public class SongTimeline
{
    private ArrayList<Song> mSongs = new ArrayList<Song>(12);
    private int mCurrentPos = 0;

    public void addSongs(ArrayList<Song> songs)
    {
        for(int i = 0; i < songs.size() ; i ++)
            mSongs.add(songs.get(i));
    }

    public Song getSong(int delta)
    {
        Song song;

        int pos = mCurrentPos + delta;
        int size = mSongs.size();
        
        if(pos < 0)
        {
            if(size == 0)
                return null;
            else
            {
                mCurrentPos = size - 1;
                song = mSongs.get(size - 1);
            }
        }
        else if (pos > size)
            return null;
        else if (pos == size)
        {
            if (size == 0)
                return null;
            else
            {
                mCurrentPos = 0;
                song = mSongs.get(0);
            }
        }
        else
            song = mSongs.get(pos);
        
        return song;
    }
    
    public Song setCurrentQueuePosition(int pos)
    {
        if(pos < 0)
            pos = mSongs.size() - 1;
        mCurrentPos = pos;
        return getSong(0);
    }
    
    public Song getSongByQueuePosition(int id)
    {
        return mSongs.get(id);
    }
    
    public int getPosition()
    {
        return mCurrentPos;
    }

    public int getLength()
    {
        return mSongs.size();
    }
    
    public void clear()
    {
        mSongs.clear();
        mCurrentPos = 0;
    }

    public void writeState(DataOutputStream out)
    {        
        try
        {
            out.writeInt(mCurrentPos);
            out.writeInt(mSongs.size());

            for (int i = 0; i != mSongs.size(); ++i)
            {
                Song song = mSongs.get(i);
                if(song == null)
                    out.writeLong(-1);
                else
                    out.writeLong(song.getId());
            }
        }
        catch(IOException e)
        { 
            e.printStackTrace();
        }
    }

    public void readState(DataInputStream in, Context con)
    { 
        try
        {
            mCurrentPos = in.readInt();
            int n = in.readInt();
            if(n > 0)
            {
                ArrayList<Song> songs = new ArrayList<Song>(n);

                for(int i = 0; i != n; i++)
                {
                    long id = in.readLong();
                    if(id == -1)
                        continue;

                    songs.add(new Song(id, con));
                }
                mSongs = songs;
            }
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }
}
