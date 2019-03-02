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

public class ArtistsHorizontalAdapter extends RecyclerView.Adapter<ArtistsHorizontalAdapter.MyViewHolder> {

    private static RecyclerViewClickListener itemListener;
    private List<LocalTrack> artistList;
    private Context ctx;

    public interface RecyclerViewClickListener
    {
        void artistItemClicked(String artist);
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        View card;
        ImageView art;
        TextView title, info;
        RelativeLayout bottomHolder;

        public MyViewHolder(View view) {
            super(view);
            card = view;
            art = view.findViewById(R.id.backImage);
            title = view.findViewById(R.id.card_title);
            info = view.findViewById(R.id.card_info);
            bottomHolder = view.findViewById(R.id.bottomHolder);

        }
    }

    public ArtistsHorizontalAdapter(List<LocalTrack> artistsHome, Context ctx, RecyclerViewClickListener itemListener) {
        this.artistList = artistsHome;
        this.ctx = ctx;
        this.itemListener = itemListener;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.parent_card_layout, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, final int position) {

        final LocalTrack lt = artistList.get(position);
        holder.title.setText(lt.getArtist());
        holder.info.setText(lt.getSongs() + " song(s)");

        holder.card.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                itemListener.artistItemClicked(lt.getArtist());
            }
        });
    }

    @Override
    public int getItemCount() {
        return artistList.size();
    }
}

