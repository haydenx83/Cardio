package com.example.hcarothe.cardio;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;


public class MusicActivity extends AppCompatActivity implements AudioManager.OnAudioFocusChangeListener {

    private static final String TAG = MusicActivity.class.getSimpleName();
    private MediaPlayer mPlayer;
    Button buttonPlay, buttonStop, buttonSkip,buttonPrev,buttonExpand, buttonCollapse;
    ImageView imageCover;
    TextView songTV, dur;
    SeekBar progress;
    String PATH_TO_FILE;
    private File[] music_files;
    private int number_of_songs;
    private int current_song_index;
    private MediaObserver observer = null;
    private int current_hour, current_minute, current_second, final_hour, final_minute, final_second;
    AudioManager audioManager;
    Intent intent;
    Boolean pauseHumanTriggered;
    AudioReceiver myAudioReceiver;
    MediaMetadataRetriever myRetriever;
    Bitmap bitmap;
    byte[] data;
    ListView musicListView;
    ArrayAdapter<String> musicAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music);

        pauseHumanTriggered = false;
        current_song_index = 0;
        intent = getIntent();
        PATH_TO_FILE = intent.getExtras().getString("PATH_TO_FILE");

        openMusicFolder();
        Log.d(TAG, "onCreate() Restoring previous state");

        setUpAudio();
        setUpDisplay();
        setUpAudioFocus();
        setUpButtons();
    }
    private void setUpAudio()
    {
        mPlayer = new MediaPlayer();
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        myAudioReceiver = new AudioReceiver();
        myRetriever = new MediaMetadataRetriever();
    }
    private void setUpDisplay() {
        progress = (SeekBar) findViewById(R.id.progressBar);

        imageCover = (ImageView) findViewById(R.id.CoverArt);
        songTV = (TextView) findViewById(R.id.songName);
        dur = (TextView) findViewById(R.id.dur);
    }
    private void setUpAudioFocus() {
        int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {

        } else {
            Log.d("AudioManager", "Worked");
            setDataSource(current_song_index % number_of_songs);
        }
    }

    private void setUpButtons() {
        buttonPlay = (Button) findViewById(R.id.play);
        buttonPlay.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (mPlayer.isPlaying() == false) {
                    mPlayer.start();
                    pauseHumanTriggered = false;
                    buttonPlay.setBackgroundResource(android.R.drawable.ic_media_pause);

                } else {
                    mPlayer.pause();
                    pauseHumanTriggered = true;
                    pauseHumanTriggered = true;
                    buttonPlay.setBackgroundResource(android.R.drawable.ic_media_play);
                }
            }
        });

        buttonStop = (Button) findViewById(R.id.stop);
        buttonStop.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                onBackPressed();
            }
        });

        buttonSkip = (Button) findViewById(R.id.skip);
        buttonSkip.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                nextSong();
            }
        });

        buttonPrev = (Button) findViewById(R.id.prev);
        buttonPrev.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                prevSong();
            }
        });

        buttonExpand = (Button) findViewById(R.id.expand);
        buttonExpand.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                expandList();
            }
        });
        buttonCollapse= (Button) findViewById(R.id.collapse);
        buttonCollapse.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                collapseList();
            }
        });
    }

    private void setDataSource(int song_index) {
        mPlayer.reset();
        try {
            Log.d("Files", PATH_TO_FILE + '/' + music_files[song_index].getName());
            mPlayer.setDataSource(PATH_TO_FILE + '/' + music_files[song_index].getName());
            myRetriever.setDataSource(PATH_TO_FILE + '/' + music_files[song_index].getName());
            data = myRetriever.getEmbeddedPicture();

            if (data != null) {
                bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                imageCover.setImageBitmap(bitmap);
                Log.d("Bitmap", "Worked");
            } else {
                bitmap = BitmapFactory.decodeResource(this.getResources(), R.drawable.ic_notification);
                imageCover.setImageBitmap(bitmap);
                Log.d("Bitmap", "Didn't not work");
            }

            prepareMediaPlayer();
        } catch (IllegalArgumentException e) {
            //Toast.makeText(getApplicationContext(), "You might not set the URI correctly!", Toast.LENGTH_LONG).show();
        } catch (SecurityException e) {
            //Toast.makeText(getApplicationContext(), "You might not set the URI correctly!", Toast.LENGTH_LONG).show();
        } catch (IllegalStateException e) {
            //Toast.makeText(getApplicationContext(), "You might not set the URI correctly!", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void prepareMediaPlayer() {
        try {
            mPlayer.prepare();
            playMusic();
        } catch (IllegalStateException e) {
            //Toast.makeText(getApplicationContext(), "You might not set the URI correctly!", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            //Toast.makeText(getApplicationContext(), "You might not set the URI correctly!", Toast.LENGTH_LONG).show();
        }
    }

    private void openMusicFolder() {
        Log.d("Files", "Path: " + PATH_TO_FILE);
        File local_music_files[] = new File(PATH_TO_FILE).listFiles();
        String music_names[] = new String[local_music_files.length];
        Log.d("Files", "Size: " + local_music_files.length);

        for (int i = 0; i < local_music_files.length; i++) {
            Log.d("Files", "FileName:" + local_music_files[i].getName());
            music_names[i] = local_music_files[i].getName();
        }
        music_files = local_music_files;
        number_of_songs = local_music_files.length;
        Log.d("open","worked");
        musicListView = (ListView) findViewById(R.id.musicListView);
        musicAdapter = new ArrayAdapter<String>(this, R.layout.activity_listview, music_names);

        musicListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                current_song_index = position - 1;
                nextSong();
            }
        });
        musicListView.setAdapter(musicAdapter);
    }
    private void expandList()
    {
        buttonPlay.setVisibility(View.INVISIBLE);
        buttonPrev.setVisibility(View.INVISIBLE);
        buttonSkip.setVisibility(View.INVISIBLE);
        buttonStop.setVisibility(View.INVISIBLE);
        progress.setVisibility(View.INVISIBLE);
        buttonCollapse.setVisibility(View.VISIBLE);
        buttonExpand.setVisibility(View.INVISIBLE);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                ActionBar.LayoutParams.MATCH_PARENT,
                ActionBar.LayoutParams.MATCH_PARENT
        );

        params.addRule(RelativeLayout.BELOW, R.id.collapse);
        musicListView.setLayoutParams(params);
    }

    private void collapseList()
    {
        buttonPlay.setVisibility(View.VISIBLE);
        buttonPrev.setVisibility(View.VISIBLE);
        buttonSkip.setVisibility(View.VISIBLE);
        buttonStop.setVisibility(View.VISIBLE);
        progress.setVisibility(View.VISIBLE);
        buttonCollapse.setVisibility(View.INVISIBLE);
        buttonExpand.setVisibility(View.VISIBLE);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                ActionBar.LayoutParams.MATCH_PARENT,
                ActionBar.LayoutParams.MATCH_PARENT
        );

        params.addRule(RelativeLayout.BELOW, R.id.expand);
//        params.addRule(RelativeLayout., R.id.musicAct);
        musicListView.setLayoutParams(params);
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                // resume playback
                if (pauseHumanTriggered == false) {
                    Log.d("AudioManager", "Untriggered");
                    if (mPlayer == null) {
                        Log.d("mPlayer", "works");
                        setDataSource(current_song_index);
                    } else if (!mPlayer.isPlaying()) {
                        mPlayer.start();
                    }
                    mPlayer.setVolume(1.0f, 1.0f);
                }
                break;

            case AudioManager.AUDIOFOCUS_LOSS:
                // Lost focus for an unbounded amount of time: stop playback and release media player
                if (mPlayer.isPlaying()) mPlayer.stop();
                mPlayer.release();
                mPlayer = null;
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                // Lost focus for a short time, but we have to stop
                // playback. We don't release the media player because playback
                // is likely to resume
                if (mPlayer.isPlaying()) mPlayer.pause();
                Log.d("AudioManager", "Triggered");
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                // Lost focus for a short time, but it's ok to keep playing
                // at an attenuated level
                if (mPlayer.isPlaying()) mPlayer.setVolume(0.1f, 0.1f);
                Log.d("AudioManager", "Triggered");
                break;
        }

    }

    private class MediaObserver implements Runnable {
        private AtomicBoolean stop = new AtomicBoolean(false);

        public void stop() {
            stop.set(true);
        }

        @Override
        public void run() {
            while (!stop.get() && mPlayer != null) {
                progress.setProgress(mPlayer.getCurrentPosition());

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    ///////starts media player/////////
    public void playMusic() {
        mPlayer.start();
        observer = new MediaObserver();
        new Thread(observer).start();
        registerReceiver(myAudioReceiver, intentFilter);
        progress.setMax(mPlayer.getDuration());
        final_second = mPlayer.getDuration() / 1000;
        final_hour = final_second / 3600;
        final_minute = (final_second % 3600) / 60;
        final_second = final_second % 60;
        songTV.setText(music_files[current_song_index].getName());


        progress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progressChanged = 0;


            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progressChanged = progress;
                current_second = progress / 1000;
                current_hour = current_second / 3600;
                current_minute = (current_second % 3600) / 60;
                current_second = current_second % 60;

                dur.setText(current_hour + ":" + (current_minute < 10 ? "0" : "") + current_minute + ":" + (current_second < 10 ? "0" : "") + current_second
                        + "/" + final_hour + ":" + (final_minute < 10 ? "0" : "") + final_minute + ":" + (final_second < 10 ? "0" : "") + final_second);

                if (fromUser) {
                    mPlayer.seekTo(progressChanged);
                }
            }

            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            public void onCompletion(MediaPlayer mp) {
                mp.reset();
                observer.stop();
                setDataSource(++current_song_index % number_of_songs);
            }
        });
    }

    private class AudioReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                mPlayer.pause();
            }
        }
    }

    private IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);

    private void nextSong()
    {
        if (mPlayer != null) {
            if (mPlayer.isPlaying() == false) {
                buttonPlay.setBackgroundResource(android.R.drawable.ic_media_pause);
            }
            mPlayer.reset();
            observer.stop();
            musicListView.setSelection(current_song_index + 1);
            setDataSource(++current_song_index % number_of_songs);
        }
    }

    private void prevSong()
    {
        if (mPlayer != null) {
            if (mPlayer.isPlaying() == false) {
                buttonPlay.setBackgroundResource(android.R.drawable.ic_media_pause);
            }
            mPlayer.reset();
            observer.stop();
            if(current_song_index <= 0)
            {
                current_song_index = number_of_songs - 1;
                musicListView.setSelection(current_song_index);
                setDataSource(current_song_index % number_of_songs);
            }
            else {
                musicListView.setSelection(current_song_index - 1);
                setDataSource(--current_song_index % number_of_songs);
            }
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mPlayer != null)
        {
            mPlayer.stop();
            mPlayer.release();
        }
        if (observer != null) observer.stop();
        if (audioManager != null) audioManager.abandonAudioFocus(this);
        unregisterReceiver(myAudioReceiver);
        myRetriever.release();
    }
}














































