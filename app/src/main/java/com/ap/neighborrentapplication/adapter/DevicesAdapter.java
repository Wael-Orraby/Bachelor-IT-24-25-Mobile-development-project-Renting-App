package com.ap.neighborrentapplication.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ap.neighborrentapplication.R;
import com.ap.neighborrentapplication.models.Device;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.GranularRoundedCorners;

import java.util.ArrayList;

public class DevicesAdapter extends RecyclerView.Adapter<DevicesAdapter.ViewHolder> {

    private ArrayList<Device> devices;
    private Context context;

    // Constructor die de devices en context ontvangt
    public DevicesAdapter(ArrayList<Device> devices, Context context) {
        this.devices = devices;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.viewholder_device_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Device device = devices.get(position);

        // Gegevens instellen voor elke view
        holder.title.setText(device.getName());
        holder.subtitle.setText(device.getDescription());
        holder.price.setText("€" + device.getPricePerDay() + " per dag");
        holder.city.setText(device.getPostalCode()+" "+device.getCity());
        holder.status.setText(device.getAvailable() ? "Beschikbaar" : "Niet beschikbaar");

        // Laad de afbeelding met Glide en gebruik afgeronde hoeken
        Glide.with(context)
                .load(device.getImageUrl())
                .transform(new GranularRoundedCorners(30, 30, 0, 0))
                .into(holder.pic);
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    // ViewHolder Klasse voor referentie naar de UI elementen
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, subtitle, price, city, status;
        ImageView pic, favoriteIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            // Initialiseer de views
            title = itemView.findViewById(R.id.titleTxt);
            subtitle = itemView.findViewById(R.id.descriptionTxt);
            price = itemView.findViewById(R.id.priceTxt);
            city = itemView.findViewById(R.id.cityTxt);
            status = itemView.findViewById(R.id.statusTxt);
            pic = itemView.findViewById(R.id.pic);
            favoriteIcon = itemView.findViewById(R.id.favoriteIcon);
        }
    }

    // Methode om de lijst met apparaten bij te werken
    public void updateDevices(ArrayList<Device> newDevices) {
        this.devices = newDevices;
        notifyDataSetChanged();
    }
}
