package com.cryotech.musicplayer;

import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.design.widget.AppBarLayout;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

public class HomeActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, AllSongsHorizontalAdapter.RecyclerViewClickListener,
        ArtistsHorizontalAdapter.RecyclerViewClickListener, AlbumsHorizontalAdapter.RecyclerViewClickListener {

    private ArrayList<LocalTrack> homeAllSongsList;
    private ArrayList<LocalTrack> homeArtistsList;
    private ArrayList<LocalTrack> homeAlbumsList;

    private AllSongsHorizontalAdapter allSongHomeAdt;
    private ArtistsHorizontalAdapter artistHomeAdt;
    private AlbumsHorizontalAdapter albumHomeAdt;

    private NavigationView navigationView;
    private DrawerLayout drawer;
    private AppBarLayout appBarLayout;
    private NestedScrollView scrollView;
    private RecyclerView allSongsRecyclerView;
    private RecyclerView artistsRecyclerView;
    private RecyclerView albumsRecyclerView;

    private TextView trackTitleBar;
    private TextView trackArtistBar;
    private TextView trackDurBar;
    private ImageView trackImage;

    private PlayerFragment playerFragment;
    private BroadcastReceiver srvReceiver;
    private HeadphoneReceiver headphoneReciever;
    private SensorManager mSensorManager;
    public MusicService musicSrv;
    private Intent playIntent;
    private boolean musicBound = false;
    private boolean sharedApp = false;
    public int selectedSongPos = -1;

    public Handler mHandler = new Handler();

    //AndroidSDK\platform-tools\adb connect 192.168.1.10:5555
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        toggle.syncState();

        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.getMenu().getItem(0).setChecked(true);

        initSharedPrefs();

        srvReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String s = intent.getStringExtra(MusicService.MSG);

                if (s.equals("SONG_COMPLETE"))
                {
                    if (playerFragment != null)
                    {
                        if (playerFragment.isVisible())
                        {
                            playerFragment.mSongTitle.setText(musicSrv.getCurrentSongTitle());
                            playerFragment.mSongArtist.setText(musicSrv.getCurrentSongArtist());
                            playerFragment.btnPlay.setImageResource(android.R.drawable.ic_media_pause);
                        }
                    }

                    initTrackBar();
                }
                else if (s.equals("NOTIF_STATE_CHANGE"))
                {
                    if (playerFragment != null)
                    {
                        if (playerFragment.isVisible())
                        {
                            if (musicSrv.mediaPlayer != null)
                            {
                                if (musicSrv.mediaPlayer.isPlaying())
                                {
                                    playerFragment.btnPlay.setImageResource(android.R.drawable.ic_media_pause);
                                }
                                else
                                {
                                    playerFragment.btnPlay.setImageResource(android.R.drawable.ic_media_play);
                                }
                            }
                        }
                    }
                }

            }
        };

        headphoneReciever = new HeadphoneReceiver();
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

    }

    @Override
    protected void onStart() {
        super.onStart();

        navigationView.getMenu().getItem(0).setChecked(true);

        if(playIntent==null){

            playIntent = new Intent(this, MusicService.class);
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
            startService(playIntent);
        }

        if (musicSrv != null)
        {
            initTrackBar();
        }

        LocalBroadcastManager.getInstance(this).registerReceiver((srvReceiver),
                new IntentFilter(MusicService.RESULT)
        );

        IntentFilter filter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        registerReceiver(headphoneReciever, filter);

    }

    @Override
    protected void onStop() {
        super.onStop();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(srvReceiver);
        unregisterReceiver(headphoneReciever);

    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_settings) {
            startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(final MenuItem item) {

        final int id = item.getItemId();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                if (id == R.id.nav_home) {
                    drawer.closeDrawer(GravityCompat.START);

                } else if (id == R.id.nav_playlists) {
                    startActivity(new Intent(getApplicationContext(), PlaylistActivity.class));

                } else if (id == R.id.nav_equalizer) {
                    startActivity(new Intent(getApplicationContext(), EqualizerActivity.class));

                } else if (id == R.id.nav_recent) {
                    startActivity(new Intent(getApplicationContext(), RecentsActivity.class));

                } else if (id == R.id.nav_search) {
                    startActivity(new Intent(getApplicationContext(), SearchActivity.class));

                } else if (id == R.id.nav_settings) {
                    startActivity(new Intent(getApplicationContext(), SettingsActivity.class));

                } else if (id == R.id.nav_feedback) {
                    createFeedback();

                } else if (id == R.id.nav_share) {
                    Intent sendIntent = new Intent();
                    sendIntent.setAction(Intent.ACTION_SEND);
                    sendIntent.putExtra(Intent.EXTRA_TEXT, "Link to Google Play.");
                    sendIntent.setType("text/plain");
                    startActivity(sendIntent);

                    sharedApp = true;

                }
            }
        }, 250);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onPause() {
        super.onPause();
        mHandler.removeCallbacks(mUpdateDurationUI);
    }

    @Override
    public void onResume() {
        super.onResume();

        initHomeScroll();
        initHomeAllSongs(5);
        initHomeArtists(5);
        initHomeAlbums(5);

        if (selectedSongPos != -1)
        {
            mHandler.postDelayed(mUpdateDurationUI, 100);
        }

        if (sharedApp)
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this, R.style.MyAlertDialogStyle);
            builder.setCancelable(false);
            new android.app.AlertDialog.Builder(this, R.style.MyAlertDialogStyle)
                    .setTitle("Cryotech Music")
                    .setMessage("Thanks for your support! Leave us a rating and comment in the Play Store!")
                    .setPositiveButton("Okay", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            Uri uri = Uri.parse("market://details?id=" + getPackageName());
                            Intent myAppLinkToMarket = new Intent(Intent.ACTION_VIEW, uri);
                            try {
                                startActivity(myAppLinkToMarket);
                            } catch (ActivityNotFoundException e) {
                                Toast.makeText(getApplicationContext(), "Application not found", Toast.LENGTH_SHORT).show();
                            }
                        }

                    })
                    .setNegativeButton("Later", null)
                    .create().show();

            sharedApp = false;
        }

        if (musicSrv != null) mSensorManager.registerListener(musicSrv.mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);

    }

    public void initSharedPrefs()
    {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        Preferences.HEADPHONE_STATE = preferences.getBoolean("headphoneState", true);
        Preferences.DISPLAY_ALBUM_ART = preferences.getBoolean("albumArtDisplay", true);
        Preferences.FADE_EFFECT = preferences.getBoolean("fadeEffect", false);
        Preferences.NOTIF_DISPLAY = preferences.getBoolean("notifDisplay", true);
        Preferences.SHAKE_SKIP = preferences.getBoolean("shakeSkip", false);
        Preferences.DUPLICATE_MEMBERS = preferences.getBoolean("duplicateMembers", false);
    }

    public void initTrackBar()
    {
        selectedSongPos = musicSrv.getCurrentSongIndex();

        if (trackTitleBar == null)
        {
            trackTitleBar = findViewById(R.id.selected_track_title);
            trackArtistBar = findViewById(R.id.selected_track_artist);
            trackDurBar = findViewById(R.id.selected_track_duration);
            trackImage = findViewById(R.id.imb);
            Glide.with(this).load(R.drawable.ic_music).into(trackImage);
        }

        if (selectedSongPos != -1)
        {
            trackTitleBar.setText(musicSrv.getCurrentSongTitle());
            trackArtistBar.setText(musicSrv.getCurrentSongArtist());
            updateDuration();
        }
        else
        {
            trackTitleBar.setText("Cryotech Music");
            trackArtistBar.setText("Premium Music Player");
            trackDurBar.setText("");
        }

    }

    private void initHomeScroll()
    {
        appBarLayout = findViewById(R.id.appbar);
        scrollView = findViewById(R.id.nested_scroller);

        if (scrollView != null) {

            scrollView.setOnScrollChangeListener(new NestedScrollView.OnScrollChangeListener() {
                @Override
                public void onScrollChange(NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {

                    if (scrollY == 0) {
                        appBarLayout.setExpanded(true);
                    }

                    if (scrollY == (v.getChildAt(0).getMeasuredHeight() - v.getMeasuredHeight())) {
                        appBarLayout.setExpanded(false);
                    }
                }
            });
        }
    }

    private void initHomeAllSongs(int numDisplay)
    {

        homeAllSongsList = new ArrayList<>();
        allSongsRecyclerView = findViewById(R.id.all_songs_home);
        allSongHomeAdt = new AllSongsHorizontalAdapter(homeAllSongsList, this, this);
        allSongsRecyclerView.setLayoutManager(new LinearLayoutManager(HomeActivity.this, LinearLayoutManager.HORIZONTAL, false));
        allSongsRecyclerView.setAdapter(allSongHomeAdt);

        String duration = "";
        ContentResolver musicResolver = getContentResolver();
        String selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0";
        Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String sortOrder = android.provider.MediaStore.Audio.Media.TITLE + " ASC";
        Cursor musicCursor = musicResolver.query(musicUri, null, selection, null, sortOrder);

        if(musicCursor!=null && musicCursor.moveToFirst()){

            int idColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media._ID);
            int titleColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.TITLE);
            int artistColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.ARTIST);
            int albumColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.ALBUM);
            int pathColumn = musicCursor.getColumnIndex
                    (MediaStore.Audio.Media.DATA);
            int durColumn = musicCursor.getColumnIndex
                    (MediaStore.Audio.Media.DURATION);

            do {
                long thisId = musicCursor.getLong(idColumn);
                String thisTitle = musicCursor.getString(titleColumn);
                String thisArtist = musicCursor.getString(artistColumn);
                String thisAlbum = musicCursor.getString(albumColumn);
                String thisPath = musicCursor.getString(pathColumn);
                long thisDuration = musicCursor.getLong(durColumn);

                if (thisDuration > 10000) {
                    if (String.valueOf(thisDuration) != null) {
                        try {
                            Long time = Long.valueOf(thisDuration);
                            long seconds = time / 1000;
                            long minutes = seconds / 60;
                            seconds = seconds % 60;

                            if (seconds < 10) {
                                String csongs_duration = String.valueOf(minutes) + ":0" + String.valueOf(seconds);
                                duration = csongs_duration;
                            } else {
                                String ccsongs_duration = String.valueOf(minutes) + ":" + String.valueOf(seconds);
                                duration = ccsongs_duration;
                            }
                        } catch (NumberFormatException e) {
                            duration = String.valueOf(thisDuration);
                        }
                    } else {
                        duration = "null";
                    }
                }

                if (homeAllSongsList.size() < numDisplay)
                {
                    homeAllSongsList.add(new LocalTrack(thisId, thisTitle, thisArtist, thisAlbum, thisPath, duration, -1));
                }

            }
            while (musicCursor.moveToNext());
        }
        musicCursor.close();
    }

    private void initHomeArtists(int numDisplay)
    {
        homeArtistsList = new ArrayList<>();
        artistHomeAdt = new ArtistsHorizontalAdapter(homeArtistsList, this, this);
        artistsRecyclerView = findViewById(R.id.artist_home);
        artistsRecyclerView.setLayoutManager(new LinearLayoutManager(HomeActivity.this, LinearLayoutManager.HORIZONTAL, false));
        artistsRecyclerView.setAdapter(artistHomeAdt);

        ContentResolver musicResolver = getContentResolver();
        Uri musicUri = android.provider.MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI;
        String sortOrder = MediaStore.Audio.Media.ARTIST + " ASC";
        Cursor musicCursor = musicResolver.query(musicUri, null, null, null, sortOrder);

        if(musicCursor!=null && musicCursor.moveToFirst()){
            int idColumn = musicCursor.getColumnIndex
                    (MediaStore.Audio.Artists._ID);
            int artistColumn = musicCursor.getColumnIndex
                    (MediaStore.Audio.Media.ARTIST);
            int countColumn = musicCursor.getColumnIndex
                    (MediaStore.Audio.Artists.NUMBER_OF_TRACKS);
            do {
                long thisId = musicCursor.getLong(idColumn);
                String thisArtist = musicCursor.getString(artistColumn);
                int thisCount = musicCursor.getInt(countColumn);

                if (homeArtistsList.size() < numDisplay)
                {
                    homeArtistsList.add(new LocalTrack(thisId, null, thisArtist, null, null, null, thisCount));
                }

            }
            while (musicCursor.moveToNext());
        }
        musicCursor.close();

    }

    private void initHomeAlbums(int numDisplay)
    {
        homeAlbumsList = new ArrayList<>();
        albumHomeAdt = new AlbumsHorizontalAdapter(homeAlbumsList, this, this);
        albumsRecyclerView = findViewById(R.id.albums_home);
        albumsRecyclerView.setLayoutManager(new LinearLayoutManager(HomeActivity.this, LinearLayoutManager.HORIZONTAL, false));
        albumsRecyclerView.setAdapter(albumHomeAdt);

        ContentResolver musicResolver = getContentResolver();
        Uri musicUri = android.provider.MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI;
        String sortOrder = MediaStore.Audio.Media.ALBUM + " ASC";
        Cursor musicCursor = musicResolver.query(musicUri, null, null, null, sortOrder);

        if(musicCursor!=null && musicCursor.moveToFirst()){

            int idColumn = musicCursor.getColumnIndex
                    (MediaStore.Audio.Albums._ID);
            int albumColumn = musicCursor.getColumnIndex
                    (MediaStore.Audio.Albums.ALBUM);
            int countColumn = musicCursor.getColumnIndex
                    (MediaStore.Audio.Albums.NUMBER_OF_SONGS);

            do {
                long thisId = musicCursor.getLong(idColumn);
                String thisAlbum = musicCursor.getString(albumColumn);
                int thisCount = musicCursor.getInt(countColumn);

                if (homeAlbumsList.size() < numDisplay)
                {
                    homeAlbumsList.add(new LocalTrack(thisId, null, null, thisAlbum, null, null, thisCount));
                }
            }
            while (musicCursor.moveToNext());
        }
        musicCursor.close();

        for (int i = 0; i < homeAlbumsList.size() - 1; i++)
        {
            if (homeAlbumsList.get(i).getAlbum().equals(homeAlbumsList.get(i + 1).getAlbum()))
            {
                homeAlbumsList.remove(i + 1);
                i--;
            }
        }
    }

    private void createFeedback()
    {
        final AlertDialog.Builder builderInner = new AlertDialog.Builder(this, R.style.MyAlertDialogStyle);
        final AlertDialog alert = builderInner.create();

        LayoutInflater li = LayoutInflater.from(this);
        View renameView = li.inflate(R.layout.feedback_dialog, null);

        final EditText feedbackInput = renameView.findViewById(R.id.feedbackInput);
        final Button btnSend = renameView.findViewById(R.id.btnSend);
        final Button btnCancel = renameView.findViewById(R.id.btnCancel);

        feedbackInput.getBackground().setColorFilter(Color.parseColor("#222222"), PorterDuff.Mode.SRC_ATOP);
        feedbackInput.setSelection(feedbackInput.getText().length());

        btnSend.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                String feedbackMessage;

                feedbackMessage = feedbackInput.getText().toString();

                Intent intent = new Intent(Intent.ACTION_SENDTO);
                intent.setType("text/plain");
                intent.setData(Uri.parse("mailto:temp.business@gmail.com"));
                intent.putExtra(Intent.EXTRA_SUBJECT, "User Feedback Report");
                intent.putExtra(Intent.EXTRA_TEXT, feedbackMessage);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                try {
                    startActivity(intent);
                } catch (android.content.ActivityNotFoundException ex) {
                    Toast.makeText(HomeActivity.this, "There are no email clients installed", Toast.LENGTH_SHORT).show();
                }


            }

        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                alert.dismiss();

            }
        });

        alert.setView(renameView);
        alert.show();
    }

    @Override
    public void allSongItemClicked(int position)
    {
        Preferences.PLAYLIST = false;
        musicSrv.setList(homeAllSongsList);
        String title = homeAllSongsList.get(position).getTitle();
        String artist = homeAllSongsList.get(position).getArtist();
        musicSrv.setSong(title, artist);
        musicSrv.setSongIndex(position);
        musicSrv.playSong();
        initTrackBar();
    }

    @Override
    public void artistItemClicked(String artist) {

        Intent newIntent = new Intent(HomeActivity.this, ArtistSongsActivity.class);
        newIntent.putExtra("Artist", artist);
        HomeActivity.this.startActivity(newIntent);
    }

    @Override
    public void albumItemClicked(String album) {

        Intent newIntent = new Intent(HomeActivity.this, AlbumSongsActivity.class);
        newIntent.putExtra("Album", album);
        HomeActivity.this.startActivity(newIntent);
    }

    public void onClickPlayer(View v)
    {
        if (selectedSongPos != -1)
        {
            hideFragment("player");
            showFragment("player");
        }
    }
    public void onClickLibrary(View v)
    {
        startActivity(new Intent(getApplicationContext(), AllSongsActivity.class));
    }

    public void onClickArtists(View v)
    {
        startActivity(new Intent(getApplicationContext(), ArtistsActivity.class));
    }

    public void onClickAlbums(View v)
    {
        startActivity(new Intent(getApplicationContext(), AlbumsActivity.class));
    }

    public void onClickRecents(View v)
    {
        startActivity(new Intent(getApplicationContext(), RecentsActivity.class));
    }

    public void onClickPlaylists(View v)
    {
        startActivity(new Intent(getApplicationContext(), PlaylistActivity.class));
    }

    public void onClickEqualizer(View v)
    {
        startActivity(new Intent(getApplicationContext(), EqualizerActivity.class));
    }

    public void onClickSearch(View v)
    {
        startActivity(new Intent(getApplicationContext(), SearchActivity.class));
    }

    public void showFragment(String type)
    {
        if (type.equals("player"))
        {
            Bundle bundle = new Bundle();
            bundle.putString("Title", musicSrv.getCurrentSongTitle());
            bundle.putString("Artist", musicSrv.getCurrentSongArtist());
            android.support.v4.app.FragmentManager fm = getSupportFragmentManager();
            playerFragment = (PlayerFragment) fm.findFragmentByTag("player");
            if (playerFragment == null)
            {
                playerFragment = new PlayerFragment();
            }
            playerFragment.setArguments(bundle);
            fm.beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_up, R.anim.slide_out_up, R.anim.slide_in_up, R.anim.slide_out_up)
                    .add(R.id.fragContainer, playerFragment, "player")
                    .show(playerFragment)
                    .addToBackStack(null)
                    .commitAllowingStateLoss();
        }
    }

    public void hideFragment(String type)
    {
        if (type.equals("player"))
        {
            android.support.v4.app.FragmentManager fm = getSupportFragmentManager();
            android.support.v4.app.Fragment frag = fm.findFragmentByTag("player");
            if (frag != null) {
                fm.beginTransaction()
                        .remove(frag)
                        .commitAllowingStateLoss();
            }
        }
    }

    public void updateDuration()
    {
        if (selectedSongPos != -1)
        {
            mHandler.postDelayed(mUpdateDurationUI, 100);
        }
    }

    public Runnable mUpdateDurationUI = new Runnable() {
        public void run() {

            try
            {
                final long currentDuration = musicSrv.mediaPlayer.getCurrentPosition();
                trackDurBar.setText(""+milliSecondsToTimer(currentDuration));
                mHandler.postDelayed(this, 100);
            }
            catch (IllegalStateException e)
            {
                e.printStackTrace();
            }

        }
    };

    public String milliSecondsToTimer(long milliseconds){
        String finalTimerString = "";
        String secondsString;

        int hours = (int)( milliseconds / (1000*60*60));
        int minutes = (int)(milliseconds % (1000*60*60)) / (1000*60);
        int seconds = (int) ((milliseconds % (1000*60*60)) % (1000*60) / 1000);

        if(hours > 0){
            finalTimerString = hours + ":";
        }

        if(seconds < 10){
            secondsString = "0" + seconds;
        }else{
            secondsString = "" + seconds;}

        finalTimerString = finalTimerString + minutes + ":" + secondsString;

        return finalTimerString;
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (musicConnection != null) {
            mSensorManager.unregisterListener(musicSrv.mSensorListener);
            unbindService(musicConnection);
        }
        hideFragment("player");
    }

    private class HeadphoneReceiver extends BroadcastReceiver {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
                int state = intent.getIntExtra("state", -1);
                if (Preferences.HEADPHONE_STATE)
                {
                    if (musicSrv != null)
                    {
                        switch (state) {
                            case 0:
                                if (musicSrv.mediaPlayer.isPlaying())
                                {
                                    musicSrv.pauseSong();
                                }
                                break;
                            case 1:
                                break;
                            default:
                                break;
                        }
                    }
                }
            }
        }
    }

    private ServiceConnection musicConnection = new ServiceConnection(){

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder)service;
            musicSrv = binder.getService();
            initTrackBar();
            musicBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicBound = false;
        }
    };
}

