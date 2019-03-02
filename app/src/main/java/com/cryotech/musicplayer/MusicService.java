package com.cryotech.musicplayer;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.NotificationTarget;

import java.util.ArrayList;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class MusicService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener, AudioManager.OnAudioFocusChangeListener {

    private final IBinder musicBind = new MusicBinder();
    private LocalBroadcastManager broadcaster;
    private AudioManager audioManager;
    public static final String RESULT = "com.cryotech.musicplayer.Activities.MusicService.REQUEST_PROCESSED";
    public static final String MSG = "com.cryotech.musicplayer.Activities.MusicService.MESSAGE";
    public MediaPlayer mediaPlayer;
    private float volume = 1;
    public ArrayList<LocalTrack> recentSongs;
    public ArrayList<LocalTrack> songList;
    private ArrayList<LocalTrack> originalList;
    private ArrayList<String> referenceList;
    private String currentSongTitle;
    private String currentSongArtist;
    private int upcomingSongIndex = -1;
    private int currentSongIndex = -1;
    private long currentSongDuration;
    private int recentSize = 5;
    private int seekForwardTime = 5000;
    private int seekBackwardTime = 5000;
    private boolean eqEnabled;
    private boolean isShuffle = false;
    private boolean isRepeat = false;
    private boolean playlistEnabled = false;
    private Notification status;
    private NotificationTarget notificationTarget;
    private PendingIntent pendingIntent;
    private RemoteViews views;
    private RemoteViews bigViews;

    public String getCurrentSongTitle() {
        return currentSongTitle;
    }

    public String getCurrentSongArtist() {
        return currentSongArtist;
    }

    public long getCurrentSongDuration() {
        return currentSongDuration;
    }

    public int getUpcomingSongIndex() { return upcomingSongIndex; }

    public int getCurrentSongIndex() {
        return currentSongIndex;
    }

    public int getRecentSize() {
        return recentSize;
    }

    public boolean getEqEnabled() { return eqEnabled; }

    public boolean getRepeatState() {return isRepeat;}

    public boolean getShuffleState() {return isShuffle;}

    public void setList(ArrayList<LocalTrack> theSongs){
        songList=theSongs;
    }

    public void setRecentSize(int size) {recentSize = size; }

    public void setSongIndex(int songIndex){
        currentSongIndex=songIndex;
    }

    public void setUpcomingSongIndex(int songIndex){
        upcomingSongIndex=songIndex;
    }

    public void setSong(String songTitle, String songArtist) {currentSongTitle = songTitle; currentSongArtist = songArtist;}

    public void setDuration (long duration) {currentSongDuration = duration;}

    public void setEqEnabled(boolean enabled) {eqEnabled = enabled;}

    public void setRepeat(boolean state)
    {
        isRepeat = state;
    }

    public void setShuffle(boolean state)
    {
        isShuffle = state;
    }

    public void sendResult(String message) {
        Intent intent = new Intent(RESULT);
        if(message != null)
            intent.putExtra(MSG, message);
        broadcaster.sendBroadcast(intent);
    }

    public void onCreate(){

        super.onCreate();
        mediaPlayer = new MediaPlayer();
        broadcaster = LocalBroadcastManager.getInstance(this);
        recentSongs = new ArrayList<>();
        referenceList = new ArrayList<>();
        originalList = new ArrayList<>();

        audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

        initOriginalList();
        initRefList();
        initMusicPlayer();
    }

    private void initMusicPlayer(){

        mediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnErrorListener(this);
    }

    public void initOriginalList()
    {
        if (originalList.size() != 0)
        {
            originalList.clear();
        }
        String duration;
        ContentResolver musicResolver = getContentResolver();
        String selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0";
        Uri musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";
        Cursor musicCursor = musicResolver.query(musicUri, null, selection, null, sortOrder);

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
                    originalList.add(new LocalTrack(thisId, thisTitle, thisArtist, thisAlbum, thisPath, duration, -1));
                }
            }
            while (musicCursor.moveToNext());
        }
        musicCursor.close();
    }

    public void initRefList()
    {
        if (referenceList.size() != 0)
        {
            referenceList.clear();
        }
        ContentResolver musicResolver = getContentResolver();
        String selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0";
        Uri musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";
        Cursor musicCursor = musicResolver.query(musicUri, null, selection, null, sortOrder);

        if(musicCursor!=null && musicCursor.moveToFirst()){

            int titleColumn = musicCursor.getColumnIndex
                    (MediaStore.Audio.Media.TITLE);
            do {
                String thisTitle = musicCursor.getString(titleColumn);
                referenceList.add(thisTitle);
            }
            while (musicCursor.moveToNext());
        }

        musicCursor.close();
    }

    public void playSong(){

        Intent serviceIntent = new Intent(this, MusicService.class);
        serviceIntent.setAction(Constants.ACTION.STARTFOREGROUND_ACTION);
        startService(serviceIntent);

        audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        if (songList.size() != 0)
        {

            setSong(songList.get(currentSongIndex).getTitle(), songList.get(currentSongIndex).getArtist());
            updateLabels();

            mediaPlayer.reset();

            if (currentSongIndex != -1)
            {
                LocalTrack playSong = songList.get(currentSongIndex);
                long currSong = playSong.getId();
                Uri trackUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        currSong);

                if (recentSongs.size() < recentSize)
                {
                    recentSongs.add(0, playSong);
                }
                else
                {
                    recentSongs.remove(recentSongs.size() - 1);
                    recentSongs.add(0, playSong);
                }

                try{
                    mediaPlayer.setDataSource(getApplicationContext(), trackUri);
                }
                catch(Exception e){
                    Log.e("MUSIC SERVICE", "Error setting data source", e);
                }

                if (Preferences.FADE_EFFECT)
                {
                    volume = 0;
                    startFadeIn();
                    try
                    {
                        Thread.sleep(200);
                    }
                    catch(InterruptedException e){
                        e.printStackTrace();
                    }

                }
                mediaPlayer.prepareAsync();
            }
        }
    }

    public void pauseSong()
    {
        if (mediaPlayer != null)
        {
            if(mediaPlayer.isPlaying()){
                mediaPlayer.pause();
                pausedNotifUI();
            }
            else {
                mediaPlayer.start();
                playingNotifUI();
            }
        }

        buildNotification();
        sendResult("NOTIF_STATE_CHANGE");
    }

    public void rewindSong()
    {
        int currentPosition = mediaPlayer.getCurrentPosition();
        if(currentPosition - seekBackwardTime >= 0){
            mediaPlayer.seekTo(currentPosition - seekBackwardTime);
        }else{
            mediaPlayer.seekTo(0);
        }
    }

    public void fastforwardSong()
    {
        int currentPosition = mediaPlayer.getCurrentPosition();
        if(currentPosition + seekForwardTime <= mediaPlayer.getDuration()){
            mediaPlayer.seekTo(currentPosition + seekForwardTime);
        }else{
            mediaPlayer.seekTo(mediaPlayer.getDuration());
        }
    }

    public void playNext()
    {
        if (!Preferences.PLAYLIST)
        {
            currentSongIndex = originalIndex();
            setList(originalList);
        }

        if (isShuffle)
        {
            Random rand = new Random();
            currentSongIndex = rand.nextInt((songList.size() - 1) + 1);
            playSong();
        }
        else if(currentSongIndex < (songList.size() - 1)){
            currentSongIndex += 1;
            playSong();

        }else{

            currentSongIndex = 0;
            playSong();
        }

        sendResult("SONG_COMPLETE");
    }

    public void playPrevious()
    {
        if (!Preferences.PLAYLIST)
        {
            currentSongIndex = originalIndex();
            setList(originalList);
        }

        if (isShuffle)
        {
            Random rand = new Random();
            currentSongIndex = rand.nextInt((songList.size() - 1) + 1);
            playSong();
        }
        else if(currentSongIndex > 0){
            currentSongIndex -= 1;
            playSong();
        }else{
            currentSongIndex = songList.size() - 1;
            playSong();
        }

        sendResult("SONG_COMPLETE");
    }

    private void updateLabels()
    {
        Intent intent = new Intent();
        sendBroadcast(intent);
    }

    private void startFadeIn(){
        final int FADE_DURATION = 4000;
        final int FADE_INTERVAL = 250;
        final int MAX_VOLUME = 1;
        int numberOfSteps = FADE_DURATION/FADE_INTERVAL;
        final float deltaVolume = MAX_VOLUME / (float)numberOfSteps;

        final Timer timer = new Timer(true);
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                fadeInStep(deltaVolume);
                if(volume>=1f){
                    timer.cancel();
                    timer.purge();
                }
            }
        };

        timer.schedule(timerTask,FADE_INTERVAL,FADE_INTERVAL);
    }

    private void fadeInStep(float deltaVolume){
        mediaPlayer.setVolume(volume, volume);
        volume += deltaVolume;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent.getAction() != null) {

            if (intent.getAction().equals(Constants.ACTION.STARTFOREGROUND_ACTION)) {
                showNotification();

            } else if (intent.getAction().equals(Constants.ACTION.PREV_ACTION)) {
                playPrevious();
                buildNotification();

            } else if (intent.getAction().equals(Constants.ACTION.PLAY_ACTION)) {
                pauseSong();

            } else if (intent.getAction().equals(Constants.ACTION.NEXT_ACTION)) {
                playNext();
                buildNotification();

            } else if (intent.getAction().equals(
                    Constants.ACTION.STOPFOREGROUND_ACTION)) {

                stopForeground(true);
                stopSelf();
            }
        }

        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return musicBind;
    }

    @Override
    public boolean onUnbind(Intent intent){
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        setDuration(mediaPlayer.getDuration());
        mp.start();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {

        if (!Preferences.PLAYLIST)
        {
            currentSongIndex = originalIndex();
            setList(originalList);
        }

        if (upcomingSongIndex != -1)
        {
            currentSongIndex = upcomingSongIndex;
            upcomingSongIndex = -1;
            playSong();
        }
        else if(isShuffle)
        {
            Random rand = new Random();
            currentSongIndex = rand.nextInt((songList.size() - 1) + 1);
            playSong();
        }
        else if(isRepeat)
        {
            playSong();
        }
        else
        {
            playNext();
        }

        sendResult("SONG_COMPLETE");
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        return false;
    }

    private int originalIndex()
    {
        return referenceList.indexOf(currentSongTitle);
    }

    public void showNotification() {

        if (currentSongIndex != -1)
        {
            if (views == null && bigViews == null)
            {
                views = new RemoteViews(getPackageName(),
                        R.layout.custom_notification);
                bigViews = new RemoteViews(getPackageName(),
                        R.layout.custom_notification_expanded);

                views.setViewVisibility(R.id.status_bar_album_art, View.GONE);

                Intent notificationIntent = new Intent(this, HomeActivity.class);
                notificationIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                pendingIntent = PendingIntent.getActivity(this, 0,
                        notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);

                Intent closeIntent = new Intent(this, MusicService.class);
                closeIntent.setAction(Constants.ACTION.STOPFOREGROUND_ACTION);
                PendingIntent pcloseIntent = PendingIntent.getService(this, 0,
                        closeIntent, 0);

                Intent previousIntent = new Intent(this, MusicService.class);
                previousIntent.setAction(Constants.ACTION.PREV_ACTION);
                PendingIntent ppreviousIntent = PendingIntent.getService(getApplicationContext(), 0,
                        previousIntent, 0);

                Intent playIntent = new Intent(this, MusicService.class);
                playIntent.setAction(Constants.ACTION.PLAY_ACTION);
                PendingIntent pplayIntent = PendingIntent.getService(getApplicationContext(), 0,
                        playIntent, 0);

                Intent nextIntent = new Intent(this, MusicService.class);
                nextIntent.setAction(Constants.ACTION.NEXT_ACTION);
                PendingIntent pnextIntent = PendingIntent.getService(getApplicationContext(), 0,
                        nextIntent, 0);

                views.setOnClickPendingIntent(R.id.status_bar_collapse, pcloseIntent);
                bigViews.setOnClickPendingIntent(R.id.status_bar_collapse, pcloseIntent);

                views.setOnClickPendingIntent(R.id.status_bar_prev, ppreviousIntent);
                bigViews.setOnClickPendingIntent(R.id.status_bar_prev, ppreviousIntent);

                views.setOnClickPendingIntent(R.id.status_bar_play, pplayIntent);
                bigViews.setOnClickPendingIntent(R.id.status_bar_play, pplayIntent);

                views.setOnClickPendingIntent(R.id.status_bar_next, pnextIntent);
                bigViews.setOnClickPendingIntent(R.id.status_bar_next, pnextIntent);
            }

            views.setTextViewText(R.id.status_bar_song_name, getCurrentSongTitle());
            bigViews.setTextViewText(R.id.status_bar_song_name, getCurrentSongTitle());

            views.setTextViewText(R.id.status_bar_artist_name, getCurrentSongArtist());
            bigViews.setTextViewText(R.id.status_bar_artist_name, getCurrentSongArtist());

            bigViews.setTextViewText(R.id.status_bar_app_name, "Cryotech Music");

            playingNotifUI();
            buildNotification();
        }
    }


    private void buildNotification()
    {
        if (Preferences.NOTIF_DISPLAY)
        {
            if (status == null)
            {
                status = new Notification.Builder(getApplicationContext())
                        .setSmallIcon(R.drawable.ic_music)
                        .build();

                status.contentView = views;
                status.bigContentView = bigViews;
                status.flags = Notification.FLAG_ONGOING_EVENT;
                status.contentIntent = pendingIntent;

                notificationTarget = new NotificationTarget(
                        this,
                        bigViews,
                        R.id.status_bar_album_art,
                        status,
                        101);
            }

            Glide.with(getApplicationContext()).load(R.drawable.music_bx).asBitmap().into(notificationTarget);
            startForeground(101, status);
        }
    }

    public void pausedNotifUI()
    {
        views.setImageViewResource(R.id.status_bar_play, android.R.drawable.ic_media_play);
        bigViews.setImageViewResource(R.id.status_bar_play, android.R.drawable.ic_media_play);
    }

    public void playingNotifUI()
    {
        views.setImageViewResource(R.id.status_bar_play, android.R.drawable.ic_media_pause);
        bigViews.setImageViewResource(R.id.status_bar_play, android.R.drawable.ic_media_pause);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        audioManager.abandonAudioFocus(this);
        if (mediaPlayer.isPlaying())
        {
            mediaPlayer.stop();
        }
        mediaPlayer.release();
        stopSelf();
    }

    @Override
    public void onAudioFocusChange(int focusChange) {

        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                pauseSong();
                mediaPlayer.setVolume(1.0f, 1.0f);
                break;

            case AudioManager.AUDIOFOCUS_LOSS:
                pauseSong();
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                pauseSong();
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                pauseSong();
                break;
        }
    }

    public final SensorEventListener mSensorListener = new SensorEventListener() {

        private float mAccel;
        private float mAccelCurrent;
        private float mAccelLast;

        public void onSensorChanged(SensorEvent se) {
            float x = se.values[0];
            float y = se.values[1];
            float z = se.values[2];
            mAccelLast = mAccelCurrent;
            mAccelCurrent = (float) Math.sqrt((double) (x * x + y * y + z * z));
            float delta = mAccelCurrent - mAccelLast;
            mAccel = mAccel * 0.9f + delta;

            if (Preferences.SHAKE_SKIP) {
                if (mAccel > 12) {
                    playNext();
                    Toast.makeText(getApplicationContext(), "Skipped Song", Toast.LENGTH_SHORT).show();
                }
            }
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    public class MusicBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }
    }
}
