package com.cryotech.musicplayer;

import android.content.ContentUris;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class PlayerFragment extends Fragment implements SeekBar.OnSeekBarChangeListener{

    private View rootView;
    private HomeActivity home;
    private SeekBar songProgressBar;
    private String songTitle;
    private String songArtist;
    private String songDuration;
    public TextView mSongTitle;
    public TextView mSongArtist;
    private TextView mSongCurrentDur;
    private TextView mSongTotalDur;
    private ImageView bgImage;
    private ImageButton btnRepeat;
    private ImageButton btnShuffle;
    private ImageButton btnPrevious;
    private ImageButton btnRewind;
    public ImageButton btnPlay;
    private ImageButton btnFastForward;
    private ImageButton btnNext;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        songTitle = getArguments().getString("Title");
        songArtist = getArguments().getString("Artist");

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_player, container, false);
        return rootView;

    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        songProgressBar  = rootView.findViewById(R.id.songProgressBar);
        mSongTitle = rootView.findViewById(R.id.songTitle);
        mSongArtist = rootView.findViewById(R.id.songArtist);
        mSongCurrentDur = rootView.findViewById(R.id.songCurrentDurationLabel);
        mSongTotalDur = rootView.findViewById(R.id.songTotalDurationLabel);
        bgImage = rootView.findViewById(R.id.imageView);
        btnRepeat = rootView.findViewById(R.id.btnRepeat);
        btnShuffle = rootView.findViewById(R.id.btnShuffle);
        btnPrevious = rootView.findViewById(R.id.btnPrevious);
        btnRewind = rootView.findViewById(R.id.btnBackward);
        btnPlay = rootView.findViewById(R.id.btnPlay);
        btnFastForward = rootView.findViewById(R.id.btnForward);
        btnNext = rootView.findViewById(R.id.btnNext);
        home = (HomeActivity)getActivity();

        initTitleLabels();
        initPlayerState();
        initMediaControls();

    }

    private void initTitleLabels()
    {
        if (songTitle != null && songArtist != null)
        {
            mSongTitle.setText(songTitle);
            mSongArtist.setText(songArtist);
        }
    }

    private void initPlayerState()
    {
        if (home.musicSrv.mediaPlayer.isPlaying())
        {
            btnPlay.setImageResource(android.R.drawable.ic_media_pause);
        }
        else
        {
            btnPlay.setImageResource(android.R.drawable.ic_media_play);
        }

        if (home.musicSrv.getRepeatState())
        {
            btnRepeat.setImageResource(R.drawable.ic_repeat_f);
            btnShuffle.setImageResource(R.drawable.ic_shuffle_d);
        }
        else if (home.musicSrv.getShuffleState())
        {
            btnShuffle.setImageResource(R.drawable.ic_shuffle_f);
            btnRepeat.setImageResource(R.drawable.ic_repeat_d);
        }
        else
        {
            btnRepeat.setImageResource(R.drawable.ic_repeat_d);
            btnShuffle.setImageResource(R.drawable.ic_shuffle_d);
        }

        songProgressBar.setOnSeekBarChangeListener(this);
        songProgressBar.setProgress(0);
        songProgressBar.setMax(100);

        updateProgressBar();
    }

    private void initMediaControls()
    {
        btnRepeat.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                if (!home.musicSrv.getRepeatState())
                {
                    home.musicSrv.setRepeat(true);
                    home.musicSrv.setShuffle(false);
                    btnRepeat.setImageResource(R.drawable.ic_repeat_f);
                    btnShuffle.setImageResource(R.drawable.ic_shuffle_d);
                    Toast.makeText(getActivity(), "Repeat ON", Toast.LENGTH_SHORT).show();
                }
                else
                {
                    home.musicSrv.setRepeat(false);
                    btnRepeat.setImageResource(R.drawable.ic_repeat_d);
                }

            }
        });
        btnShuffle.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                if (!home.musicSrv.getShuffleState())
                {
                    home.musicSrv.setShuffle(true);
                    home.musicSrv.setRepeat(false);
                    btnShuffle.setImageResource(R.drawable.ic_shuffle_f);
                    btnRepeat.setImageResource(R.drawable.ic_repeat_d);
                    Toast.makeText(getActivity(), "Shuffle ON", Toast.LENGTH_SHORT).show();
                }
                else
                {
                    home.musicSrv.setShuffle(false);
                    btnShuffle.setImageResource(R.drawable.ic_shuffle_d);
                }

            }
        });
        btnPrevious.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                home.musicSrv.playPrevious();
                songTitle = home.musicSrv.getCurrentSongTitle();
                songArtist = home.musicSrv.getCurrentSongArtist();
                mSongTitle.setText(songTitle);
                mSongArtist.setText(songArtist);
                home.musicSrv.sendResult("SONG_COMPLETE");
            }
        });

        btnRewind.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                home.musicSrv.rewindSong();
            }
        });

        btnPlay.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                home.musicSrv.pauseSong();

                if (home.musicSrv.mediaPlayer.isPlaying())
                {
                    btnPlay.setImageResource(android.R.drawable.ic_media_pause);
                }
                else
                {
                    btnPlay.setImageResource(android.R.drawable.ic_media_play);
                }

            }
        });

        btnFastForward.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                home.musicSrv.fastforwardSong();
            }
        });

        btnNext.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                home.musicSrv.playNext();
                songTitle = home.musicSrv.getCurrentSongTitle();
                songArtist = home.musicSrv.getCurrentSongArtist();
                mSongTitle.setText(songTitle);
                mSongArtist.setText(songArtist);
                home.musicSrv.sendResult("SONG_COMPLETE");
            }
        });
    }


    private void updateProgressBar() {

        if (home.musicSrv.mediaPlayer.isPlaying())
        {
            home.mHandler.postDelayed(mUpdateTimeTask, 100);
        }
    }

    public Runnable mUpdateTimeTask = new Runnable() {
        public void run() {

            try
            {
                long totalDuration = home.musicSrv.getCurrentSongDuration();
                long currentDuration = home.musicSrv.mediaPlayer.getCurrentPosition();

                mSongTotalDur.setText(""+home.milliSecondsToTimer(totalDuration));
                mSongCurrentDur.setText(""+home.milliSecondsToTimer(currentDuration));

                int progress = (getProgressPercentage(currentDuration, totalDuration));
                songProgressBar.setProgress(progress);

                home.mHandler.postDelayed(this, 100);
            }
            catch (IllegalStateException e)
            {
                e.printStackTrace();
            }
        }
    };

    public Uri getAlbumArt(long album_id)
    {
        final Uri sArtworkUri = Uri
                .parse("content://media/external/audio/albumart");

        return ContentUris.withAppendedId(sArtworkUri, album_id);
    }

    @Override
    public void onPause() {
        super.onPause();

        home.mHandler.removeCallbacks(mUpdateTimeTask);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (home.selectedSongPos != -1)
        {
            home.mHandler.postDelayed(mUpdateTimeTask, 100);
        }

    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        home.mHandler.removeCallbacks(mUpdateTimeTask);
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

        home.mHandler.removeCallbacks(mUpdateTimeTask);
        int totalDuration = home.musicSrv.mediaPlayer.getDuration();
        int currentPosition = progressToTimer(seekBar.getProgress(), totalDuration);

        home.musicSrv.mediaPlayer.seekTo(currentPosition);

        updateProgressBar();
    }

    private int getProgressPercentage(long currentDuration, long totalDuration){
        Double percentage;

        long currentSeconds = (int) (currentDuration / 1000);
        long totalSeconds = (int) (totalDuration / 1000);

        percentage =(((double)currentSeconds)/totalSeconds)*100;

        return percentage.intValue();
    }

    private int progressToTimer(int progress, int totalDuration) {
        int currentDuration;
        totalDuration = totalDuration / 1000;
        currentDuration = (int) ((((double)progress) / 100) * totalDuration);

        return currentDuration * 1000;
    }
}
