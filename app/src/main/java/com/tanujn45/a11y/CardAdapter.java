package com.tanujn45.a11y;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CardAdapter extends RecyclerView.Adapter<CardAdapter.CardViewHolder> {
    private List<CardData> cardDataList;

    public static class CardViewHolder extends RecyclerView.ViewHolder {
        public TextView nameTextView;
        public TextView textToSpeakTextView;
        public TextView descriptionTextView;
        public TextView numOfGesturesTextView;

        public CardViewHolder(View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            textToSpeakTextView = itemView.findViewById(R.id.textToSpeakTextView);
            descriptionTextView = itemView.findViewById(R.id.descriptionTextView);
            numOfGesturesTextView = itemView.findViewById(R.id.numOfGesturesTextView);
        }
    }

    public CardAdapter(List<CardData> cardDataList) {
        this.cardDataList = cardDataList;
    }

    @Override
    public CardViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_view_item, parent, false);
        return new CardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(CardViewHolder holder, int position) {
        CardData cardData = cardDataList.get(position);
        holder.nameTextView.setText(cardData.getName());
        holder.textToSpeakTextView.setText(cardData.getTextToSpeak());
        holder.descriptionTextView.setText(cardData.getDescription());
        holder.numOfGesturesTextView.setText(String.valueOf(cardData.getNumOfGestures()));
    }

    @Override
    public int getItemCount() {
        return cardDataList.size();
    }
}
