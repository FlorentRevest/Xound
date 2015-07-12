package com.florentrevest.xound;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

public class HelpActivity extends Activity implements OnClickListener
{
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);
        View mainView = findViewById(R.id.mainView);
        mainView.setOnClickListener(this);
    }
    
    public void onClick(View arg0)
    {
        finish();
    }
}
