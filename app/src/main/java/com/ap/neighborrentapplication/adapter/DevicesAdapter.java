package com.ap.neighborrentapplication.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ap.neighborrentapplication.R;
import com.ap.neighborrentapplication.models.Device;
import com.ap.neighborrentapplication.ui.activity.DashboardActivity;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.GranularRoundedCorners;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.CollectionReference;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class DevicesAdapter extends RecyclerView.Adapter<DevicesAdapter.ViewHolder> {

    private ArrayList<Device> devices;
    private Context context;
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;

    // Constructor die de devices en context ontvangt
    public DevicesAdapter(ArrayList<Device> devices, Context context) {
        this.devices = devices;
        this.context = context;
        this.firestore = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
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

        // Check if device is in favorites
        checkIfFavorite(device.getId(), holder.favoriteIcon);

        holder.favoriteIcon.setOnClickListener(v -> toggleFavorite(device, holder.favoriteIcon));
    }

    private void checkIfFavorite(String deviceId, ImageView favoriteIcon) {
        String userId = auth.getCurrentUser().getUid();
        firestore.collection("favorites")
                .whereEqualTo("userId", userId)
                .whereEqualTo("deviceId", deviceId)
                .get()
                .addOnSuccessListener(documents -> {
                    boolean isFavorite = !documents.isEmpty();
                    favoriteIcon.setImageResource(isFavorite ? 
                        R.drawable.ic_redheart : R.drawable.ic_heart_outline);
                });
    }

    private void toggleFavorite(Device device, ImageView favoriteIcon) {
        String userId = auth.getCurrentUser().getUid();
        CollectionReference favoritesRef = firestore.collection("favorites");
        
        favoritesRef.whereEqualTo("userId", userId)
                .whereEqualTo("deviceId", device.getId())
                .get()
                .addOnSuccessListener(documents -> {
                    if (documents.isEmpty()) {
                        addToFavorites(userId, device, favoriteIcon);
                    } else {
                        removeFromFavorites(documents.getDocuments().get(0), favoriteIcon);
                    }
                })
                .addOnFailureListener(e -> 
                    Toast.makeText(context, "Fout bij verwerken van favoriet: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show()
                );
    }

    private void addToFavorites(String userId, Device device, ImageView favoriteIcon) {
        Map<String, Object> favorite = new HashMap<>();
        favorite.put("userId", userId);
        favorite.put("deviceId", device.getId());
        favorite.put("timestamp", new Date());

        firestore.collection("favorites").add(favorite)
                .addOnSuccessListener(documentReference -> {
                    favoriteIcon.setImageResource(R.drawable.ic_redheart);
                    Toast.makeText(context, "Toegevoegd aan favorieten", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> 
                    Toast.makeText(context, "Fout bij toevoegen aan favorieten: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show()
                );
    }

    private void removeFromFavorites(DocumentSnapshot document, ImageView favoriteIcon) {
        document.getReference().delete()
                .addOnSuccessListener(aVoid -> {
                    favoriteIcon.setImageResource(R.drawable.ic_heart_outline);
                    Toast.makeText(context, "Verwijderd uit favorieten", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> 
                    Toast.makeText(context, "Fout bij verwijderen uit favorieten: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show()
                );
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
