package com.cryotech.musicplayer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.media.MediaMetadataRetriever;
import android.media.RingtoneManager;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;

public class Utilities {

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

    private void AddToPlaylist(final Activity activity, long audioId, long pID) {

        boolean canAdd = true;
        ContentResolver musicResolver = activity.getContentResolver();
        Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external", pID);
        Cursor cur = musicResolver.query(uri, null, null, null, null);

        if (Preferences.DUPLICATE_MEMBERS)
        {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, audioId);
            values.put(MediaStore.Audio.Playlists.Members.AUDIO_ID, audioId);
            musicResolver.insert(uri, values);
            Toast.makeText(activity, "Added Song", Toast.LENGTH_SHORT).show();
        }
        else
        {
            if(cur!=null && cur.moveToFirst()){

                int idColumn = cur.getColumnIndex
                        (MediaStore.Audio.Playlists.Members.AUDIO_ID);
                do {
                    long thisId = cur.getLong(idColumn);
                    if (thisId == audioId) canAdd = false;
                }
                while (cur.moveToNext());
            }
            cur.close();
            if (canAdd)
            {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, audioId);
                values.put(MediaStore.Audio.Playlists.Members.AUDIO_ID, audioId);
                musicResolver.insert(uri, values);
                Toast.makeText(activity, "Added Song", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(activity, "Song Already Exists", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void CreatePlaylistMenu(final Activity activity, final long audioID)
    {
        final AlertDialog.Builder builderSingle = new AlertDialog.Builder(activity, R.style.MyAlertDialogStyle);
        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(activity, R.layout.library_context_menu, R.id.textContext);
        final ArrayList<Long> playlistIDs = new ArrayList<>();
        builderSingle.setTitle("Select Playlist");

        arrayAdapter.clear();
        playlistIDs.clear();

        ContentResolver musicResolver = activity.getContentResolver();
        Uri musicUri = MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI;
        Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);

        if(musicCursor!=null && musicCursor.moveToFirst()){
            int idColumn = musicCursor.getColumnIndex
                    (MediaStore.Audio.Playlists._ID);
            int titleColumn = musicCursor.getColumnIndex
                    (MediaStore.Audio.Playlists.NAME);
            do {
                long thisId = musicCursor.getLong(idColumn);
                String thisTitle = musicCursor.getString(titleColumn);
                arrayAdapter.add(thisTitle);
                playlistIDs.add(thisId);
            }
            while (musicCursor.moveToNext());
        }
        musicCursor.close();

        if (arrayAdapter.isEmpty()) arrayAdapter.add("No Playlist Found. Click here to create one.");
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

                        if (strName == "No Playlist Found. Click here to create one.")
                        {
                            Intent newIntent = new Intent(activity, PlaylistActivity.class);
                            newIntent.putExtra("NewPlaylist", true);
                            activity.startActivity(newIntent);
                        }
                        else
                        {
                            int position = arrayAdapter.getPosition(strName);
                            AddToPlaylist(activity, audioID, playlistIDs.get(position));
                        }
                    }
                });
        builderSingle.show();
    }

    public void RenameSong(final Activity activity, final MusicService musicService, final AllSongsAdapter adapter, final boolean isPlaying, final int songPOS, final String songTITLE, final String songARTIST, final String songALBUM, final long songID)
    {

        final AlertDialog.Builder builderInner = new AlertDialog.Builder(activity, R.style.MyAlertDialogStyle);
        final AlertDialog alert = builderInner.create();

        LayoutInflater li = LayoutInflater.from(activity);
        View renameView = li.inflate(R.layout.rename_song_dialog, null);

        final EditText songTitleInput = renameView.findViewById(R.id.renameSongTitleInput);
        final EditText songArtistInput = renameView.findViewById(R.id.renameSongArtistInput);
        final EditText songAlbumInput = renameView.findViewById(R.id.renameSongAlbumInput);
        final Button btnRename = renameView.findViewById(R.id.btnRenameSong);
        final Button btnCancel = renameView.findViewById(R.id.btnCancelSongRename);

        songTitleInput.getBackground().setColorFilter(Color.parseColor("#222222"), PorterDuff.Mode.SRC_ATOP);
        songArtistInput.getBackground().setColorFilter(Color.parseColor("#222222"), PorterDuff.Mode.SRC_ATOP);
        songAlbumInput.getBackground().setColorFilter(Color.parseColor("#222222"), PorterDuff.Mode.SRC_ATOP);

        songTitleInput.setSingleLine();
        songTitleInput.setText(songTITLE, TextView.BufferType.EDITABLE);
        songTitleInput.setSelection(songTitleInput.getText().length());

        songArtistInput.setSingleLine();
        songArtistInput.setText(songARTIST, TextView.BufferType.EDITABLE);

        songAlbumInput.setSingleLine();
        songAlbumInput.setText(songALBUM, TextView.BufferType.EDITABLE);

        btnRename.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                String SongTitleInput;
                String SongArtistInput;
                String SongAlbumInput;
                String currTitle;
                String currArtist;
                String currAlbum;

                SongTitleInput = songTitleInput.getText().toString();
                SongArtistInput = songArtistInput.getText().toString();
                SongAlbumInput = songAlbumInput.getText().toString();

                SongTitleInput = SongTitleInput.replaceAll("\\p{P}","");
                SongArtistInput = SongArtistInput.replaceAll("\\p{P}","");
                SongAlbumInput = SongAlbumInput.replaceAll("\\p{P}","");

                currTitle = songTITLE;
                currArtist = songARTIST;
                currAlbum = songALBUM;

                currTitle = currTitle.replaceAll("\\p{P}","");
                currArtist = currArtist.replaceAll("\\p{P}","");
                currAlbum = currAlbum.replaceAll("\\p{P}","");

                if (SongTitleInput.matches(currTitle) && SongArtistInput.matches(currArtist) && SongAlbumInput.matches(currAlbum))
                {
                    Toast.makeText(activity.getApplicationContext(), "No changes have been made", Toast.LENGTH_SHORT).show();
                }
                else if (!SongTitleInput.matches(currTitle) && !SongTitleInput.matches("") && SongArtistInput.matches(currArtist) && SongAlbumInput.matches(currAlbum))
                {
                    ContentValues values = new ContentValues(1);
                    values.put(MediaStore.Audio.Media.TITLE, SongTitleInput);

                    activity.getApplicationContext().getContentResolver().update(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            values, "_id=" + songID, null);

                    adapter.updateItem(songPOS, SongTitleInput, currArtist, currAlbum);
                    Toast.makeText(activity.getApplicationContext(), "Title has been renamed", Toast.LENGTH_SHORT).show();
                    alert.dismiss();

                }
                else if (!SongArtistInput.matches(currArtist) && !SongArtistInput.matches("") && SongTitleInput.matches(currTitle) && SongAlbumInput.matches(currAlbum))
                {
                    ContentValues values = new ContentValues(1);
                    values.put(MediaStore.Audio.Media.ARTIST, SongArtistInput);

                    activity.getApplicationContext().getContentResolver().update(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            values, "_id=" + songID, null);

                    adapter.updateItem(songPOS, currTitle, SongArtistInput, currAlbum);
                    Toast.makeText(activity.getApplicationContext(), "Artist has been renamed", Toast.LENGTH_SHORT).show();
                    alert.dismiss();
                }
                else if (!SongAlbumInput.matches(currAlbum) && !SongAlbumInput.matches("") && SongTitleInput.matches(currTitle) && SongArtistInput.matches(currArtist))
                {
                    ContentValues values = new ContentValues(1);
                    values.put(MediaStore.Audio.Media.ALBUM, SongAlbumInput);

                    activity.getApplicationContext().getContentResolver().update(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            values, "_id=" + songID, null);

                    adapter.updateItem(songPOS, currTitle, currArtist, SongAlbumInput);
                    Toast.makeText(activity.getApplicationContext(), "Album has been renamed", Toast.LENGTH_SHORT).show();
                    alert.dismiss();
                }
                else if (!SongTitleInput.matches(currTitle) && !SongTitleInput.matches("") && !SongArtistInput.matches(currArtist) && !SongArtistInput.matches("") && SongAlbumInput.matches(currAlbum))
                {
                    ContentValues values = new ContentValues(1);
                    values.put(MediaStore.Audio.Media.TITLE, SongTitleInput);
                    values.put(MediaStore.Audio.Media.ARTIST, SongArtistInput);

                    activity.getApplicationContext().getContentResolver().update(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            values, "_id=" + songID, null);

                    adapter.updateItem(songPOS, SongTitleInput, SongArtistInput, currAlbum);
                    Toast.makeText(activity.getApplicationContext(), "Title and Artist have been renamed", Toast.LENGTH_SHORT).show();
                    alert.dismiss();
                }
                else if (!SongTitleInput.matches(currTitle) && !SongTitleInput.matches("") && !SongAlbumInput.matches(currAlbum) && !SongAlbumInput.matches("") && SongArtistInput.matches(currArtist))
                {
                    ContentValues values = new ContentValues(1);
                    values.put(MediaStore.Audio.Media.TITLE, SongTitleInput);
                    values.put(MediaStore.Audio.Media.ALBUM, SongAlbumInput);

                    activity.getApplicationContext().getContentResolver().update(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            values, "_id=" + songID, null);

                    adapter.updateItem(songPOS, SongTitleInput, currArtist, SongAlbumInput);
                    Toast.makeText(activity.getApplicationContext(), "Title and Album have been renamed", Toast.LENGTH_SHORT).show();
                    alert.dismiss();
                }
                else if (!SongArtistInput.matches(currArtist) && !SongArtistInput.matches("") && !SongAlbumInput.matches(currAlbum) && !SongAlbumInput.matches("") && SongTitleInput.matches(currTitle))
                {
                    ContentValues values = new ContentValues(1);
                    values.put(MediaStore.Audio.Media.ARTIST, SongArtistInput);
                    values.put(MediaStore.Audio.Media.ALBUM, SongAlbumInput);

                    activity.getApplicationContext().getContentResolver().update(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            values, "_id=" + songID, null);

                    adapter.updateItem(songPOS, currTitle, SongArtistInput, SongAlbumInput);
                    Toast.makeText(activity.getApplicationContext(), "Artist and Album have been renamed", Toast.LENGTH_SHORT).show();
                    alert.dismiss();
                }
                else if (!SongArtistInput.matches(currArtist) && !SongArtistInput.matches("") && !SongTitleInput.matches(currTitle) && !SongTitleInput.matches("") && !SongAlbumInput.matches(currAlbum) && !SongAlbumInput.matches(""))
                {
                    ContentValues values = new ContentValues(1);
                    values.put(MediaStore.Audio.Media.TITLE, SongTitleInput);
                    values.put(MediaStore.Audio.Media.ARTIST, SongArtistInput);
                    values.put(MediaStore.Audio.Media.ALBUM, SongAlbumInput);

                    activity.getApplicationContext().getContentResolver().update(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            values, "_id=" + songID, null);

                    adapter.updateItem(songPOS, SongTitleInput, SongArtistInput, SongAlbumInput);
                    Toast.makeText(activity.getApplicationContext(), "All fields have been renamed", Toast.LENGTH_SHORT).show();
                    alert.dismiss();
                }
                else
                {
                    Toast.makeText(activity.getApplicationContext(), "Fields can not be blank", Toast.LENGTH_SHORT).show();
                }

                if (isPlaying)
                {
                    musicService.setSong(SongTitleInput, SongArtistInput);
                    musicService.showNotification();
                }
                else
                {
                    musicService.initOriginalList();
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

    public void SetRingtone(final Activity activity, String songTITLE, final String songPATH)
    {
        AlertDialog.Builder builderInner = new AlertDialog.Builder(activity, R.style.MyAlertDialogStyle);
        builderInner.setTitle("Stealth Music");
        builderInner.setMessage("Are you sure you want to set '" + songTITLE + "' as your ringtone?");
        builderInner.setPositiveButton(
                "Yes",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(
                            DialogInterface dialog,
                            int which) {
                        Uri uri = MediaStore.Audio.Media.getContentUriForPath(songPATH);

                        RingtoneManager.setActualDefaultRingtoneUri(activity.getApplicationContext(),
                                RingtoneManager.TYPE_RINGTONE, uri);
                        Toast.makeText(activity.getApplicationContext(), "Ringtone Set", Toast.LENGTH_SHORT).show();

                    }
                });
        builderInner.setNegativeButton("No", null);
        builderInner.show();
    }

    public void ViewDetails (Activity activity, String songTITLE, String songARTIST, String songALBUM, String songPATH)
    {
        AlertDialog.Builder builderView = new AlertDialog.Builder(activity, R.style.MyAlertDialogStyle);

        LayoutInflater li = LayoutInflater.from(activity);
        View detailsView = li.inflate(R.layout.details_dialog, null);

        MediaMetadataRetriever metaRetriever = new MediaMetadataRetriever();
        metaRetriever.setDataSource(songPATH);
        final String duration = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        final Long longDuration = Long.parseLong(duration);

        final TextView mSongTitle = detailsView.findViewById(R.id.mSongTitle);
        final TextView mSongArtist = detailsView.findViewById(R.id.mSongArtist);
        final TextView mSongAlbum = detailsView.findViewById(R.id.mAlbum);
        final TextView mSongDuration = detailsView.findViewById(R.id.mDuration);
        final TextView mSongLocation = detailsView.findViewById(R.id.mFolderLocation);

        mSongTitle.setText(songTITLE);
        mSongTitle.setTextColor(Color.parseColor("#FFFFFF"));
        mSongArtist.setText(songARTIST);
        mSongArtist.setTextColor(Color.parseColor("#FFFFFF"));
        mSongAlbum.setText(songALBUM);
        mSongAlbum.setTextColor(Color.parseColor("#FFFFFF"));
        mSongDuration.setText(String.valueOf(""+milliSecondsToTimer(longDuration)));
        mSongDuration.setTextColor(Color.parseColor("#FFFFFF"));
        mSongLocation.setText(songPATH);
        mSongLocation.setTextColor(Color.parseColor("#FFFFFF"));

        builderView.setView(detailsView);

        builderView.setNegativeButton(
                "Back",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        builderView.show();
    }

    public void DeleteSong(final Activity activity, final MusicService musicService, final ArrayList<LocalTrack> songLIST, final AllSongsAdapter adapter, final int songPOS, final String songTITLE, final long songID)
    {
        AlertDialog.Builder builderInner = new AlertDialog.Builder(activity, R.style.MyAlertDialogStyle);
        builderInner.setTitle("Cryotech Music");
        builderInner.setMessage("Are you sure you want to delete the song '" + songTITLE + "'?");
        builderInner.setPositiveButton(
                "Delete",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(
                            DialogInterface dialog,
                            int which) {

                        Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                songID);
                        String selection = MediaStore.Audio.Media._ID+ "=?";
                        String audioId = Long.toString(songID);
                        String[] selectionArgs = {audioId};
                        activity.getApplicationContext().getContentResolver().delete(uri, selection, selectionArgs);

                        File file = new File(songLIST.get(songPOS).getPath());
                        file.delete();
                        activity.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
                        adapter.removeItem(songPOS);
                        musicService.initOriginalList();
                        musicService.initRefList();
                        Toast.makeText(activity, "Deleted Song", Toast.LENGTH_SHORT).show();

                    }
                });
        builderInner.setNegativeButton("Cancel", null);
        builderInner.show();
    }


//    public void RenamePlaylist(final Activity activity, final String playlistTITLE, final long playlistID, final ArrayList<Playlist> pList, final PlaylistAdapter adapter)
//    {
//        AlertDialog.Builder builderSingle = new AlertDialog.Builder(activity, R.style.MyAlertDialogStyle);
//        final AlertDialog alert = builderSingle.create();
//
//        LayoutInflater li = LayoutInflater.from(activity);
//        View renamePlaylistView = li.inflate(R.layout.rename_playlist_dialog, null);
//
//        final EditText playlistTitleInput = (EditText) renamePlaylistView.findViewById(R.id.renamePlaylistTitleInput);
//        final Button btnRename = (Button) renamePlaylistView.findViewById(R.id.btnRenamePlaylist);
//        final Button btnCancel = (Button) renamePlaylistView.findViewById(R.id.btnCancelRenamePlaylist);
//
//        playlistTitleInput.getBackground().setColorFilter(Color.parseColor("#093258"), PorterDuff.Mode.SRC_ATOP);
//
//        playlistTitleInput.setSingleLine();
//        playlistTitleInput.setText(playlistTITLE, TextView.BufferType.EDITABLE);
//        playlistTitleInput.setSelection(playlistTitleInput.getText().length());
//
//        alert.setView(renamePlaylistView);
//
//        btnRename.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//
//                final String newPlaylistTITLE = playlistTitleInput.getText().toString();
//
//                if (!newPlaylistTITLE.isEmpty())
//                {
//                    Uri uri = MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI;
//                    ContentResolver resolver = activity.getContentResolver();
//                    ContentValues values = new ContentValues();
//                    String where = MediaStore.Audio.Playlists._ID + " =? ";
//                    String[] whereVal = { Long.toString(playlistID) };
//                    values.put(MediaStore.Audio.Playlists.NAME, newPlaylistTITLE);
//                    resolver.update(uri, values, where, whereVal);
//
//                    pList.clear();
//                    getPlaylists(activity, pList);
//                    adapter.notifyDataSetChanged();
//
//                    Toast.makeText(activity.getApplicationContext(), "Renamed Playlist", Toast.LENGTH_SHORT).show();
//                    alert.dismiss();
//                }
//                else if (newPlaylistTITLE.isEmpty())
//                {
//                    Toast.makeText(activity.getApplicationContext(), "Fields can not be blank", Toast.LENGTH_SHORT).show();
//                }
//
//            }
//        });
//
//        btnCancel.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//
//                alert.dismiss();
//
//            }
//        });
//
//        alert.show();
//    }
//
//    public void DeletePlaylist(final Activity activity, final long playlistID, final ArrayList<Playlist> pList, final PlaylistAdapter adapter)
//    {
//        AlertDialog.Builder builderInner = new AlertDialog.Builder(activity, R.style.MyAlertDialogStyle);
//        builderInner.setTitle("Stealth Music");
//        builderInner.setMessage("Are you sure you want to delete this playlist?");
//        builderInner.setPositiveButton(
//                "Delete",
//                new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(
//                            DialogInterface dialog,
//                            int which) {
//
//                        Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
//                                playlistID);
//                        String selection = MediaStore.Audio.Playlists._ID+ "=?";
//                        String audioId = Long.toString(playlistID);
//                        String[] selectionArgs = {audioId};
//
//                        activity.getContentResolver().delete(uri, selection, selectionArgs);
//
//                        pList.clear();
//
//                        Toast.makeText(activity.getApplicationContext(), "Deleted Playlist", Toast.LENGTH_SHORT).show();
//
//                    }
//                });
//        builderInner.setNegativeButton("Cancel", null);
//        builderInner.show();
//    }

}
