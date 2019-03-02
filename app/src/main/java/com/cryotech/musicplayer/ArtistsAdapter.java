package com.cryotech.musicplayer;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class ArtistsAdapter extends RecyclerView.Adapter<ArtistsAdapter.MyViewHolder> {

    private static RecyclerViewClickListener itemListener;
    private List<LocalTrack> artistList;
    private Context ctx;

    public interface RecyclerViewClickListener
    {
        void artistItemClicked(String artist);
        void ItemLongClicked(int position);
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        public View card;
        public TextView title, count;
        public ImageView thumbnail, overflow;

        public MyViewHolder(View view) {
            super(view);
            card = view;
            title = view.findViewById(R.id.title);
            count =  view.findViewById(R.id.count);
            thumbnail = view.findViewById(R.id.thumbnail);
            overflow = view.findViewById(R.id.overflow);
        }
    }


    public ArtistsAdapter(List<LocalTrack> artistList, Context ctx, RecyclerViewClickListener itemListener) {
        this.ctx = ctx;
        this.artistList = artistList;
        this.itemListener = itemListener;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.parent_card, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final MyViewHolder holder, final int position) {

        final LocalTrack lt = artistList.get(position);
        holder.title.setText(lt.getArtist());
        holder.count.setText(lt.getSongs() + " song(s)");

        holder.thumbnail.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                itemListener.artistItemClicked(lt.getArtist());
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
        return artistList.size();
    }

}
