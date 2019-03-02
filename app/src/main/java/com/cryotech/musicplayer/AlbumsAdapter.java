package com.cryotech.musicplayer;

import android.content.ContentUris;
import android.content.Context;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.List;

public class AlbumsAdapter extends RecyclerView.Adapter<AlbumsAdapter.MyViewHolder> {

    private static RecyclerViewClickListener itemListener;
    private List<LocalTrack> albumList;
    private Context ctx;

    public interface RecyclerViewClickListener
    {
        void albumItemClicked(String album);
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
            count = view.findViewById(R.id.count);
            thumbnail = view.findViewById(R.id.thumbnail);
            overflow = view.findViewById(R.id.overflow);
        }
    }


    public AlbumsAdapter(List<LocalTrack> albumList, Context ctx, RecyclerViewClickListener itemListener) {
        this.ctx = ctx;
        this.albumList = albumList;
        this.itemListener = itemListener;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.parent_card, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final MyViewHolder holder, final int position) {
        final LocalTrack lt = albumList.get(position);
        holder.title.setText(lt.getAlbum());
        holder.count.setText(lt.getSongs() + " song(s)");

        if (Preferences.DISPLAY_ALBUM_ART)
        {
            Glide.with(ctx).load(getAlbumArt(albumList.get(position).getId()))
                    .skipMemoryCache(true)
                    .into(holder.thumbnail);
        }

        holder.thumbnail.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                itemListener.albumItemClicked(lt.getAlbum());
            }
        });

        holder.overflow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                itemListener.ItemLongClicked(position);
            }
        });
    }

    public Uri getAlbumArt(long album_id)
    {
        final Uri sArtworkUri = Uri
                .parse("content://media/external/audio/albumart");

        return ContentUris.withAppendedId(sArtworkUri, album_id);
    }

    @Override
    public int getItemCount() {
        return albumList.size();
    }
}
