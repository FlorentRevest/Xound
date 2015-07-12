package com.florentrevest.xound;

import java.util.ArrayList;
import java.util.HashMap;
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
import android.text.Html;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class ArtistsListFragment extends Fragment implements ArtistsInfoService.OnArtistsInfoListener
{
    ArtistsAdapter m_adapter;
    GridView m_gridView;
    List<String> m_nameList;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        List<Integer> idsList = new ArrayList<Integer>();
        m_nameList = new ArrayList<String>();
        
        View view = inflater.inflate(R.layout.fragment_grid_list, container, false);
        view.findViewById(R.id.nameView).setVisibility(View.GONE);
        m_gridView = (GridView)view.findViewById(R.id.gridView);
        Cursor cur = getActivity().getContentResolver().query(MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI, new String[] {MediaStore.Audio.Artists._ID, MediaStore.Audio.Artists.ARTIST}, null, null, null);

        if(cur != null)
        {
            if(cur.moveToFirst())
            {
                do
                {
                    m_nameList.add(cur.getString(cur.getColumnIndex(MediaStore.Audio.Artists.ARTIST)));
                    idsList.add(cur.getInt(cur.getColumnIndex(MediaStore.Audio.Artists._ID))); 
                } while (cur.moveToNext());
            }
            cur.close();
        }
                  
        m_adapter = new ArtistsAdapter(getActivity(), idsList);
        m_gridView.setAdapter(m_adapter);
        m_gridView.setOnItemClickListener(new OnItemClickListener()
        {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                if(id != -1)
                {
                    Uri uri = MediaStore.Audio.Artists.Albums.getContentUri("external", id);
                    m_callback.onArtistSelected(uri.toString(), ((TextView)view.findViewById(R.id.text)).getText().toString());
                }
                else
                    m_callback.onArtistSelected(null, ((TextView)view.findViewById(R.id.text)).getText().toString());
            }
        });
        
        m_gridView.setOnItemLongClickListener(new OnItemLongClickListener()
        {
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, final long artist_id)
            {
                if(artist_id != -1)
                {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    final String artistTitle = ((TextView)view.findViewById(R.id.text)).getText().toString();
                    builder.setTitle(artistTitle);
                
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
                            
                                Cursor cursor = getActivity().getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,  new String[] {MediaStore.Audio.Media._ID}, String.valueOf(MediaStore.Audio.Media.ARTIST_ID) + "=" + String.valueOf(artist_id) + " AND is_music!=0", null, null);

                                if(cursor.moveToFirst())
                                    do sl.add(new Song(cursor.getInt(0), getActivity())); while (cursor.moveToNext());
                            
                                cursor.close();
                            
                                PlaybackService ps = PlaybackService.get();
                                ps.addSongs(sl);
                                break;
                            case 1:
                                final Intent MessIntent = new Intent(Intent.ACTION_SEND);
                                MessIntent.setType("text/plain");
                                MessIntent.putExtra(Intent.EXTRA_TEXT, artistTitle);
                                startActivity(Intent.createChooser(MessIntent, getString(R.string.share)));
                                break;
                            }
                            dialog.dismiss();
                        }
                    });
                
                    dialog.show();
                }
                return true;
            }
        });

        TextView emptyView = new TextView(getActivity());
        getActivity().addContentView(emptyView, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
        emptyView.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
        emptyView.setText(Html.fromHtml("<h2>Your Library is empty!</h2>You can add music from your PC by USB"));
        m_gridView.setEmptyView(emptyView);

        return view;
    }
    
    OnArtistSelectedListener m_callback;

    public interface OnArtistSelectedListener
    {
        public void onArtistSelected(String uri, String name);
    }

    @Override
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);
        
        try {
            m_callback = (OnArtistSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnArtistSelectedListener");
        }
    }
    
    private class ArtistsAdapter extends BaseAdapter
    {
        private Context m_context;
        private List<Integer> m_idsList;
        private HashMap<View, Thread> m_threadsHash;

        public ArtistsAdapter(Context context, List<Integer> idsList)
        {
            m_context = context;
            m_idsList = new ArrayList<Integer>();
            m_idsList.add(-1); //Every Artists
            m_idsList.addAll(idsList);
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
            
            Thread previousThread = m_threadsHash.get(convertView);
            if(previousThread != null)
            {
                previousThread.interrupt();
                m_threadsHash.remove(convertView);
            }
            
            previousThread = new Thread(new ItemLoader(ItemLoader.artist, convertView, m_idsList.get(position)));
            previousThread.start();

            m_threadsHash.put(convertView, previousThread);
            return convertView;
        }

        public int getCount()
        {
            return m_idsList.size();
        }

        public Object getItem(int position)
        {
            return null;
        }

        public long getItemId(int position)
        {
            long id;
            try {
                id = m_idsList.get(position);
            }
            catch(IndexOutOfBoundsException e)
            {
                id = -1;
            }
            return id;
        } 
    }

    public void onArtistInfoListener(String modifiedArtist)
    {
        int position = m_nameList.indexOf(modifiedArtist);
        int visiblePosition = m_gridView.getFirstVisiblePosition();
        View v = m_gridView.getChildAt(position - visiblePosition);

        if(v == null || getActivity() == null || m_adapter.getItemId(position) == -1)
            return;

        new Thread(new ItemLoader(ItemLoader.artist, v, (int)m_adapter.getItemId(position))).start();
    }
}
