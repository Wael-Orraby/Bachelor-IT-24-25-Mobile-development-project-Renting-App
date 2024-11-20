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
import com.ap.neighborrentapplication.models.Profile;
import com.bumptech.glide.Glide;

import java.util.ArrayList;

public class ProfileAdapter extends RecyclerView.Adapter<ProfileAdapter.ViewHolder> {

    private ArrayList<Profile> profiles;
    private Context context;

    // Constructor om de lijst met profielen en de context te ontvangen
    public ProfileAdapter(ArrayList<Profile> profiles, Context context) {
        this.profiles = profiles;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.viewholder_profile, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Profile profile = profiles.get(position);

        // Stel profielgegevens in op de UI
        holder.name.setText(profile.getName());
        holder.email.setText(profile.getEmail());
        holder.location.setText(profile.getLocation());

        // Laad profielfoto met Glide
        Glide.with(context)
                .load(profile.getProfileImageUrl())
                .circleCrop()
                .placeholder(R.drawable.circle_background) // Placeholder afbeelding
                .into(holder.profilePic);

        // Voeg onClickListener toe als je een actie op profielklikken wilt toevoegen
        holder.itemView.setOnClickListener(v -> {
            // Actie bij klikken op een profiel
        });
    }

    @Override
    public int getItemCount() {
        return profiles.size();
    }

    // Methode om de lijst met profielen bij te werken
    public void updateProfiles(ArrayList<Profile> newProfiles) {
        this.profiles = newProfiles;
        notifyDataSetChanged();
    }

    // ViewHolder Klasse voor referentie naar de UI elementen
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, email, location;
        ImageView profilePic;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            // Initialiseer de views
            name = itemView.findViewById(R.id.profileNameTxt);
            email = itemView.findViewById(R.id.profileEmailTxt);
            location = itemView.findViewById(R.id.profileLocationTxt);
            profilePic = itemView.findViewById(R.id.profileImage);
        }
    }
}
