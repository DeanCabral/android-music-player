package com.cryotech.musicplayer;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

public class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.MyViewHolder> {

    private static RecyclerViewClickListener itemListener;
    private ArrayList<LocalTrack> songList;
    private ArrayList<LocalTrack> originalList;
    private TextView resultCount;
    private Context ctx;
    public int filterPref = 0;

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


    public SearchAdapter(ArrayList<LocalTrack> songList, TextView resultCount, Context mContext, RecyclerViewClickListener itemListener) {
        this.ctx = mContext;
        this.resultCount = resultCount;
        this.songList = songList;
        this.itemListener = itemListener;
    }

    public Filter getFilter() {
        return new Filter() {

            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                final FilterResults oReturn = new FilterResults();
                final ArrayList<LocalTrack> results = new ArrayList<>();
                if (originalList == null)
                    originalList = songList;
                if (constraint != null) {
                    if (originalList != null && originalList.size() > 0) {
                        for (final LocalTrack g : originalList) {

                            switch (filterPref) {
                                case 0:
                                    String lowerTitle = g.getTitle().toLowerCase();
                                    if (lowerTitle
                                            .contains(constraint.toString().toLowerCase()))
                                        results.add(g);
                                    break;
                                case 1:
                                    String lowerArtist = g.getArtist().toLowerCase();
                                    if (lowerArtist
                                            .contains(constraint.toString().toLowerCase()))
                                        results.add(g);
                                    break;
                                case 2:
                                    String lowerAlbum = g.getAlbum().toLowerCase();
                                    if (lowerAlbum
                                            .contains(constraint.toString().toLowerCase()))
                                        results.add(g);
                                    break;
                            }
                        }
                    }
                    oReturn.values = results;
                }
                return oReturn;
            }

            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(CharSequence constraint,
                                          FilterResults results) {
                songList = (ArrayList<LocalTrack>) results.values;
                resultCount.setText("Displaying " + songList.size() + " Result(s)");
                notifyDataSetChanged();
            }
        };
    }

    public ArrayList<LocalTrack> getList() { return songList; }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.all_songs_card, parent, false);
        return new MyViewHolder(itemView);
    }


    @Override
    public void onBindViewHolder(final MyViewHolder holder, final int position) {
        final LocalTrack lt = songList.get(position);
        holder.title.setText(lt.getTitle());
        holder.artist.setText(lt.getArtist());
        holder.duration.setText(String.valueOf(lt.getDuration()));

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
