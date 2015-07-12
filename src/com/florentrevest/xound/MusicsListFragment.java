package com.florentrevest.xound;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class MusicsListFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, OnItemClickListener
{
    SimpleCursorAdapter m_adapter;
    long[] m_idsList;
    Boolean m_sort;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_musics_list, container, false);
        ListView listView = (ListView)view.findViewById(R.id.listView);
        listView.setOnItemClickListener(this);

        m_adapter = new SimpleCursorAdapter(getActivity(), android.R.layout.simple_list_item_1, null, new String [] {MediaStore.Audio.Media.TITLE}, new int[] {android.R.id.text1}, 0);
        listView.setAdapter(m_adapter);
        
        if(getArguments() != null && getArguments().getLongArray("IDs") != null)
        {
            m_idsList = getArguments().getLongArray("IDs");
            m_sort = getArguments().getBoolean("Sort");
            
            String name = getArguments().getString("Name");
            TextView nameView = (TextView)view.findViewById(R.id.nameView);
            nameView.setText(name);

            getLoaderManager().initLoader(0, null, this);
        }
        
        listView.setDivider(null);
        listView.setDividerHeight(0);
        
        listView.setOnItemLongClickListener(new OnItemLongClickListener()
        {
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, final long music_id)
            {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                final String musicTitle = ((TextView)view).getText().toString();
                builder.setTitle(musicTitle);

                ListView modeList = new ListView(getActivity());
                String[] stringArray = new String[] { getString(R.string.add_to_queue), getString(R.string.share) };
                ArrayAdapter<String> modeAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, android.R.id.text1, stringArray);
                modeList.setAdapter(modeAdapter);

                builder.setView(modeList);
                final Dialog dialog = builder.create();

                modeList.setOnItemClickListener(new OnItemClickListener()
                {
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id)
                    {
                        switch(position)
                        {
                        case 0:
                            ArrayList<Song> sl = new ArrayList<Song>();
                            sl.add(new Song(music_id, getActivity()));

                            PlaybackService ps = PlaybackService.get();
                            ps.addSongs(sl);
                            break;
                        case 1:
                            final Intent MessIntent = new Intent(Intent.ACTION_SEND);
                            MessIntent.setType("text/plain");
                            MessIntent.putExtra(Intent.EXTRA_TEXT, musicTitle);
                            startActivity(Intent.createChooser(MessIntent, getString(R.string.share)));
                            break;
                        }
                        dialog.dismiss();
                    }
                });
                
                dialog.show();
                return true;
            }
        });
        return view;
    }

    public Loader<Cursor> onCreateLoader(int i, Bundle args)
    {
        String where = "";
        for(long id : m_idsList)
            where += String.valueOf(MediaStore.Audio.Media.ALBUM_ID) + "=" + String.valueOf(id) + " OR ";
        where = where.substring(0, where.length() - 3);
        String sort = null;
        if(m_sort)
            sort = MediaStore.Audio.Media.TITLE + " ASC";
        return new CursorLoader(getActivity(), MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, new String[] {MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE}, where  + " AND is_music!=0", null, sort);
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor data)
    {
        m_adapter.swapCursor(data);
    }

    public void onLoaderReset(Loader<Cursor> loader)
    {
        m_adapter.swapCursor(null);
    }
    
    OnMusicSelectedListener m_callback;

    public interface OnMusicSelectedListener
    {
        public void showMusicPlayer();
    }

    @Override
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);
        
        try{
            m_callback = (OnMusicSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnMusicSelectedListener");
        }

        Intent playbackIntent = new Intent(activity, PlaybackService.class);
        activity.startService(playbackIntent);
    } 

    public void onItemClick(AdapterView<?> l, View v, int position, long j)
    {
        ArrayList<Song> sl = new ArrayList<Song>();
        
        int i = 0;
        while(i < l.getCount())
        {
            sl.add(new Song(l.getItemIdAtPosition(i), getActivity()));
            i ++;
        }

        PlaybackService ps = PlaybackService.get();
        ps.clearQueue();
        ps.addSongs(sl);
        ps.jumpToQueuePosition(position);
        m_callback.showMusicPlayer();
    }
}
