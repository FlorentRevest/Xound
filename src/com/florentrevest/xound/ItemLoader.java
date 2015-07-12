package com.florentrevest.xound;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class ItemLoader extends Thread
{
    public static final String artist = "Artist";
    public static final String album = "Album";

    private ImageView m_imageView;
    private TextView m_textView;
    private Bitmap m_bitmap = null;
    private CharSequence m_text = null;
    private String m_type = null;
    private Integer m_id;

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight)
    {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth)
        {
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);

            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }

        return inSampleSize;
    }
    
    ItemLoader(String type, View view, Integer id)
    {
        m_type = type;
        m_textView = (TextView)view.findViewById(R.id.text);
        m_imageView = (ImageView)view.findViewById(R.id.icon);
        m_id = id;
    }
    
    public void run()
    {
        if(m_bitmap != null)
        {
            m_bitmap.recycle();
            m_bitmap = null;
        }
        if(m_id != -1)
        {
            if(m_type == artist)
            {
                Cursor cur = m_textView.getContext().getContentResolver().query(MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI, new String[] {MediaStore.Audio.Artists._ID, MediaStore.Audio.Artists.ARTIST}, MediaStore.Audio.Artists._ID + " = " + m_id, null, null);

                if(cur != null)
                {
                    if(cur.moveToFirst())
                    {
                        m_text = cur.getString(cur.getColumnIndex(MediaStore.Audio.Artists.ARTIST));
                        if(m_text.equals("<unknown>"))
                            m_text = null;
                        else
                        {
                            try 
                            {
                                File f = m_textView.getContext().getFileStreamPath(m_text.toString().replace("/", "") + " - Photo");

                                if(f.exists())
                                {
                                    final BitmapFactory.Options options = new BitmapFactory.Options();
                                    options.inJustDecodeBounds = true;
                                    BitmapFactory.decodeFile(f.getAbsolutePath(), options);

                                    options.inSampleSize = calculateInSampleSize(options, 250, 250);
                                    options.inJustDecodeBounds = false;
                                    m_bitmap = BitmapFactory.decodeFile(f.getAbsolutePath(), options);
                                }
                            } catch (Exception e) {
                            }
                        }
                    }
                    cur.close();
                }
            }
            else if(m_type == album)
            {
                Cursor cur = m_textView.getContext().getContentResolver().query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, new String[] {MediaStore.Audio.Albums._ID, MediaStore.Audio.Albums.ALBUM}, MediaStore.Audio.Albums._ID + " = " + m_id, null, null);

                if(cur != null)
                {
                    if(cur.moveToFirst())
                    {
                        m_text = cur.getString(cur.getColumnIndex(MediaStore.Audio.Albums.ALBUM));

                        try 
                        {
                            ParcelFileDescriptor pfd = null;
                            try
                            {
                                pfd = m_textView.getContext().getContentResolver().openFileDescriptor(Uri.parse("content://media/external/audio/albumart/" + m_id), "r");
                            }
                            catch(FileNotFoundException e)
                            {}

                            if(pfd != null)
                            {
                                FileDescriptor fd = pfd.getFileDescriptor();
                            
                                final BitmapFactory.Options options = new BitmapFactory.Options();
                                options.inJustDecodeBounds = true;
                                BitmapFactory.decodeFileDescriptor(fd, null, options);

                                options.inSampleSize = calculateInSampleSize(options, 250, 250);
                                options.inJustDecodeBounds = false;
                                m_bitmap = BitmapFactory.decodeFileDescriptor(fd, null, options);
                            }
                        } catch (Exception e) {
                        }
                    }
                    cur.close();
                }
            }
        }
        m_imageView.post(new Runnable()
        {
            public void run()
            {
                if(m_bitmap != null)
                    m_imageView.setImageBitmap(m_bitmap);
                else
                {
                    if(m_id == -1)
                    {
                        if(m_type == artist)
                            m_imageView.setImageResource(R.drawable.no_artists);
                        else if(m_type == album)
                            m_imageView.setImageResource(R.drawable.no_covers);
                    }
                    else
                    {
                        if(m_type == artist)
                            m_imageView.setImageResource(R.drawable.no_artist);
                        else if(m_type == album)
                            m_imageView.setImageResource(R.drawable.no_cover);
                    }
                }
                
                if(m_text != null)
                    m_textView.setText(m_text);
                else
                {
                    if(m_id == -1)
                    {
                        if(m_type == artist)
                            m_textView.setText(R.string.every_artists);
                        else if(m_type == album)
                            m_textView.setText(R.string.every_albums);
                    }
                    else
                    {
                        if(m_type == artist)
                            m_textView.setText(R.string.unknown_artists);
                        else if(m_type == album)
                            m_textView.setText("");
                    }
                }
            }
        });
    }
}
