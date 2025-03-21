package com.example.ilocanospeech_to_texttranslatorapp.adapter;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ilocanospeech_to_texttranslatorapp.R;
import com.example.ilocanospeech_to_texttranslatorapp.model.RecyclerModel;

import java.util.List;

public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.ViewHolder> {

    Context context;

    List<RecyclerModel> recyclerModels;

    public RecyclerAdapter(Context context, List<RecyclerModel> recyclerModels){
        this.context = context;
        this.recyclerModels = recyclerModels;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);

        View view = inflater.inflate(R.layout.items_recycler, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.timestamp_id.setText(recyclerModels.get(position).getTimestamp());
        holder.english_text_id.setText(recyclerModels.get(position).getEnglish_text());
        holder.ilocano_text_id.setText(recyclerModels.get(position).getIlocano_text());
    }

    @Override
    public int getItemCount() {
        return recyclerModels.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{

        private TextView timestamp_id, english_text_id, ilocano_text_id;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            timestamp_id = itemView.findViewById(R.id.timestamp_id);
            english_text_id = itemView.findViewById(R.id.english_text_id);
            ilocano_text_id = itemView.findViewById(R.id.ilocano_text_id);

        }
    }

}
