package com.cryotech.musicplayer;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class AlbumsActivity extends AppCompatActivity implements AlbumsAdapter.RecyclerViewClickListener{

    private RecyclerView recyclerView;
    private AlbumsAdapter adapter;
    private List<LocalTrack> albumsList;
    private ArrayList<LocalTrack> albumSongs;

    private MusicService musicSrv;
    private Intent playIntent;
    private boolean musicBound = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_albums);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        initCollapsingToolbar();

        recyclerView = findViewById(R.id.recycler_view);

        albumsList = new ArrayList<>();
        albumSongs = new ArrayList<>();
        adapter = new AlbumsAdapter(albumsList, this, this);

        RecyclerView.LayoutManager mLayoutManager = new GridLayoutManager(this, 2);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.addItemDecoration(new GridSpacingItemDecoration(2, dpToPx(10), true));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(adapter);

    }

    @Override
    protected void onStart() {
        super.onStart();
        if(playIntent==null){
            playIntent = new Intent(this, MusicService.class);
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
            startService(playIntent);
        }
        loadAlbums();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(AlbumsActivity.this, HomeActivity.class));
        finish();
    }

    private void loadAlbums()
    {
        ContentResolver musicResolver = getContentResolver();
        Uri musicUri = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI;
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

                if (!albumsList.contains(new LocalTrack(thisId, null, null, thisAlbum, null, null, thisCount))) {

                    albumsList.add(new LocalTrack(thisId, null, null, thisAlbum, null, null, thisCount));
                }
            }
            while (musicCursor.moveToNext());
        }
        musicCursor.close();

        for (int i = 0; i < albumsList.size() - 1; i++)
        {
            if (albumsList.get(i).getAlbum().equals(albumsList.get(i + 1).getAlbum()))
            {
                albumsList.remove(i + 1);
                i--;
            }
        }
    }

    private void loopSongs(String albumName)
    {
        ArrayList<LocalTrack> tempList = new ArrayList<>();
        if (!albumSongs.isEmpty())
        {
            tempList.addAll(albumSongs);
            albumSongs.clear();
        }
        ContentResolver musicResolver = getContentResolver();
        Uri musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.ALBUM + "=?";
        String[] selectionArgs = {albumName};
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";
        Cursor musicCursor = musicResolver.query(musicUri, null, selection, selectionArgs, sortOrder);

        if(musicCursor!=null && musicCursor.moveToFirst()){

            int idColumn = musicCursor.getColumnIndex
                    (MediaStore.Audio.Media._ID);
            int titleColumn = musicCursor.getColumnIndex
                    (MediaStore.Audio.Media.TITLE);
            int artistColumn = musicCursor.getColumnIndex
                    (MediaStore.Audio.Media.ARTIST);
            int albumColumn = musicCursor.getColumnIndex
                    (MediaStore.Audio.Media.ALBUM);
            int pathColumn = musicCursor.getColumnIndex
                    (MediaStore.Audio.Media.DATA);

            do {
                long thisId = musicCursor.getLong(idColumn);
                String thisTitle = musicCursor.getString(titleColumn);
                String thisArtist = musicCursor.getString(artistColumn);
                String thisAlbum = musicCursor.getString(albumColumn);
                String thisPath = musicCursor.getString(pathColumn);

                albumSongs.add(new LocalTrack(thisId, thisTitle, thisArtist, thisAlbum, thisPath, null, -1));
            }
            while (musicCursor.moveToNext());
        }
        musicCursor.close();
        Preferences.PLAYLIST = true;
        if (albumSongs.size() > 0)
        {
            musicSrv.setList(albumSongs);
            String title = albumSongs.get(0).getTitle();
            String artist = albumSongs.get(0).getArtist();
            musicSrv.setSong(title, artist);
            musicSrv.setSongIndex(0);
            musicSrv.playSong();
            Toast.makeText(AlbumsActivity.this, "Looping Songs", Toast.LENGTH_SHORT).show();
        }
        else
        {
            musicSrv.setList(tempList);
            Toast.makeText(AlbumsActivity.this, "Empty Playlist", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void albumItemClicked(String album)
    {
        Intent newIntent = new Intent(AlbumsActivity.this, AlbumSongsActivity.class);
        newIntent.putExtra("Album", album);
        AlbumsActivity.this.startActivity(newIntent);
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

        final String albumTitle = albumsList.get(pos).getAlbum();

        builderSingle.setTitle("Select Option");

        arrayAdapter.clear();
        arrayAdapter.add("Loop Songs");

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
                            loopSongs(albumTitle);
                        }

                    }
                });
        builderSingle.show();
    }

    private void initCollapsingToolbar() {
        final CollapsingToolbarLayout collapsingToolbar =
                findViewById(R.id.collapsing_toolbar);
        collapsingToolbar.setTitle(" ");
        AppBarLayout appBarLayout = findViewById(R.id.appbar);
        appBarLayout.setExpanded(true);

        appBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            boolean isShow = false;
            int scrollRange = -1;

            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                if (scrollRange == -1) {
                    scrollRange = appBarLayout.getTotalScrollRange();
                }
                if (scrollRange + verticalOffset == 0) {
                    collapsingToolbar.setTitle("Browse Albums");
                    isShow = true;
                } else if (isShow) {
                    collapsingToolbar.setTitle(" ");
                    isShow = false;
                }
            }
        });
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

    public class GridSpacingItemDecoration extends RecyclerView.ItemDecoration {

        private int spanCount;
        private int spacing;
        private boolean includeEdge;

        public GridSpacingItemDecoration(int spanCount, int spacing, boolean includeEdge) {
            this.spanCount = spanCount;
            this.spacing = spacing;
            this.includeEdge = includeEdge;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view);
            int column = position % spanCount;

            if (includeEdge) {
                outRect.left = spacing - column * spacing / spanCount;
                outRect.right = (column + 1) * spacing / spanCount;

                if (position < spanCount) {
                    outRect.top = spacing;
                }
                outRect.bottom = spacing;
            } else {
                outRect.left = column * spacing / spanCount;
                outRect.right = spacing - (column + 1) * spacing / spanCount;
                if (position >= spanCount) {
                    outRect.top = spacing;
                }
            }
        }
    }

    private int dpToPx(int dp) {
        Resources r = getResources();
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics()));
    }
}
