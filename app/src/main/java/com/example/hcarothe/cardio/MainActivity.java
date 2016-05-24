package com.example.hcarothe.cardio;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    Intent nextActivity;
    List<String> musicDirectories;
    File[] songFiles;
    ListView directoryListView;
    ArrayAdapter<String> directoryAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setUp();
        retrieveDirectories();
    }
    public void setUp()
    {
        directoryListView = (ListView) findViewById(R.id.directoryListView);
        musicDirectories = new ArrayList<String>();
        directoryAdapter = new ArrayAdapter<String>(this, R.layout.activity_listview, musicDirectories);

        directoryListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String item = ((TextView)view).getText().toString();
                //retrieveSongList(item);
                //Toast.makeText(getBaseContext(), item, Toast.LENGTH_LONG);
                nextActivity = new Intent(MainActivity.this, MusicActivity.class);
                nextActivity.putExtra("PATH_TO_FILE", item);
                startActivity(nextActivity);
            }
        });
        directoryListView.setAdapter(directoryAdapter);
    }
    public void retrieveDirectories()
    {
        ContentResolver cr = this.getContentResolver();

        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0";
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";
        Cursor cur = cr.query(uri, null, selection, null, sortOrder);
        int count = 0;

        if(cur != null)
        {
            count = cur.getCount();

            if(count > 0)
            {
                while(cur.moveToNext())
                {
                    String data = cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.DATA));
                    data = data.substring(0,data.lastIndexOf('/') + 1);
//                  Log.d("Directories", data);
                    if(!musicDirectories.contains(data))
                    {
                        musicDirectories.add(data);
                    }
                }
            }
        }

        cur.close();
        for(int i = 0;i < musicDirectories.size();i++)
        {
            Log.d("Directories-trimmed", musicDirectories.get(i));
        }

    }
}
































