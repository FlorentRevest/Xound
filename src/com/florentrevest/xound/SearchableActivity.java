package com.florentrevest.xound;

import java.text.Collator;
import java.util.ArrayList;
import java.util.HashMap;

import org.holoeverywhere.app.ListActivity;

import android.app.SearchManager;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.SimpleAdapter;

public class SearchableActivity extends ListActivity
{
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent playbackIntent = new Intent(this, PlaybackService.class);
        startService(playbackIntent);
        
        Intent intent = getIntent();
        if(Intent.ACTION_SEARCH.equals(intent.getAction()))
        {
            String query = intent.getStringExtra(SearchManager.QUERY);
            getSupportActionBar().setTitle(query);
            search(query);
        }
    }
    
    private void search(String query)
    {
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        StringBuilder where = new StringBuilder();
        where.append(MediaStore.Audio.Media.TITLE + " != ''");

        String [] keywords = null;
        if(query != null)
        {
            String [] searchWords = query.split(" ");
            keywords = new String[searchWords.length];
            Collator col = Collator.getInstance();
            col.setStrength(Collator.IDENTICAL);

            for(int i = 0; i < searchWords.length; i++)
                keywords[i] = '%' + MediaStore.Audio.keyFor(searchWords[i]) + '%';

            for (int i = 0; i < searchWords.length; i++)
            {
                where.append(" AND ");
                where.append(MediaStore.Audio.Media.ARTIST_KEY + "||");
                where.append(MediaStore.Audio.Media.TITLE_KEY + " LIKE ?");
            }
        }

        String selection = where.toString();

        String[] projection = {
            BaseColumns._ID,
            MediaStore.Audio.Artists.ARTIST,
            MediaStore.Audio.Albums.ALBUM,
            MediaStore.Audio.Media.TITLE
        };

        Cursor cursor = getContentResolver().query(uri, projection, selection, keywords, MediaStore.Audio.Media.TITLE);
        
        final ArrayList<Integer> idList = new ArrayList<Integer>();
        ArrayList<HashMap<String,String>> list = new ArrayList<HashMap<String,String>>();
        if(cursor != null)
        {
            if(cursor.moveToFirst())
            {
                do
                {
                    HashMap<String,String> item;
                    item = new HashMap<String,String>();
                    item.put("line1", cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)));
                    item.put("line2", cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)));
                    list.add(item);

                    idList.add(cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media._ID)));
                } while (cursor.moveToNext());
            }
            cursor.close();
        }

        SimpleAdapter adapter = new SimpleAdapter(this, list, android.R.layout.two_line_list_item, new String[] { "line1", "line2" }, new int[] {android.R.id.text1, android.R.id.text2});
        setListAdapter(adapter);

        getListView().setOnItemClickListener(new OnItemClickListener()
        {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                PlaybackService ps = PlaybackService.get();
                ps.clearQueue();
                ArrayList<Song> sl = new ArrayList<Song>();
                sl.add(new Song(idList.get(position), SearchableActivity.this));
                ps.addSongs(sl);
                ps.jumpToQueuePosition(0);
            }
        });
    }
}
