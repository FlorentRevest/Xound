package com.florentrevest.xound;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class AlbumsListFragment extends Fragment
{
    private AlbumsAdapter m_adapter;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        Uri uri = null;
        final List<Integer> idsList = new ArrayList<Integer>();
        final LinkedHashMap<String, List<Integer>> artistsHash = new LinkedHashMap<String, List<Integer>>();
        
        View view = inflater.inflate(R.layout.fragment_grid_list, container, false);

        if(getArguments() != null && getArguments().getString("Name") != null)
        {
            final String name = getArguments().getString("Name");
            TextView nameView = (TextView)view.findViewById(R.id.nameView);
            nameView.setText(name);
            nameView.setOnClickListener(new OnClickListener() {
                public void onClick(View arg0)
                {
                    m_callback.onArtistInfoSelected(name);
                }
            });
        }

        GridView gridView = (GridView)view.findViewById(R.id.gridView);
        if(getArguments() != null && getArguments().getString("URI") != null)
            uri = Uri.parse(getArguments().getString("URI"));
        else
            uri = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI;

        Cursor cur = getActivity().getContentResolver().query(uri, new String[] {MediaStore.Audio.Albums._ID, MediaStore.Audio.Albums.ALBUM}, null, null, MediaStore.Audio.Albums.ALBUM + " ASC");

        if(cur != null)
        {
            if(cur.moveToFirst())
            {
                do {
                    int id = cur.getInt(cur.getColumnIndex(MediaStore.Audio.Albums._ID));
                    String name = cur.getString(cur.getColumnIndex(MediaStore.Audio.Albums.ALBUM));
                    idsList.add(id);

                    List<Integer> ids = new ArrayList<Integer>();
                    if(artistsHash.containsKey(name))
                        ids = artistsHash.get(name);
                    ids.add(id);

                    artistsHash.put(name, ids);
                } while (cur.moveToNext());
            }
            cur.close();
        }

        m_adapter = new AlbumsAdapter(getActivity(), artistsHash);
        gridView.setAdapter(m_adapter);
        gridView.setOnItemClickListener(new OnItemClickListener()
        {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {                
                if(id == -1)
                    m_callback.onAlbumSelected(idsList, true, getString(R.string.every_albums));
                else
                    m_callback.onAlbumSelected(artistsHash.get(((TextView)view.findViewById(R.id.text)).getText().toString()), false, ((TextView)view.findViewById(R.id.text)).getText().toString());
            }
        });
        gridView.setOnItemLongClickListener(new OnItemLongClickListener()
        {
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, final long album_id)
            {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                final String albumTitle = ((TextView)view.findViewById(R.id.text)).getText().toString();
                builder.setTitle(albumTitle);

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
                            
                            Cursor cursor = getActivity().getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,  new String[] {MediaStore.Audio.Media._ID}, String.valueOf(MediaStore.Audio.Media.ALBUM_ID) + "=" + String.valueOf(album_id) + " AND is_music!=0", null, null);

                            if(cursor != null)
                            {
                                if(cursor.moveToFirst())
                                    do sl.add(new Song(cursor.getInt(0), getActivity())); while (cursor.moveToNext());
                            
                                cursor.close();
                            }
                            
                            PlaybackService ps = PlaybackService.get();
                            ps.addSongs(sl);
                            break;
                        case 1:
                            final Intent MessIntent = new Intent(Intent.ACTION_SEND);
                            MessIntent.setType("text/plain");
                            MessIntent.putExtra(Intent.EXTRA_TEXT, albumTitle);
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
    
    OnAlbumSelectedListener m_callback;

    public interface OnAlbumSelectedListener
    {
        public void onAlbumSelected(List<Integer> id, Boolean sort, String name);
        public void onArtistInfoSelected(String name);
    }

    @Override
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);
        
        try{
            m_callback = (OnAlbumSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnAlbumSelectedListener");
        }
    }

    private class AlbumsAdapter extends BaseAdapter
    {
        private Context m_context;
        private LinkedHashMap<String, List<Integer>> m_idsHash;
        private HashMap<View, Thread> m_threadsHash;

        public AlbumsAdapter(Context context, LinkedHashMap<String, List<Integer>> idsHash)
        {
            m_context = context;
            m_idsHash = new LinkedHashMap<String, List<Integer>>();
            if(idsHash.size() > 1)
            {
                List<Integer> everything = new ArrayList<Integer>();
                everything.add(-1);
                m_idsHash.put("", everything);
            }
            m_idsHash.putAll(idsHash);
            m_threadsHash = new HashMap<View, Thread>();
        }

        public View getView(int position, View convertView, ViewGroup parent)
        {
            if(convertView == null)
            {
                LayoutInflater inflater = (LayoutInflater)m_context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.item, null);
            }
            
            ((TextView)convertView.findViewById(R.id.text)).setText("");
            ((ImageView)convertView.findViewById(R.id.icon)).setImageBitmap(null);

            @SuppressWarnings("unchecked")
            List<Integer> list = (List<Integer>)m_idsHash.values().toArray()[position];
            if(list.get(0) != null)
            {
                Thread previousThread = m_threadsHash.get(convertView);
                if(previousThread != null)
                {
                    previousThread.interrupt();
                    m_threadsHash.remove(convertView);
                }

                previousThread = new Thread(new ItemLoader(ItemLoader.album, convertView, list.get(0)));
                previousThread.start();

                m_threadsHash.put(convertView, previousThread);
            }

            return convertView;
        }

        public int getCount()
        {
            return m_idsHash.size();
        }

        public Object getItem(int position)
        {
            return null;
        }
        
        @SuppressWarnings("unchecked")
        public long getItemId(int position)
        {
            return ((List<Integer>)m_idsHash.values().toArray()[position]).get(0);
        }
    }
}
