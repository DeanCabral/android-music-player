package com.cryotech.musicplayer;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;


import java.util.List;

public class AllSongsHorizontalAdapter extends RecyclerView.Adapter<AllSongsHorizontalAdapter.MyViewHolder> {

    private static RecyclerViewClickListener itemListener;
    private List<LocalTrack> allSongs;
    private Context ctx;

    public interface RecyclerViewClickListener
    {
        void allSongItemClicked(int position);
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        View card;
        ImageView art;
        TextView title, artist;
        RelativeLayout bottomHolder;

        public MyViewHolder(View view) {
            super(view);
            card = view;
            art = view.findViewById(R.id.backImage);
            title = view.findViewById(R.id.card_title);
            artist = view.findViewById(R.id.card_artist);
            bottomHolder = view.findViewById(R.id.bottomHolder);
        }

    }

    public AllSongsHorizontalAdapter(List<LocalTrack> artistsHome, Context ctx, RecyclerViewClickListener itemListener) {
        this.allSongs = artistsHome;
        this.ctx = ctx;
        this.itemListener = itemListener;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_card_layout, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, final int position) {

        final LocalTrack lt = allSongs.get(position);
        holder.title.setText(lt.getTitle());
        holder.artist.setText(lt.getArtist());

        holder.card.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {

                itemListener.allSongItemClicked(position);
            }
        });

    }

    @Override
    public int getItemCount() {
        return allSongs.size();
    }
}
