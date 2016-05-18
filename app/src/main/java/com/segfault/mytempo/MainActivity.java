package com.segfault.mytempo;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;


public class MainActivity extends ActionBarActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private MediaPlayer mPlayer;
    Button buttonPlay, buttonStop, buttonSkip;
    ImageView imageCover;
    TextView songTV, dur;
    SeekBar progress;
    String PATH_TO_FILE = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).toString();
    private File[] music_files;
    private int number_of_songs;
    private int current_song_index;
    private MediaObserver observer = null;
    private int current_hour, current_minute, current_second, final_hour, final_minute, final_second;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        current_song_index = 0;
        music_files = openMusicFolder();
        number_of_songs = get_number_of_songs(music_files);
        Log.d(TAG, "onCreate() Restoring previous state");
        mPlayer = new MediaPlayer();

        progress = (SeekBar) findViewById(R.id.progressBar);

        imageCover = (ImageView) findViewById(R.id.CoverArt);
        songTV = (TextView) findViewById(R.id.songTextView);
        dur = (TextView) findViewById(R.id.dur);

        buttonPlay = (Button) findViewById(R.id.play);
        buttonPlay.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (mPlayer.isPlaying() == false) {
                    mPlayer.start();
                    buttonPlay.setBackgroundResource(android.R.drawable.ic_media_pause);

                } else {
                    mPlayer.pause();
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

                if (mPlayer.isPlaying() == false) {
                    mPlayer.start();
                    buttonPlay.setBackgroundResource(android.R.drawable.ic_media_pause);

                }
                setDataSource(++current_song_index%number_of_songs);
            }
        });
        setDataSource(current_song_index);
    }
    private void setDataSource(int song_index) {
        mPlayer.reset();
        try {
            Log.d("Files",PATH_TO_FILE + '/' + music_files[song_index].getName());
            mPlayer.setDataSource(PATH_TO_FILE + '/' + music_files[song_index].getName());
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
    private void prepareMediaPlayer()
    {
        try {
            mPlayer.prepare();
            playMusic();
        } catch (IllegalStateException e) {
            //Toast.makeText(getApplicationContext(), "You might not set the URI correctly!", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            //Toast.makeText(getApplicationContext(), "You might not set the URI correctly!", Toast.LENGTH_LONG).show();
        }
    }
    private File[] openMusicFolder() {
        Log.d("Files", "Path: " + PATH_TO_FILE);
        File local_music_files[] = new File(PATH_TO_FILE).listFiles();
        Log.d("Files", "Size: "+ local_music_files.length);
        for (int i  = 0; i < local_music_files.length; i++)
        {
            Log.d("Files", "FileName:" + local_music_files[i].getName());
        }
        return local_music_files;
    }
    private int get_number_of_songs(File local_music_file[]){
        return local_music_file.length;
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
        progress.setMax(mPlayer.getDuration());
        final_second = mPlayer.getDuration() / 1000;
        final_hour = final_second / 3600;
        final_minute = (final_second % 3600) / 60;
        final_second = final_second % 60;

        progress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progressChanged = 0;


            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progressChanged = progress;
                current_second = progress / 1000;
                current_hour = current_second / 3600;
                current_minute = (current_second % 3600) / 60;
                current_second = current_second % 60;

                dur.setText(current_hour + ":" + (current_minute < 10 ? "0" : "") + current_minute + ":" + (current_second < 10 ? "0" : "") + current_second
                        + "/" + final_hour + ":" + (final_minute < 10 ? "0" : "")  + final_minute + ":" + (final_second < 10 ? "0" : "") + final_second);

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
}
