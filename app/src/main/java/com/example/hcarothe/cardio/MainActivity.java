package com.example.hcarothe.cardio;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.SyncStateContract;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private Intent nextActivity;
    private List<SpannableString> musicDirectories;
    private List<String> directories;
    private ListView directoryListView;
    private ArrayAdapter<SpannableString> directoryAdapter;
    public static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        permissionCheck();
    }

    public void permissionCheck() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_CONTACTS)
                    != PackageManager.PERMISSION_GRANTED) {

                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.READ_EXTERNAL_STORAGE)) {

                } else {

                    // No explanation needed, we can request the permission.

                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);

                    // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                    // app-defined int constant. The callback method gets the
                    // result of the request.
                }
            }
        }
        else
        {
            setUp();
            retrieveDirectories();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    setUp();
                    retrieveDirectories();
                } else {

                }
                return;
            }
        }
    }


    public void setUp() {
        directoryListView = (ListView) findViewById(R.id.directoryListView);
        directories = new ArrayList<>();
        musicDirectories = new ArrayList<>();
        directoryAdapter = new ArrayAdapter<>(this, R.layout.activity_listview, musicDirectories);

        directoryListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                nextActivity = new Intent(MainActivity.this, MusicActivity.class);
                nextActivity.putExtra("PATH_TO_FILE", directories.get(position));
                startActivity(nextActivity);
            }
        });
        directoryListView.setAdapter(directoryAdapter);
    }

    private void retrieveDirectories() {
        ContentResolver cr = this.getContentResolver();

        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0";
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";
        Cursor cur = cr.query(uri, null, selection, null, sortOrder);
        int count;

        if (cur != null) {
            count = cur.getCount();

            if (count > 0) {
                while (cur.moveToNext()) {
                    String path = cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.DATA));
                    String directory;
                    SpannableString spannableString;
                    path = path.substring(0, path.lastIndexOf('/') + 1);

                    if (!directories.contains(path)) {
                        directory = path.substring(0, path.length() - 1);
                        directory = directory.substring(directory.lastIndexOf('/') + 1);
                        spannableString = new SpannableString(directory + "\n" + path);
                        spannableString.setSpan(new RelativeSizeSpan(1.25f), 0, directory.length(), 0);

                        directories.add(path);
                        musicDirectories.add(spannableString);
                    }
                }
            }
        }
        if (cur != null) {
            cur.close();
        }
    }
}
































