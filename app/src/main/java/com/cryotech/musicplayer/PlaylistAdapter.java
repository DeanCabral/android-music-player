package com.cryotech.musicplayer;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.List;

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.MyViewHolder> {

    private static RecyclerViewClickListener itemListener;
    private List<LocalTrack> songList;
    private Context ctx;
    private MyViewHolder viewHolder;

    public interface RecyclerViewClickListener
    {
        void ItemClicked(int position);
        void ItemLongClicked(int position);
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        public View card;
        public TextView title, label, count;
        public ImageView thumbnail, overflow;

        public MyViewHolder(View view) {
            super(view);
            card = view;
            title = view.findViewById(R.id.playlist_title);
            label = view.findViewById(R.id.playlist_label);
            thumbnail = view.findViewById(R.id.thumbnail);
            overflow = view.findViewById(R.id.overflow);
        }
    }


    public PlaylistAdapter(List<LocalTrack> songList, Context mContext, RecyclerViewClickListener itemListener) {
        this.ctx = mContext;
        this.songList = songList;
        this.itemListener = itemListener;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.playlists_card, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final MyViewHolder holder, final int position) {
        final LocalTrack lt = songList.get(position);
        viewHolder = holder;
        viewHolder.title.setText(lt.getTitle());
        viewHolder.label.setText("Custom Playlist");

        Glide.with(ctx).load(R.drawable.music_bx).into(holder.thumbnail);
        holder.thumbnail.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {

                itemListener.ItemClicked(position);

            }
        });

        holder.overflow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                itemListener.ItemLongClicked(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return songList.size();
    }
}