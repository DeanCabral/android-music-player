package com.cryotech.musicplayer;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import java.util.ArrayList;

public class PlaylistSongsActivity extends AppCompatActivity implements AllSongsAdapter.RecyclerViewClickListener{

    private long mID;
    private String mTitle;
    private RecyclerView recyclerView;
    private AllSongsAdapter adapter;
    private ArrayList<LocalTrack> songList;
    private Utilities utils;

    private MusicService musicSrv;
    private Intent playIntent;
    private boolean musicBound = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist_songs);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mTitle = getIntent().getStringExtra("PLAYLIST_TITLE");
        mID = getIntent().getLongExtra("PLAYLIST_ID", -1);
        setTitle(mTitle);

        recyclerView = findViewById(R.id.recycler_view);

        utils = new Utilities();
        songList = new ArrayList<>();
        RecyclerView.LayoutManager mLayoutManager = new GridLayoutManager(this, 1);
        recyclerView.setLayoutManager(mLayoutManager);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL);
        recyclerView.addItemDecoration(dividerItemDecoration);
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        initAdapter();
        initAddSong();
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
    protected void onResume() {
        super.onResume();
        refreshList();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.playlist, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == android.R.id.home)
        {
            finish();
        }
        else if (id == R.id.action_refresh) {
            refreshList();
            musicSrv.setList(songList);
            Toast.makeText(getApplicationContext(), "Refreshed Playlist", Toast.LENGTH_SHORT).show();
        }

        return super.onOptionsItemSelected(item);
    }

    private void initAdapter()
    {
        adapter = new AllSongsAdapter(songList, this, this);
        recyclerView.setAdapter(adapter);
    }

    private void initAddSong()
    {
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent newIntent = new Intent(getApplicationContext(), AllSongsActivity.class);
                newIntent.putExtra("Select", true);
                PlaylistSongsActivity.this.startActivity(newIntent);
            }
        });
    }

    private void refreshList()
    {
        if (mID != -1)
        {
            initAdapter();
            getPlaylistSongsList(mID);
        }
    }

    public void getPlaylistSongsList(long mPlaylistID) {

        if (songList != null)
        {
            songList.clear();
        }
        String duration = "";
        ContentResolver musicResolver = getContentResolver();
        Uri musicUri = MediaStore.Audio.Playlists.Members.getContentUri("external", mPlaylistID);
        Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);

        if(musicCursor!=null && musicCursor.moveToFirst()){

            int idColumn = musicCursor.getColumnIndex
                    (MediaStore.Audio.Playlists.Members.AUDIO_ID);
            int titleColumn = musicCursor.getColumnIndex
                    (MediaStore.Audio.Media.TITLE);
            int artistColumn = musicCursor.getColumnIndex
                    (MediaStore.Audio.Media.ARTIST);
            int albumColumn = musicCursor.getColumnIndex
                    (MediaStore.Audio.Media.ALBUM);
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
                songList.add(new LocalTrack(thisId, thisTitle, thisArtist, thisAlbum, thisPath, duration, -1));
            }
            while (musicCursor.moveToNext());
        }
        musicCursor.close();
        adapter.notifyDataSetChanged();
    }

    @Override
    public void ItemClicked(int position)
    {
        Toast.makeText(PlaylistSongsActivity.this, "Starting Playlist", Toast.LENGTH_SHORT).show();
        Preferences.PLAYLIST = true;
        musicSrv.setList(songList);
        String title = songList.get(position).getTitle();
        String artist = songList.get(position).getArtist();
        musicSrv.setSong(title, artist);
        musicSrv.setSongIndex(position);
        musicSrv.playSong();
    }

    @Override
    public void ItemLongClicked(int position)
    {
        createMenu(position);
    }

    private void createMenu(final int pos)
    {
        final AlertDialog.Builder builderSingle = new AlertDialog.Builder(this, R.style.MyAlertDialogStyle);
        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, R.layout.library_context_menu, R.id.textContext);

        final long songID = songList.get(pos).getId();
        final String songTitle = songList.get(pos).getTitle();
        final String songArtist = songList.get(pos).getArtist();
        final String songAlbum = songList.get(pos).getAlbum();
        final String songPath = songList.get(pos).getPath();

        builderSingle.setTitle("Select Option");

        arrayAdapter.clear();
        arrayAdapter.add("Play Next");
        arrayAdapter.add("Remove From Playlist");
        arrayAdapter.add("Rename");
        arrayAdapter.add("Set As Ringtone");
        arrayAdapter.add("View Details");
        arrayAdapter.add("Delete");

        builderSingle.setNegativeButton(
                "Back",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        builderSingle.setAdapter(
                arrayAdapter,
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        String strName = arrayAdapter.getItem(which);

                        if (strName == "Play Next")
                        {
                            musicSrv.setUpcomingSongIndex(pos);
                            Toast.makeText(getApplicationContext(), "Upcoming song: " + songTitle, Toast.LENGTH_SHORT).show();
                        }
                        else if (strName == "Remove From Playlist")
                        {
                            removeFromPlaylist(songID);
                        }
                        else if (strName == "Rename")
                        {
                            utils.RenameSong(PlaylistSongsActivity.this, musicSrv, adapter, isCurrentlyPlaying(pos), pos, songTitle, songArtist, songAlbum, songID);
                        }
                        else if (strName == "Set As Ringtone")
                        {
                            utils.SetRingtone(PlaylistSongsActivity.this, songTitle, songPath);
                        }
                        else if (strName == "View Details")
                        {
                            utils.ViewDetails(PlaylistSongsActivity.this, songTitle, songArtist, songAlbum, songPath);
                        }
                        else if (strName == "Delete")
                        {
                            utils.DeleteSong(PlaylistSongsActivity.this, musicSrv, songList, adapter, pos, songTitle, songID);
                        }

                    }
                });
        builderSingle.show();

    }

    private void removeFromPlaylist(final long audioId) {

        AlertDialog.Builder builderInner = new AlertDialog.Builder(this, R.style.MyAlertDialogStyle);
        builderInner.setTitle("Stealth Music");
        builderInner.setMessage("Are you sure you want to remove this song from the playlist?");
        builderInner.setPositiveButton(
                "Remove",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(
                            DialogInterface dialog
                            ,
                            int which) {
                        ContentResolver musicResolver = getContentResolver();
                        Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external", mID);

                        musicResolver.delete(uri, MediaStore.Audio.Playlists.Members.AUDIO_ID +" = "+ audioId, null);
                        getPlaylistSongsList(mID);

                        Toast.makeText(PlaylistSongsActivity.this, "Removed Song", Toast.LENGTH_SHORT).show();

                    }
                });
        builderInner.setNegativeButton("Cancel", null);
        builderInner.show();

    }

    private boolean isCurrentlyPlaying(int songIndex)
    {
        if (musicSrv.getCurrentSongIndex() == songIndex)
        {
            return true;
        }
        return false;
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
            musicBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicBound = false;
        }
    };

}
