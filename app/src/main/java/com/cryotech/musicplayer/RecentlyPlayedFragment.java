package com.cryotech.musicplayer;

import android.content.ContentResolver;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class RecentlyPlayedFragment extends Fragment implements AllSongsAdapter.RecyclerViewClickListener {

    private View rootView;
    private RecentsActivity recents;
    private TextView lastPlayedDisplay;
    private TextView limitBtn;
    private RecyclerView recyclerView;
    private AllSongsAdapter adapter;
    private ArrayList<LocalTrack> songList;
    private Utilities utils;

    private ArrayAdapter<String> arrayAdapter;
    private AlertDialog.Builder builderInner;

    public RecentlyPlayedFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_recently_played, container, false);
        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        lastPlayedDisplay = rootView.findViewById(R.id.lastPlayed_display);
        recyclerView = rootView.findViewById(R.id.recycler_view);
        limitBtn = rootView.findViewById(R.id.lastPlayed_limit);
        recents = (RecentsActivity)getActivity();

        builderInner = new AlertDialog.Builder(getActivity(), R.style.MyAlertDialogStyle);
        arrayAdapter = new ArrayAdapter<>(getActivity(), R.layout.library_context_menu, R.id.textContext);

        utils = new Utilities();
        songList = new ArrayList<>();
        RecyclerView.LayoutManager mLayoutManager = new GridLayoutManager(getActivity(), 1);
        recyclerView.setLayoutManager(mLayoutManager);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL);
        recyclerView.addItemDecoration(dividerItemDecoration);
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        initAdapter();
        initLimit();
    }

    @Override
    public void onStop() {
        super.onStop();

        long[] idList = new long[songList.size()];
        for (int i = 0; i < songList.size(); i++)
        {
            idList[i] = songList.get(i).getId();
        }
        recents.saveRecents(idList);
    }

    public void initAdapter()
    {
        adapter = new AllSongsAdapter(songList, getActivity(), this);
        recyclerView.setAdapter(adapter);
    }

    public void initRecentlyPlayed()
    {
        recents.musicSrv.setRecentSize(recents.musicSrv.getRecentSize());

        if (recents.musicSrv.getRecentSize() < recents.musicSrv.recentSongs.size())
        {
            recents.musicSrv.recentSongs.subList(recents.musicSrv.getRecentSize(), recents.musicSrv.recentSongs.size()).clear();
        }

        if (songList.isEmpty())
        {
            for (int i = 0; i < recents.loadRecents().length; i++)
            {
                loadSongsViaID(recents.loadRecents()[i]);
            }
        }

        loadSongs();
        lastPlayedDisplay.setText("Displaying " + songList.size() + " Result(s)");
    }

    private void initLimit()
    {
        limitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                builderInner.setTitle("Limit Size");

                arrayAdapter.clear();
                arrayAdapter.add("5 songs");
                arrayAdapter.add("10 songs");
                arrayAdapter.add("15 songs");

                builderInner.setNegativeButton(
                        "Back",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });

                builderInner.setAdapter(
                        arrayAdapter,
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                String strName = arrayAdapter.getItem(which);

                                if (strName == "5 songs")
                                {
                                    recents.musicSrv.setRecentSize(5);
                                }
                                else if (strName == "10 songs")
                                {
                                    recents.musicSrv.setRecentSize(10);
                                }
                                else if (strName == "15 songs")
                                {
                                    recents.musicSrv.setRecentSize(15);
                                }

                                initRecentlyPlayed();

                            }
                        });
                builderInner.show();

            }
        });
    }

    public void loadSongs() {

        songList.clear();
        songList.addAll(recents.musicSrv.recentSongs);
        adapter.notifyDataSetChanged();
    }

    private void loadSongsViaID(long id)
    {
        String duration;
        ContentResolver musicResolver = getActivity().getContentResolver();
        String selection = MediaStore.Audio.Media._ID + "=?";
        String[] selectionArgs = new String[] {"" + id};
        Uri musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor musicCursor = musicResolver.query(musicUri, null, selection, selectionArgs, null);

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
            int durColumn = musicCursor.getColumnIndex
                    (MediaStore.Audio.Media.DURATION);

            do {
                long thisId = musicCursor.getLong(idColumn);
                String thisTitle = musicCursor.getString(titleColumn);
                String thisArtist = musicCursor.getString(artistColumn);
                String thisAlbum = musicCursor.getString(albumColumn);
                String thisPath = musicCursor.getString(pathColumn);
                long thisDuration = musicCursor.getLong(durColumn);

                if (thisDuration > 10000)
                {
                    if(String.valueOf(thisDuration) != null){
                        try{
                            Long time = Long.valueOf(thisDuration);
                            long seconds = time/1000;
                            long minutes = seconds/60;
                            seconds = seconds % 60;

                            if(seconds<10){
                                String csongs_duration = String.valueOf(minutes) + ":0" + String.valueOf(seconds);
                                duration = csongs_duration;
                            }else{
                                String ccsongs_duration = String.valueOf(minutes) + ":" + String.valueOf(seconds);
                                duration = ccsongs_duration;
                            }
                        }catch(NumberFormatException e){
                            duration = String.valueOf(thisDuration);
                        }
                    }else{
                        duration = "null";
                    }
                    if (recents.musicSrv.recentSongs.size() < recents.musicSrv.getRecentSize())
                    {
                        recents.musicSrv.recentSongs.add(new LocalTrack(thisId, thisTitle, thisArtist, thisAlbum, thisPath, duration, -1));
                    }

                }
            }
            while (musicCursor.moveToNext());
        }
        musicCursor.close();
    }

    @Override
    public void ItemClicked(int position) {

        if (recents.musicSrv != null)
        {
            recents.musicSrv.setList(songList);
        }

        String title = songList.get(position).getTitle();
        String artist = songList.get(position).getArtist();
        recents.musicSrv.setSong(title, artist);
        recents.musicSrv.setSongIndex(position);
        recents.musicSrv.playSong();

    }

    @Override
    public void ItemLongClicked(int position)
    {
        createMenu(position);
    }

    private void createMenu(final int pos)
    {
        final android.app.AlertDialog.Builder builderSingle = new android.app.AlertDialog.Builder(getActivity(), R.style.MyAlertDialogStyle);
        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getActivity(), R.layout.library_context_menu, R.id.textContext);

        final long songID = songList.get(pos).getId();
        final String songTitle = songList.get(pos).getTitle();
        final String songArtist = songList.get(pos).getArtist();
        final String songAlbum = songList.get(pos).getAlbum();
        final String songPath = songList.get(pos).getPath();

        builderSingle.setTitle("Select Option");

        arrayAdapter.clear();
        arrayAdapter.add("Play Next");
        arrayAdapter.add("Add To Playlist");
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
                            Toast.makeText(getActivity(), "Upcoming song: " + songTitle, Toast.LENGTH_SHORT).show();
                        }
                        else if (strName == "Add To Playlist")
                        {
                            utils.CreatePlaylistMenu(getActivity(), songID);
                        }
                        else if (strName == "Rename")
                        {
                            utils.RenameSong(getActivity(), recents.musicSrv, adapter, isCurrentlyPlaying(pos), pos, songTitle, songArtist, songAlbum, songID);
                        }
                        else if (strName == "Set As Ringtone")
                        {
                            utils.SetRingtone(getActivity(), songTitle, songPath);
                        }
                        else if (strName == "View Details")
                        {
                            utils.ViewDetails(getActivity(), songTitle, songArtist, songAlbum, songPath);
                        }
                        else if (strName == "Delete")
                        {
                            utils.DeleteSong(getActivity(), recents.musicSrv, songList, adapter, pos, songTitle, songID);
                        }

                    }
                });
        builderSingle.show();

    }

    private boolean isCurrentlyPlaying(int songIndex)
    {
        if (recents.musicSrv.getCurrentSongIndex() == songIndex)
        {
            return true;
        }
        return false;
    }
}
