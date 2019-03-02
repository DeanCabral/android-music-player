package com.cryotech.musicplayer;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class RecentsActivity extends AppCompatActivity {

    private TabLayout tabLayout;
    private ViewPager viewPager;
    public ViewPagerAdapter adapter;

    private RecentlyPlayedFragment rpFragment;
    private RecentlyAddedFragment raFragment;

    public MusicService musicSrv;
    private Intent playIntent;
    private boolean musicBound = false;
    public int selectedSongPos = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recents);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle("Recent Songs");

        viewPager = findViewById(R.id.viewpager);
        tabLayout = findViewById(R.id.tabs);

        rpFragment = new RecentlyPlayedFragment();
        raFragment = new RecentlyAddedFragment();

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    public void saveRecents(long[] songIDs)
    {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < songIDs.length; i++) {
            str.append(songIDs[i]).append(",");
        }
        sharedPreferences.edit().putString("idstring", str.toString()).apply();
    }

    public long[] loadRecents()
    {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String savedString = sharedPreferences.getString("idstring", "");
        StringTokenizer st = new StringTokenizer(savedString, ",");
        long[] savedList = new long[15];
        for (int i = 0; i < 10; i++) {

            if (st.hasMoreTokens())
            {
                savedList[i] = Integer.parseInt(st.nextToken());
            }
        }
        return savedList;
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
    private void setupViewPager(ViewPager viewPager) {
        adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(rpFragment, "Recently Played");
        adapter.addFragment(raFragment, "Recently Added");
        viewPager.setAdapter(adapter);
        rpFragment.initAdapter();
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            public void onPageScrollStateChanged(int state) {}
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

            public void onPageSelected(int position) {
                if (position == 0)
                {
                    rpFragment.initAdapter();
                    rpFragment.initRecentlyPlayed();
                }
                else
                {
                    raFragment.initAdapter();
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
            setupViewPager(viewPager);
            tabLayout.setupWithViewPager(viewPager);
            viewPager.setOffscreenPageLimit(2);
            rpFragment.initRecentlyPlayed();
            rpFragment.initAdapter();
            musicBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicBound = false;
        }
    };

    class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        public ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }
    }
}