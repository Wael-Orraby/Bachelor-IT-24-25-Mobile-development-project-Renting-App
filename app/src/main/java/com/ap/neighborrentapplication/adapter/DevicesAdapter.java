package com.ap.neighborrentapplication.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ap.neighborrentapplication.R;
import com.ap.neighborrentapplication.domain.Device;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.GranularRoundedCorners;

import java.util.ArrayList;

public class DevicesAdapter extends RecyclerView.Adapter<DevicesAdapter.Viewholder> {

    ArrayList<Device> items;
    Context context;

    public DevicesAdapter(ArrayList<Device> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public Viewholder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View inflator = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.viewholder_device_list,
                parent,
                false
        );
        return new Viewholder(inflator);
    }


    @Override
    public void onBindViewHolder(@NonNull Viewholder holder, int position) {
        Device device = items.get(position);

        // Vul de velden in met data uit de entity
        holder.title.setText(device.getName());
        holder.subtitle.setText(device.getDescription());
        holder.price.setText("€" + device.getPrice());
        holder.city.setText(device.getCity());
        holder.status.setText(device.getStatus());

        holder.favoriteIcon.setImageResource(device.isFavorite() ? R.drawable.ic_redheart_outline : R.drawable.ic_heart_outline);

        holder.favoriteIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean isFavorite = device.isFavorite();
                device.setFavorite(!isFavorite); // Toggle de favoriete status

                holder.favoriteIcon.setImageResource(device.isFavorite() ? R.drawable.ic_redheart_outline : R.drawable.ic_heart_outline);
            }
        });
        Glide.with(holder.itemView.getContext())
                .load(device.getPicUrl())
                .transform(new GranularRoundedCorners(30,30,0,0))
                .into(holder.pic);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class Viewholder extends RecyclerView.ViewHolder{
        TextView title, subtitle, price, city, status;
        ImageView pic, favoriteIcon;

        public Viewholder(@NonNull View itemView) {
            super(itemView);


            title = itemView.findViewById(R.id.titleTxt);
            subtitle = itemView.findViewById(R.id.descriptionTxt);
            price = itemView.findViewById(R.id.priceTxt);
            city = itemView.findViewById(R.id.cityTxt);
            status = itemView.findViewById(R.id.statusTxt);
            pic = itemView.findViewById(R.id.pic);
            favoriteIcon = itemView.findViewById(R.id.favoriteIcon);
        }
    }
}