package com.cryotech.musicplayer;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class PlaylistActivity extends AppCompatActivity implements PlaylistAdapter.RecyclerViewClickListener{

    private RecyclerView recyclerView;
    private PlaylistAdapter adapter;
    private ArrayList<LocalTrack> playlistList;
    private ArrayList<LocalTrack> playlistSongs;
    private boolean createPlaylist;

    private MusicService musicSrv;
    private Intent playIntent;
    private boolean musicBound = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle("Playlist Library");

        createPlaylist = getIntent().getBooleanExtra("NewPlaylist", false);

        recyclerView = findViewById(R.id.recycler_view);

        playlistList = new ArrayList<>();
        playlistSongs = new ArrayList<>();
        adapter = new PlaylistAdapter(playlistList, this, this);
        RecyclerView.LayoutManager mLayoutManager = new GridLayoutManager(this, 2);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(adapter);

        getPlaylists();
        initCreatePlaylist();
        if (createPlaylist) createPlaylistDialog();
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
        startActivity(new Intent(PlaylistActivity.this, HomeActivity.class));
        finish();
    }

    private void initCreatePlaylist()
    {
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createPlaylistDialog();
            }
        });
    }

    private void createPlaylistDialog()
    {
        final AlertDialog.Builder builderInner = new AlertDialog.Builder(PlaylistActivity.this, R.style.MyAlertDialogStyle);
        final AlertDialog alert = builderInner.create();

        LayoutInflater li = LayoutInflater.from(this);
        View createView = li.inflate(R.layout.create_playlist_dialog, null);

        final EditText playlistInput = createView.findViewById(R.id.playlistInput);
        final Button btnSet = createView.findViewById(R.id.btnCreate);
        final Button btnCancel = createView.findViewById(R.id.btnCancel);

        playlistInput.getBackground().setColorFilter(Color.parseColor("#222222"), PorterDuff.Mode.SRC_ATOP);

        btnSet.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                String mPlaylistName;

                mPlaylistName = playlistInput.getText().toString();

                if (!mPlaylistName.matches(""))
                {
                    addPlaylist(mPlaylistName);
                    getPlaylists();
                    Toast.makeText(PlaylistActivity.this, "Created Playlist", Toast.LENGTH_SHORT).show();
                    alert.dismiss();
                }
                else
                {
                    Toast.makeText(PlaylistActivity.this, "Empty Field", Toast.LENGTH_SHORT).show();
                }
            }

        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                alert.dismiss();

            }
        });

        alert.setView(createView);
        alert.show();
    }

    private void getPlaylists() {

        if (playlistList != null)
        {
            playlistList.clear();
        }
        ContentResolver musicResolver = getContentResolver();
        Uri musicUri = MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI;
        Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);

        if(musicCursor!=null && musicCursor.moveToFirst()){
            int idColumn = musicCursor.getColumnIndex
                    (MediaStore.Audio.Playlists._ID);
            int titleColumn = musicCursor.getColumnIndex
                    (MediaStore.Audio.Playlists.NAME);
            int pathColumn =  musicCursor.getColumnIndex
                    (MediaStore.Audio.Playlists.DATA);

            do {
                long thisId = musicCursor.getLong(idColumn);
                String thisTitle = musicCursor.getString(titleColumn);
                String thisPath = musicCursor.getString(pathColumn);
                playlistList.add(new LocalTrack(thisId, thisTitle, null, null, thisPath, null, 0));
            }
            while (musicCursor.moveToNext());
        }
        musicCursor.close();
        adapter.notifyDataSetChanged();
    }

    private void addPlaylist(String pname) {

        ContentResolver playlistResolver = getContentResolver();
        Uri playlists = MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI;
        Cursor c = playlistResolver.query(playlists, new String[] { "*" }, null, null, null);
        long playlistId = 0;

        if(c!=null && c.moveToFirst()){

            do {
                String plname = c.getString(c.getColumnIndex(MediaStore.Audio.Playlists.NAME));
                if (plname.equalsIgnoreCase(pname)) {
                    playlistId = c.getLong(c.getColumnIndex(MediaStore.Audio.Playlists._ID));
                    break;
                }
            }
            while (c.moveToNext());
        }
        c.close();

        if (playlistId != 0) {
            Uri deleteUri = ContentUris.withAppendedId(playlists, playlistId);
            playlistResolver.delete(deleteUri, null, null);
        }

        ContentValues v1 = new ContentValues();
        v1.put(MediaStore.Audio.Playlists.NAME, pname);
        v1.put(MediaStore.Audio.Playlists.DATE_MODIFIED,
                System.currentTimeMillis());
        Uri newpl = playlistResolver.insert(playlists, v1);

    }

    private void loopPlaylist(long playlistID)
    {
        ArrayList<LocalTrack> tempList = new ArrayList<>();
        if (!playlistSongs.isEmpty())
        {
            tempList.addAll(playlistSongs);
            playlistSongs.clear();
        }
        ContentResolver musicResolver = getContentResolver();
        String selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0";
        Uri musicUri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlistID);
        String sortOrder = MediaStore.Audio.Playlists.Members.TITLE + " ASC";
        Cursor musicCursor = musicResolver.query(musicUri, null, selection, null, sortOrder);

        if(musicCursor!=null && musicCursor.moveToFirst()){

            int idColumn = musicCursor.getColumnIndex
                    (MediaStore.Audio.Playlists.Members.AUDIO_ID);
            int titleColumn = musicCursor.getColumnIndex
                    (MediaStore.Audio.Playlists.Members.TITLE);
            int artistColumn = musicCursor.getColumnIndex
                    (MediaStore.Audio.Playlists.Members.ARTIST);
            int albumColumn = musicCursor.getColumnIndex
                    (MediaStore.Audio.Playlists.Members.ALBUM);
            int pathColumn = musicCursor.getColumnIndex
                    (MediaStore.Audio.Playlists.Members.DATA);

            do {
                long thisId = musicCursor.getLong(idColumn);
                String thisTitle = musicCursor.getString(titleColumn);
                String thisArtist = musicCursor.getString(artistColumn);
                String thisAlbum = musicCursor.getString(albumColumn);
                String thisPath = musicCursor.getString(pathColumn);

                playlistSongs.add(new LocalTrack(thisId, thisTitle, thisArtist, thisAlbum, thisPath, null, -1));
            }
            while (musicCursor.moveToNext());
        }
        musicCursor.close();
        Preferences.PLAYLIST = true;
        if (playlistSongs.size() > 0)
        {
            musicSrv.setList(playlistSongs);
            String title = playlistSongs.get(0).getTitle();
            String artist = playlistSongs.get(0).getArtist();
            musicSrv.setSong(title, artist);
            musicSrv.setSongIndex(0);
            musicSrv.playSong();
            Toast.makeText(PlaylistActivity.this, "Looping Songs", Toast.LENGTH_SHORT).show();
        }
        else
        {
            musicSrv.setList(tempList);
            Toast.makeText(PlaylistActivity.this, "Empty Playlist", Toast.LENGTH_SHORT).show();
        }
    }

    private void renamePlaylist(final long playlistID, final String playlistName)
    {
        final AlertDialog.Builder builderInner = new AlertDialog.Builder(this, R.style.MyAlertDialogStyle);
        final AlertDialog alert = builderInner.create();

        LayoutInflater li = LayoutInflater.from(this);
        View renameView = li.inflate(R.layout.rename_dialog, null);

        final TextView dialogLabel = renameView.findViewById(R.id.renameTitle);
        final EditText playlistTitleInput = renameView.findViewById(R.id.renameInput);
        final Button btnRename = renameView.findViewById(R.id.btnRename);
        final Button btnCancel = renameView.findViewById(R.id.btnCancel);

        dialogLabel.setText("Rename Playlist");
        playlistTitleInput.getBackground().setColorFilter(Color.parseColor("#222222"), PorterDuff.Mode.SRC_ATOP);

        playlistTitleInput.setSingleLine();
        playlistTitleInput.setText(playlistName, TextView.BufferType.EDITABLE);
        playlistTitleInput.setSelection(playlistTitleInput.getText().length());

        btnRename.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                String PlaylistTitleInput;
                String currTitle;

                PlaylistTitleInput = playlistTitleInput.getText().toString();
                PlaylistTitleInput = PlaylistTitleInput.replaceAll("\\p{P}","");
                currTitle = playlistName;
                currTitle = currTitle.replaceAll("\\p{P}","");

                if (PlaylistTitleInput.matches(currTitle))
                {
                    Toast.makeText(PlaylistActivity.this, "No changes have been made", Toast.LENGTH_SHORT).show();
                }
                else if (!PlaylistTitleInput.matches(currTitle) && !PlaylistTitleInput.matches(""))
                {
                    ContentValues values = new ContentValues(1);
                    values.put(MediaStore.Audio.Playlists.NAME, PlaylistTitleInput);
                    getApplicationContext().getContentResolver().update(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, values, "_id=" + playlistID, null);
                    getPlaylists();
                    adapter.notifyDataSetChanged();
                    Toast.makeText(PlaylistActivity.this, "Playlist has been renamed", Toast.LENGTH_SHORT).show();
                    alert.dismiss();
                }
                else
                {
                    Toast.makeText(PlaylistActivity.this, "Fields can not be blank", Toast.LENGTH_SHORT).show();
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

    private void deletePlaylist(final long playlistID, final PlaylistAdapter adapter)
    {
        AlertDialog.Builder builderInner = new AlertDialog.Builder(PlaylistActivity.this, R.style.MyAlertDialogStyle);
        builderInner.setTitle("Stealth Music");
        builderInner.setMessage("Are you sure you want to delete this playlist?");
        builderInner.setPositiveButton(
                "Delete",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(
                            DialogInterface dialog,
                            int which) {

                        Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                                playlistID);
                        String selection = MediaStore.Audio.Playlists._ID+ "=?";
                        String audioId = Long.toString(playlistID);
                        String[] selectionArgs = {audioId};

                        getContentResolver().delete(uri, selection, selectionArgs);
                        getPlaylists();
                        adapter.notifyDataSetChanged();

                        Toast.makeText(PlaylistActivity.this, "Deleted Playlist", Toast.LENGTH_SHORT).show();

                    }
                });
        builderInner.setNegativeButton("Cancel", null);
        builderInner.show();
    }

    @Override
    public void ItemClicked(int position) {

        long playlistID = playlistList.get(position).getId();
        String playlistTitle = playlistList.get(position).getTitle();
        Intent viewPlaylistIntent = new Intent(getApplicationContext(), PlaylistSongsActivity.class);
        viewPlaylistIntent.putExtra("PLAYLIST_ID", playlistID);
        viewPlaylistIntent.putExtra("PLAYLIST_TITLE", playlistTitle);
        startActivity(viewPlaylistIntent);

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

        final long playlistID = playlistList.get(pos).getId();
        final String playlistTitle = playlistList.get(pos).getTitle();

        builderSingle.setTitle("Select Option");

        arrayAdapter.clear();
        arrayAdapter.add("Loop Songs");
        arrayAdapter.add("Rename Playlist");
        arrayAdapter.add("Delete Playlist");

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

                        if (strName == "Loop Songs")
                        {
                            loopPlaylist(playlistID);
                        }
                        else if (strName == "Rename Playlist")
                        {
                            renamePlaylist(playlistID, playlistTitle);
                        }
                        else if (strName == "Delete Playlist")
                        {
                            deletePlaylist(playlistID, adapter);
                        }

                    }
                });
        builderSingle.show();

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
