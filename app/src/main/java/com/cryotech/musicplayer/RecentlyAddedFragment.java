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

public class RecentlyAddedFragment extends Fragment implements AllSongsAdapter.RecyclerViewClickListener {

    private View rootView;
    private RecentsActivity recents;
    private TextView lastAddedTitle;
    private TextView lastAddedDisplay;
    private TextView sortBtn;
    private RecyclerView recyclerView;
    private AllSongsAdapter adapter;
    private ArrayList<LocalTrack> songList;
    private Utilities utils;

    private ArrayAdapter<String> arrayAdapter;
    private AlertDialog.Builder builderInner;

    private int numDays = 7;

    public RecentlyAddedFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_recently_added, container, false);
        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        lastAddedTitle = rootView.findViewById(R.id.lastAddedRecyclerLabel);
        lastAddedDisplay = rootView.findViewById(R.id.lastAdded_display);
        recyclerView = rootView.findViewById(R.id.recycler_view);
        sortBtn = rootView.findViewById(R.id.lastAdded_sort);
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
        initRecentlyAdded();
        initSort();
    }

    public void initAdapter()
    {
        adapter = new AllSongsAdapter(songList, getActivity(), this);
        recyclerView.setAdapter(adapter);
    }

    private void initRecentlyAdded()
    {
        loadSongs();
        lastAddedTitle.setText(numDays + " day(s) ago");
        lastAddedDisplay.setText("Displaying " + songList.size() + " Result(s)");
    }

    private void initSort()
    {
        sortBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                builderInner.setTitle("Filter Date");

                arrayAdapter.clear();
                arrayAdapter.add("1 day");
                arrayAdapter.add("7 days");
                arrayAdapter.add("14 days");
                arrayAdapter.add("21 days");
                arrayAdapter.add("28 days");

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

                                if (strName == "1 day")
                                {
                                    numDays = 1;
                                }
                                else if (strName == "7 days")
                                {
                                    numDays = 7;
                                }
                                else if (strName == "14 days")
                                {
                                    numDays = 14;
                                }
                                else if (strName == "21 days")
                                {
                                    numDays = 21;
                                }
                                else if (strName == "28 days")
                                {
                                    numDays = 28;
                                }

                                songList.clear();
                                initRecentlyAdded();
                                adapter.notifyDataSetChanged();
                            }
                        });
                builderInner.show();

            }
        });
    }

    public void loadSongs() {

        String duration;
        ContentResolver musicResolver = getActivity().getContentResolver();
        Uri musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String where = MediaStore.Audio.Media.DATE_ADDED + ">" + (System.currentTimeMillis() / 1000 - numDays*60*60*24);
        Cursor musicCursor = musicResolver.query(musicUri, null, where, null, null);

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
                    songList.add(new LocalTrack(thisId, thisTitle, thisArtist, thisAlbum, thisPath, duration, -1));
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
