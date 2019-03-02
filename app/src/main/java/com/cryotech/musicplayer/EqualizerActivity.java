package com.cryotech.musicplayer;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.Point;
import android.media.audiofx.Equalizer;
import android.media.audiofx.PresetReverb;
import android.media.audiofx.Visualizer;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.view.Display;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.HorizontalScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class EqualizerActivity extends AppCompatActivity {

    private VisualizerView mVisualizerView;
    private Visualizer mVisualizer;
    private Equalizer mEqualizer;
    private PresetReverb mReverb;

    private HorizontalScrollView hsv;
    private SwitchCompat eqSwitch;
    final short band1 = 0;
    final short band2 = 1;
    final short band3 = 2;
    final short band4 = 3;
    final short band5 = 4;
    private SeekBar seekBar1;
    private SeekBar seekBar2;
    private SeekBar seekBar3;
    private SeekBar seekBar4;
    private SeekBar seekBar5;
    private TextView freq1title;
    private TextView freq2title;
    private TextView freq3title;
    private TextView freq4title;
    private TextView freq5title;
    private Button reverb1;
    private Button reverb2;
    private Button reverb3;
    private Button reverb4;
    private Button reverb5;
    private Button reverb6;
    private Button reverb7;

    private MusicService musicSrv;
    private Intent playIntent;
    private boolean musicBound = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_equalizer);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle("Equalizer");

    }

    @Override
    protected void onStart() {
        super.onStart();
        if(playIntent==null){
            playIntent = new Intent(this, MusicService.class);
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
            startService(playIntent);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    private void initControls()
    {

        if (mEqualizer == null) {
            initVisualizerFxAndUI();
            initEqualizer();
            initReverb();
            initEnabled();
            setDefaultFreq();
        }

        eqSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                musicSrv.setEqEnabled(isChecked);
                mVisualizer.setEnabled(isChecked);
                mEqualizer.setEnabled(isChecked);
                mReverb.setEnabled(isChecked);

                if (mEqualizer.getEnabled())
                {
                    Toast.makeText(getApplicationContext(), "Equalizer enabled", Toast.LENGTH_SHORT).show();
                }
                else
                {
                    Toast.makeText(getApplicationContext(), "Equalizer disabled", Toast.LENGTH_SHORT).show();
                }

            }
        });


    }

    private void initVisualizerFxAndUI() {

        mVisualizerView = findViewById(R.id.visualizer);
        mVisualizer = new Visualizer(musicSrv.mediaPlayer.getAudioSessionId());
        mVisualizer.setEnabled(false);
        mVisualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);
        mVisualizer.setDataCaptureListener(
                new Visualizer.OnDataCaptureListener() {
                    public void onWaveFormDataCapture(Visualizer visualizer,
                                                      byte[] bytes, int samplingRate) {
                        mVisualizerView.updateVisualizer(bytes);
                    }

                    public void onFftDataCapture(Visualizer visualizer,
                                                 byte[] bytes, int samplingRate) {
                    }
                }, Visualizer.getMaxCaptureRate() / 4, true, false);
    }

    private void initEqualizer()
    {
        eqSwitch = findViewById(R.id.eqSwitch);

        freq1title = findViewById(R.id.eq_freq_title1);
        freq2title = findViewById(R.id.eq_freq_title2);
        freq3title = findViewById(R.id.eq_freq_title3);
        freq4title = findViewById(R.id.eq_freq_title4);
        freq5title = findViewById(R.id.eq_freq_title5);

        seekBar1 = findViewById(R.id.band_1);
        seekBar2 = findViewById(R.id.band_2);
        seekBar3 = findViewById(R.id.band_3);
        seekBar4 = findViewById(R.id.band_4);
        seekBar5 = findViewById(R.id.band_5);

        reverb1 = findViewById(R.id.reverb_btn_none);
        reverb2 = findViewById(R.id.reverb_btn_sr);
        reverb3 = findViewById(R.id.reverb_btn_mr);
        reverb4 = findViewById(R.id.reverb_btn_lr);
        reverb5 = findViewById(R.id.reverb_btn_mh);
        reverb6 = findViewById(R.id.reverb_btn_lh);
        reverb7 = findViewById(R.id.reverb_btn_plate);

        mEqualizer = new Equalizer(1000, musicSrv.mediaPlayer.getAudioSessionId());
        mReverb = new PresetReverb(1000, musicSrv.mediaPlayer.getAudioSessionId());

        final short minEQLevel = mEqualizer.getBandLevelRange()[0];
        final short maxEQLevel = mEqualizer.getBandLevelRange()[1];

        freq1title.setText((mEqualizer.getCenterFreq(band1) / 1000) + " Hz");
        freq2title.setText((mEqualizer.getCenterFreq(band2) / 1000) + " Hz");
        freq3title.setText((mEqualizer.getCenterFreq(band3) / 1000) + " Hz");
        freq4title.setText((mEqualizer.getCenterFreq(band4) / 1000) + " Hz");
        freq5title.setText((mEqualizer.getCenterFreq(band5) / 1000) + " Hz");

        seekBar1.setMax(maxEQLevel - minEQLevel);
        seekBar1.setProgress(mEqualizer.getBandLevel(band1));

        seekBar1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                mEqualizer.setBandLevel(band1, (short) (progress + minEQLevel));
            }

            public void onStartTrackingTouch(SeekBar seekBar) {}
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        seekBar2.setMax(maxEQLevel - minEQLevel);
        seekBar2.setProgress(mEqualizer.getBandLevel(band2));

        seekBar2.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                mEqualizer.setBandLevel(band2, (short) (progress + minEQLevel));
            }

            public void onStartTrackingTouch(SeekBar seekBar) {}
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        seekBar3.setMax(maxEQLevel - minEQLevel);
        seekBar3.setProgress(mEqualizer.getBandLevel(band3));

        seekBar3.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                mEqualizer.setBandLevel(band3, (short) (progress + minEQLevel));
            }

            public void onStartTrackingTouch(SeekBar seekBar) {}
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        seekBar4.setMax(maxEQLevel - minEQLevel);
        seekBar4.setProgress(mEqualizer.getBandLevel(band4));

        seekBar4.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                mEqualizer.setBandLevel(band4, (short) (progress + minEQLevel));

            }

            public void onStartTrackingTouch(SeekBar seekBar) {}
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        seekBar5.setMax(maxEQLevel - minEQLevel);
        seekBar5.setProgress(mEqualizer.getBandLevel(band5));

        seekBar5.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                mEqualizer.setBandLevel(band5, (short) (progress + minEQLevel));
            }

            public void onStartTrackingTouch(SeekBar seekBar) {}
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

    }

    private void initReverb()
    {
        hsv = (HorizontalScrollView) findViewById(R.id.hScrollView);

        reverb1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                unhighlightButtons();
                highlightButton(reverb1);
                int scrollX = (reverb1.getLeft() - (getScreenWidth() / 2)) + (reverb1.getWidth() / 2);
                hsv.smoothScrollTo(scrollX, 0);
                mReverb.setPreset((PresetReverb.PRESET_NONE));
            }
        });

        reverb2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                unhighlightButtons();
                highlightButton(reverb2);
                int scrollX = (reverb2.getLeft() - (getScreenWidth() / 2)) + (reverb2.getWidth() / 2);
                hsv.smoothScrollTo(scrollX, 0);
                mReverb.setPreset((PresetReverb.PRESET_SMALLROOM));
            }
        });

        reverb3.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                unhighlightButtons();
                highlightButton(reverb3);
                int scrollX = (reverb3.getLeft() - (getScreenWidth() / 2)) + (reverb3.getWidth() / 2);
                hsv.smoothScrollTo(scrollX, 0);
                mReverb.setPreset((PresetReverb.PRESET_MEDIUMROOM));
            }
        });

        reverb4.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                unhighlightButtons();
                highlightButton(reverb4);
                int scrollX = (reverb4.getLeft() - (getScreenWidth() / 2)) + (reverb4.getWidth() / 2);
                hsv.smoothScrollTo(scrollX, 0);
                mReverb.setPreset((PresetReverb.PRESET_LARGEROOM));
            }
        });

        reverb5.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                unhighlightButtons();
                highlightButton(reverb5);
                int scrollX = (reverb5.getLeft() - (getScreenWidth() / 2)) + (reverb5.getWidth() / 2);
                hsv.smoothScrollTo(scrollX, 0);
                mReverb.setPreset((PresetReverb.PRESET_MEDIUMHALL));
            }
        });

        reverb6.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                unhighlightButtons();
                highlightButton(reverb6);
                int scrollX = (reverb6.getLeft() - (getScreenWidth() / 2)) + (reverb6.getWidth() / 2);
                hsv.smoothScrollTo(scrollX, 0);
                mReverb.setPreset((PresetReverb.PRESET_LARGEHALL));
            }
        });

        reverb7.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                unhighlightButtons();
                highlightButton(reverb7);
                int scrollX = (reverb7.getLeft() - (getScreenWidth() / 2)) + (reverb7.getWidth() / 2);
                hsv.smoothScrollTo(scrollX, 0);
                mReverb.setPreset((PresetReverb.PRESET_PLATE));
            }
        });

    }

    private void initEnabled()
    {
        eqSwitch.setChecked(musicSrv.getEqEnabled());
        mVisualizer.setEnabled(musicSrv.getEqEnabled());
        mEqualizer.setEnabled(musicSrv.getEqEnabled());
        mReverb.setEnabled(musicSrv.getEqEnabled());
    }
    private void setDefaultFreq()
    {
        mEqualizer.usePreset((short) 0);
        final short lowerEqualizerBandLevel = mEqualizer.getBandLevelRange()[0];

        seekBar1.setProgress(mEqualizer.getBandLevel(band1) - lowerEqualizerBandLevel);
        seekBar2.setProgress(mEqualizer.getBandLevel(band2) - lowerEqualizerBandLevel);
        seekBar3.setProgress(mEqualizer.getBandLevel(band3) - lowerEqualizerBandLevel);
        seekBar4.setProgress(mEqualizer.getBandLevel(band4) - lowerEqualizerBandLevel);
        seekBar5.setProgress(mEqualizer.getBandLevel(band5) - lowerEqualizerBandLevel);

    }


    private void highlightButton(Button button)
    {
        button.setBackgroundColor(Color.rgb(34, 34, 34));
    }

    private void unhighlightButtons()
    {
        reverb1.setBackgroundColor(Color.rgb(17, 17, 17));
        reverb2.setBackgroundColor(Color.rgb(17, 17, 17));
        reverb3.setBackgroundColor(Color.rgb(17, 17, 17));
        reverb4.setBackgroundColor(Color.rgb(17, 17, 17));
        reverb5.setBackgroundColor(Color.rgb(17, 17, 17));
        reverb6.setBackgroundColor(Color.rgb(17, 17, 17));
        reverb7.setBackgroundColor(Color.rgb(17, 17, 17));
    }

    private int getScreenWidth()
    {
        Display display = getWindowManager().getDefaultDisplay();

        Point sizeX = new Point();
        display.getSize(sizeX);

        return sizeX.x;
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (musicConnection != null) {
            unbindService(musicConnection);
        }
    }

    private ServiceConnection musicConnection = new ServiceConnection(){

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder)service;
            musicSrv = binder.getService();
            initControls();
            musicBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicBound = false;
        }
    };

}
