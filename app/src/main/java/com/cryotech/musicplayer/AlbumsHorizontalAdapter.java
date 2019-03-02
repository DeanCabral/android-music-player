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

public class AlbumsHorizontalAdapter extends RecyclerView.Adapter<AlbumsHorizontalAdapter.MyViewHolder> {

    private static RecyclerViewClickListener itemListener;
    private List<LocalTrack> albumList;
    private Context ctx;

    public interface RecyclerViewClickListener
    {
        void albumItemClicked(String album);
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

    public AlbumsHorizontalAdapter(List<LocalTrack> albumsHome, Context ctx, RecyclerViewClickListener itemListener) {
        this.albumList = albumsHome;
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

        final LocalTrack lt = albumList.get(position);
        holder.title.setText(lt.getAlbum());
        holder.info.setText(lt.getSongs() + " song(s)");

        holder.card.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                itemListener.albumItemClicked(lt.getAlbum());
            }
        });
    }

    @Override
    public int getItemCount() {
        return albumList.size();
    }

}
