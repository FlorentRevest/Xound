package com.florentrevest.xound;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class ArtistInfoFragment extends Fragment
{
    private BufferedReader br;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_artist_info, container, false);

        TextView nameView = (TextView)view.findViewById(R.id.nameView);
        TextView biographyView = (TextView)view.findViewById(R.id.biographyView);

        String name = "";
        if(getArguments() != null && getArguments().getString("ArtistName") != null)
            name = getArguments().getString("ArtistName");
        nameView.setText(name);

        File file = getActivity().getFileStreamPath(name.replace("/", "") + " - Biography");
        StringBuilder rawBiography = new StringBuilder();

        try {
            br = new BufferedReader(new FileReader(file));
            String line;

            while ((line = br.readLine()) != null) {
                rawBiography.append(line);
                rawBiography.append('\n');
            }
        }
        catch (IOException e) {
        }
        
        String biography = rawBiography.toString();
        
        while(biography.endsWith("\n"))
            biography = biography.substring(0, biography.length() - 1);
        
        if(biography.trim().length() == 0)
            biography += "<h4>" + getString(R.string.no_biography) + "</h4>";
        else
            biography = bbcode(biography);

        biographyView.setText(Html.fromHtml(biography));

        return view;
    }
    
    public static String bbcode(String text)
    {
        String temp = text.replaceAll("\n","<BR />");

        Map<String , String> bbMap = new HashMap<String , String>();
        bbMap.put("\\[a=(.+?)\\]", "<strong>$1</strong>");
        bbMap.put("\\[l=(.+?)\\]", "<strong>$1</strong>");
        bbMap.put("\\[r=(.+?)\\]", "");
        bbMap.put("\\[b\\](.+?)\\[/b\\]", "<strong>$1</strong>");

        for(Iterator<Entry<String, String>> iterator = bbMap.entrySet().iterator(); iterator.hasNext();)
        {
            Entry<String, String> entry = iterator.next();
            temp = temp.replaceAll(entry.getKey().toString(), entry.getValue().toString());
        }

        return temp;
    }
}
