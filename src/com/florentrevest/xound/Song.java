package com.florentrevest.xound;

import java.io.FileDescriptor;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.support.v4.util.LruCache;

public class Song
{
    private long m_id;
    private long m_albumId;
    private long m_artistId;
    private String m_path;
    private String m_title;
    private String m_album;
    private String m_artist;
    private long m_duration;
    private int m_trackNumber;
    private boolean m_noCover = false;

    private static CoverCache sCoverCache = null;
        
    public Song(long id, Context c)
    {
        m_id = id;
        
        String[] projection = {
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.ARTIST_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.TRACK,
        };
        
        Cursor cursor = c.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, MediaStore.Audio.Media._ID + " = " + m_id, null, null);

        if(cursor != null)
        {
            if(cursor.moveToFirst() && cursor.getCount() >= 1)
            {
                m_id = cursor.getLong(0);
                m_path = cursor.getString(1);
                m_title = cursor.getString(2);
                m_album = cursor.getString(3);
                m_artist = cursor.getString(4);
                m_albumId = cursor.getLong(5);
                m_artistId = cursor.getLong(6);
                m_duration = cursor.getLong(7);
                m_trackNumber = cursor.getInt(8);
            }
            cursor.close();
        }
    }
    
    public Bitmap getCover(Context context)
    {
        if(m_noCover)
            return null;

        if(sCoverCache == null)
            sCoverCache = new CoverCache(context.getApplicationContext());

        Bitmap cover = sCoverCache.get(m_id);
        if(cover == null)
            m_noCover = true;
        return cover;
    }
    
    public long getId()
    {
        return m_id;
    }
    
    public long geAlbumId()
    {
        return m_albumId;
    }

    public String getPath()
    {
        return m_path;
    }

    public String getTitle()
    {
        return m_title;
    }

    public String getAlbum()
    {
        return m_album;
    }

    public long getDuration()
    {
        return m_duration;
    }

    public int getTrackNumber()
    {
        return m_trackNumber;
    }

    public String getArtist()
    {
        return m_artist;
    }

    public long getArtistId()
    {
        return m_artistId;
    }

    private static class CoverCache extends LruCache<Long, Bitmap> {
        private final Context mContext;

        public CoverCache(Context context)
        {
            super(6 * 1024 * 1024);
            mContext = context;
        }

        @Override
        public Bitmap create(Long key)
        {
            Uri uri =  Uri.parse("content://media/external/audio/media/" + key + "/albumart");
            ContentResolver res = mContext.getContentResolver();

            try {
                ParcelFileDescriptor parcelFileDescriptor = res.openFileDescriptor(uri, "r");
                if (parcelFileDescriptor != null) {
                    FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();

                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inPreferredConfig = Bitmap.Config.RGB_565;
                    options.inDither = false;
                    
                    return BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);
                }
            } catch (Exception e) {
                // no cover art found
            }

            return null;
        }

        @Override
        protected int sizeOf(Long key, Bitmap value)
        {
            return value.getRowBytes() * value.getHeight();
        }
    }
}
