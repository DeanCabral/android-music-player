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

public class AllSongsAdapter extends RecyclerView.Adapter<AllSongsAdapter.MyViewHolder> {

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
        public TextView title, artist, duration;
        public ImageView thumbnail;

        public MyViewHolder(View view) {
            super(view);
            card = view;
            title = view.findViewById(R.id.song_title);
            artist = view.findViewById(R.id.song_artist);
            duration = view.findViewById(R.id.song_duration);
            thumbnail = view.findViewById(R.id.album_art);
        }
    }


    public AllSongsAdapter(List<LocalTrack> songList, Context mContext, RecyclerViewClickListener itemListener) {
        this.ctx = mContext;
        this.songList = songList;
        this.itemListener = itemListener;
    }

    public void updateItem(int position, String title, String artist, String album)
    {
        final LocalTrack lt = songList.get(position);
        lt.setTitle(title);
        lt.setArtist(artist);
        lt.setAlbum(album);
        notifyItemChanged(position);
    }

    public void removeItem(int position)
    {
        songList.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, getItemCount());
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.all_songs_card, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final MyViewHolder holder, final int position) {
        final LocalTrack lt = songList.get(position);
        viewHolder = holder;
        viewHolder.title.setText(lt.getTitle());
        viewHolder.artist.setText(lt.getArtist());
        viewHolder.duration.setText(String.valueOf(lt.getDuration()));

        Glide.with(ctx).load(R.drawable.music_bx).into(holder.thumbnail);
        holder.card.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {

                itemListener.ItemClicked(position);

            }
        });

        holder.card.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {

                itemListener.ItemLongClicked(position);
                return true;
            }
        });
    }

    @Override
    public int getItemCount() {
        return songList.size();
    }
}

