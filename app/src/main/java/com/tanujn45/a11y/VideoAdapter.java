package com.tanujn45.a11y;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VideoViewHolder> {
    private List<Video> videoList;
    private  OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Video video);
    }

    public VideoAdapter(List<Video> videoList, OnItemClickListener listener) {
        this.videoList = videoList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.video_item, parent, false);
        return new VideoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {
        Video video = videoList.get(position);
        // Bind thumbnail image to ImageView
        holder.thumbnailImageView.setImageBitmap(video.getThumbnail());
        // Bind title to TextView
        holder.titleTextView.setText(video.getTitle());
    }

    @Override
    public int getItemCount() {
        return videoList.size();
    }

    public class VideoViewHolder extends RecyclerView.ViewHolder {
        ImageView thumbnailImageView;
        TextView titleTextView;

        public VideoViewHolder(@NonNull View itemView) {
            super(itemView);
            thumbnailImageView = itemView.findViewById(R.id.image_thumbnail);
            titleTextView = itemView.findViewById(R.id.text_title);

            itemView.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Video video = videoList.get(position);
                    listener.onItemClick(video);
                }
            });
        }
    }
}
