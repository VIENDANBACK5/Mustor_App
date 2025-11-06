package com.example.musicplayer.knowledge;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicplayer.R;

import java.util.ArrayList;

public class MusicKnowledgeAdapter extends RecyclerView.Adapter<MusicKnowledgeAdapter.ViewHolder> {

    private Context context;
    private ArrayList<MusicKnowledge> knowledgeList;

    public MusicKnowledgeAdapter(Context context, ArrayList<MusicKnowledge> knowledgeList) {
        this.context = context;
        this.knowledgeList = knowledgeList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_music_knowledge, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MusicKnowledge knowledge = knowledgeList.get(position);

        holder.tvTitle.setText(knowledge.getTitle());
        holder.tvContent.setText(knowledge.getContent());
        holder.tvCategory.setText(knowledge.getCategory());
        holder.tvDate.setText(knowledge.getDate());
        holder.tvViews.setText(knowledge.getViews() + " lượt xem");

        // Màu sắc cho các category khác nhau
        int categoryColor = getCategoryColor(knowledge.getCategory());
        holder.tvCategory.setBackgroundColor(categoryColor);

        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(context, "Mở: " + knowledge.getTitle(), Toast.LENGTH_SHORT).show();
                // TODO: Mở activity chi tiết
            }
        });
    }

    @Override
    public int getItemCount() {
        return knowledgeList.size();
    }

    private int getCategoryColor(String category) {
        switch (category) {
            case "Lịch sử":
                return 0xFF9B4DCA; // Purple
            case "Kỹ thuật":
                return 0xFF2196F3; // Blue
            case "Lý thuyết":
                return 0xFF4CAF50; // Green
            case "Văn hóa":
                return 0xFFFF9800; // Orange
            case "Sản xuất":
                return 0xFFF44336; // Red
            case "Thể loại":
                return 0xFF00BCD4; // Cyan
            default:
                return 0xFF9E9E9E; // Grey
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView tvTitle, tvContent, tvCategory, tvDate, tvViews;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvContent = itemView.findViewById(R.id.tvContent);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvViews = itemView.findViewById(R.id.tvViews);
        }
    }
}