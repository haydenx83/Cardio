package com.example.hcarothe.cardio;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.os.BatteryManager;
import android.os.Bundle;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;


public class MusicActivity extends AppCompatActivity implements AudioManager.OnAudioFocusChangeListener {

    private static final String TAG = MusicActivity.class.getSimpleName();
    private int current_hour, current_minute, current_second, final_hour, final_minute, final_second;
    private int number_of_songs, current_song_index, skipTime;
    private String timeDisplay;
    private String PATH_TO_FILE;
    private File[] music_files;
    private Boolean pauseHumanTriggered, focusLost;

    private Button buttonPlay, buttonStop, buttonSkip,buttonPrev,buttonExpand, buttonCollapse,buttonSettings;
    private Button buttonForwardTen, buttonForwardThirty, buttonBackTen, buttonBackThirty;
    private ImageView imageCover,imageForwardTen, imageForwardThirty;
    private TextView songTV, dur;
    private SeekBar progress;
    private RelativeLayout relativeLayout;
    private ListView musicListView;

    private MediaPlayer mPlayer;
    private MediaObserver observer = null;
    private AudioManager audioManager;
    private Intent intent;
    private AudioReceiver myAudioReceiver;
    private MediaMetadataRetriever myRetriever;
    private ArrayAdapter<String> musicAdapter;
    private Bitmap bitmap;
    private byte[] data;
    private PowerConnectionReceiver myPowerConnectionReceiver;
    private IntentFilter intentFilter;
    private IntentFilter batteryFilter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music);

        current_song_index = 0;
        intent = getIntent();
        PATH_TO_FILE = intent.getExtras().getString("PATH_TO_FILE");

        openMusicFolder();
        Log.d(TAG, "onCreate() Restoring previous state");

        setUp();
    }
    private void setUp()
    {
        pauseHumanTriggered = false;
        focusLost = false;
        setUpAudio();
        setUpSeekBar();
        setUpDisplay();
        setUpAudioFocus();
        setUpButtons();
        setUpReceivers();
    }
    private void setUpAudio()
    {
        mPlayer = new MediaPlayer();
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        myRetriever = new MediaMetadataRetriever();
    }
    private void setUpReceivers()
    {
        intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        batteryFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);

        myAudioReceiver = new AudioReceiver();
        myPowerConnectionReceiver = new PowerConnectionReceiver();
        registerReceiver(myAudioReceiver, intentFilter);
        registerReceiver(myPowerConnectionReceiver, batteryFilter);
    }
    private void setUpDisplay() {
        imageCover = (ImageView) findViewById(R.id.CoverArt);
        songTV = (TextView) findViewById(R.id.songName);
        dur = (TextView) findViewById(R.id.dur);
    }
    private void setUpSeekBar() {
        Drawable drawable = ResourcesCompat.getDrawable(getResources(),R.drawable.progressbar, null);
        progress = (SeekBar) findViewById(R.id.progressBar);
        assert progress != null;
        progress.setProgressDrawable(drawable);
    }
    private void setUpAudioFocus() {
        int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Log.d("AudioManager", "Worked");
            setDataSource(current_song_index % number_of_songs);
        }
    }

    private void setUpButtons() {
        buttonPlay = (Button) findViewById(R.id.play);
        buttonPlay.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (!mPlayer.isPlaying()) {
                    mPlayer.start();
                    pauseHumanTriggered = false;
                    buttonPlay.setBackgroundResource(android.R.drawable.ic_media_pause);

                } else {
                    mPlayer.pause();
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

        buttonSettings= (Button) findViewById(R.id.settings);

        buttonForwardTen= (Button) findViewById(R.id.forwardTen);
        buttonForwardTen.setOnClickListener(new OnClickListener() {

                                                public void onClick(View v) {
                                                    skipAhead(10000);
                                                }
                                            });
        buttonForwardThirty= (Button) findViewById(R.id.forwardThirty);
        buttonForwardThirty.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                skipAhead(120000);
            }
        });

        buttonBackTen= (Button) findViewById(R.id.prevTen);
        buttonBackTen.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                skipAhead(-10000);
            }
        });

        buttonBackThirty= (Button) findViewById(R.id.prevThirty);
        buttonBackThirty.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                skipAhead(-30000);
            }
        });

        imageForwardTen= (ImageView) findViewById(R.id.imageViewForwardTen);
        imageForwardThirty= (ImageView) findViewById(R.id.imageViewForwardThirty);

        relativeLayout = (RelativeLayout) findViewById(R.id.musicAct);
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
                //imageCover.setImageBitmap(bitmap);
                imageCover.setBackground(new BitmapDrawable(getResources(), bitmap));
                Log.d("Bitmap", "Worked");
            } else {
                bitmap = BitmapFactory.decodeResource(this.getResources(), R.drawable.cardio_background);
                //imageCover.setImageBitmap(bitmap);
                imageCover.setBackground(new BitmapDrawable(getResources(), bitmap));
                Log.d("Bitmap", "Didn't not work");
            }
            prepareMediaPlayer();
        } catch (IllegalArgumentException e) {
           Log.d("Exception","IllegalArgumentException");
        } catch (SecurityException e) {
            Log.d("Exception","SecurityException");
        } catch (IllegalStateException e) {
            Log.d("Exception","IllegalStateException");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void prepareMediaPlayer() {
        try {
            mPlayer.prepareAsync();
        } catch (IllegalStateException e) {
            Log.d("Exception","IllegalStateException");
        }
        mPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            public void onPrepared(MediaPlayer mp) {

                mPlayer.seekTo(0);
                playMusic();
            }
        });
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
        musicAdapter = new ArrayAdapter<>(this, R.layout.activity_musiclistview, music_names);

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
        buttonSettings.setVisibility(View.INVISIBLE);
        buttonExpand.setVisibility(View.INVISIBLE);
        buttonForwardThirty.setVisibility(View.INVISIBLE);
        buttonForwardTen.setVisibility(View.INVISIBLE);
        buttonBackTen.setVisibility(View.INVISIBLE);
        buttonBackThirty.setVisibility(View.INVISIBLE);
        imageForwardTen.setVisibility(View.INVISIBLE);
        imageForwardThirty.setVisibility(View.INVISIBLE);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                ActionBar.LayoutParams.MATCH_PARENT,
                ActionBar.LayoutParams.MATCH_PARENT
        );

        params.addRule(RelativeLayout.BELOW, R.id.collapse);
        musicListView.setLayoutParams(params);
        relativeLayout.setBackgroundColor(getResources().getColor(R.color.button_material_light));
    }

    private void collapseList()
    {
        buttonPlay.setVisibility(View.VISIBLE);
        buttonPrev.setVisibility(View.VISIBLE);
        buttonSkip.setVisibility(View.VISIBLE);
        buttonStop.setVisibility(View.VISIBLE);
        progress.setVisibility(View.VISIBLE);
        buttonCollapse.setVisibility(View.INVISIBLE);
        buttonSettings.setVisibility(View.VISIBLE);
        buttonExpand.setVisibility(View.VISIBLE);
        buttonForwardThirty.setVisibility(View.VISIBLE);
        buttonForwardTen.setVisibility(View.VISIBLE);
        buttonBackTen.setVisibility(View.VISIBLE);
        buttonBackThirty.setVisibility(View.VISIBLE);
        imageForwardTen.setVisibility(View.VISIBLE);
        imageForwardThirty.setVisibility(View.VISIBLE);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                ActionBar.LayoutParams.MATCH_PARENT,
                ActionBar.LayoutParams.MATCH_PARENT
        );

        params.addRule(RelativeLayout.BELOW, R.id.expand);
        musicListView.setLayoutParams(params);
        relativeLayout.setBackgroundColor(0xff33b5e5);
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                // resume playback
                if (!pauseHumanTriggered) {
                    Log.d("AudioManager", "trigger removed");
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
                if (mPlayer.isPlaying()) {
                    closeListeners();
                    focusLost = true;
                }
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
                // at an attenuated level\
                if( mPlayer != null) {
                    if (mPlayer.isPlaying()) {
                        mPlayer.setVolume(0.1f, 0.1f);
                        Log.d("AudioManager", "Triggered");
                    }
                }
                break;
        }

    }

    private class MediaObserver implements Runnable {
        private final AtomicBoolean stop = new AtomicBoolean(false);

        public void stop() {
            stop.set(true);
        }

        @Override
        public void run() {
            while (!stop.get()) {
                if(mPlayer != null) {
                    progress.setProgress(mPlayer.getCurrentPosition());
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    ///////starts media player/////////
    private void playMusic() {
        mPlayer.start();
        observer = new MediaObserver();
        new Thread(observer).start();
        progress.setMax(mPlayer.getDuration());
        final_second = mPlayer.getDuration() / 1000;
        final_hour = final_second / 3600;
        final_minute = (final_second % 3600) / 60;
        final_second = final_second % 60;
        songTV.setText(music_files[current_song_index].getName());

        Log.d("Duration: ", "" + mPlayer.getDuration());

        progress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progressChanged = 0;

            @SuppressLint("SetTextI18n")
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progressChanged = progress;

                current_second = (int) (progressChanged / 1000) % 60 ;
                current_minute = (int) ((progressChanged / (1000*60)) % 60);
                current_hour   = (int) ((progressChanged / (1000*60*60)) % 24);

                timeDisplay = current_hour + ":" + (current_minute < 10 ? "0" : "") + current_minute + ":" + (current_second < 10 ? "0" : "") + current_second
                        + "/" + final_hour + ":" + (final_minute < 10 ? "0" : "") + final_minute + ":" + (final_second < 10 ? "0" : "") + final_second;

                dur.setText(timeDisplay);

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
                nextSong();
            }
        });
    }

    private class AudioReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                if( mPlayer != null) {
                    if (mPlayer.isPlaying()) {
                        mPlayer.pause();
                        buttonPlay.setBackgroundResource(android.R.drawable.ic_media_play);
                        pauseHumanTriggered = true;
                    }
                }
            }
        }
    }

    public class PowerConnectionReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context , Intent intent) {
            int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            Log.d("Power","Receiver Runs");
            if(status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL) {
                Log.d("Power","Flag On");
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
            else {
                Log.d("Power","Flag Off");
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        }
    }

    private void nextSong()
    {
        if (mPlayer != null) {
            if (!mPlayer.isPlaying()) {
                buttonPlay.setBackgroundResource(android.R.drawable.ic_media_pause);
            }
            observer.stop();
            mPlayer.reset();

            if(current_song_index == number_of_songs -1)
            {
                current_song_index = 0;
            }
            else
            {
                current_song_index++;
            }
            musicListView.setSelection(current_song_index);
            setDataSource(current_song_index);
        }
    }

    private void prevSong()
    {
        if (mPlayer != null) {
            if (!mPlayer.isPlaying()) {
                buttonPlay.setBackgroundResource(android.R.drawable.ic_media_pause);
            }
            observer.stop();
            mPlayer.reset();

            if(current_song_index <= 0)
            {
                current_song_index = number_of_songs - 1;
            }
            else {
                current_song_index--;
            }
            musicListView.setSelection(current_song_index);
            setDataSource(current_song_index);
        }
    }
    private void skipAhead(int skip)
    {
        skipTime = 0;
        if(mPlayer != null) {
            skipTime = mPlayer.getCurrentPosition();
            Log.d("Skip Float",""+ (float)mPlayer.getCurrentPosition());
            Log.d("Skip Int", "" + (int)mPlayer.getCurrentPosition());
            if (skipTime + skip > mPlayer.getDuration()) {
                return;
            }
            else if(skipTime < 0)
            {
                mPlayer.seekTo(0);
            }
            else {
                mPlayer.seekTo(skipTime + skip);
            }
        }
    }

    public void onResume() {
        super .onResume();
        if(focusLost)
        {
          setUp();
        }
    }

    public void onDestroy() {
        super.onDestroy();
        closeListeners();
    }
    private void closeListeners()
    {
        if (observer != null) observer.stop();
        if (mPlayer != null)
        {
            mPlayer.stop();
            mPlayer.reset();
            mPlayer.release();
            mPlayer = null;
        }

        if (audioManager != null) audioManager.abandonAudioFocus(this);
        unregisterReceiver(myAudioReceiver);
        unregisterReceiver(myPowerConnectionReceiver);
        myRetriever.release();
        myRetriever = null;
        observer = null;
    }
}














































