package com.ap.neighborrentapplication.adapter;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ap.neighborrentapplication.R;
import com.ap.neighborrentapplication.models.Device;
import com.ap.neighborrentapplication.models.Reservation;
import com.ap.neighborrentapplication.models.Review;
import com.ap.neighborrentapplication.ui.activity.DashboardActivity;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.GranularRoundedCorners;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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

        // Check if current user is the owner
        String currentUserId = auth.getCurrentUser().getUid();
        boolean isOwner = device.getOwnerId().equals(currentUserId);

        // Toon of verberg de reserveerknop op basis van beschikbaarheid en eigenaarschap
        holder.reserveButton.setVisibility(
            (device.getAvailable() && !isOwner) ? View.VISIBLE : View.GONE
        );

        // Voeg click listener toe aan de reserveerknop als de gebruiker niet de eigenaar is
        if (!isOwner) {
            holder.reserveButton.setOnClickListener(v -> showReservationDialog(device));
        }

        // Laad de afbeelding met Glide en gebruik afgeronde hoeken
        Glide.with(context)
                .load(device.getImageUrl())
                .transform(new GranularRoundedCorners(30, 30, 0, 0))
                .into(holder.pic);

        // Load and display average rating
        loadDeviceRating(device.getId(), holder);

        // Check if device is in favorites
        checkIfFavorite(device.getId(), holder.favoriteIcon);

        holder.favoriteIcon.setOnClickListener(v -> toggleFavorite(device, holder.favoriteIcon));
    }

    private void loadDeviceRating(String deviceId, ViewHolder holder) {
        FirebaseFirestore.getInstance()
            .collection("reviews")
            .whereEqualTo("deviceId", deviceId)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                if (!querySnapshot.isEmpty()) {
                    float totalRating = 0;
                    int count = querySnapshot.size();
                    List<Review> reviews = new ArrayList<>();

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Review review = doc.toObject(Review.class);
                        if (review != null) {
                            review.setId(doc.getId()); // Set the document ID
                            totalRating += review.getRating();
                            reviews.add(review);
                        }
                    }

                    float averageRating = totalRating / count;
                    holder.averageRatingBar.setRating(averageRating);
                    holder.ratingText.setText(String.format(Locale.getDefault(), 
                        "%.1f (%d %s)", 
                        averageRating, 
                        count, 
                        count == 1 ? "beoordeling" : "beoordelingen"));
                    holder.ratingContainer.setVisibility(View.VISIBLE);

                    // Make rating container clickable
                    holder.ratingContainer.setOnClickListener(v -> showReviewsDialog(
                        holder.title.getText().toString(),
                        averageRating,
                        count,
                        reviews
                    ));
                } else {
                    holder.ratingContainer.setVisibility(View.GONE);
                }
            })
            .addOnFailureListener(e -> {
                Log.e("DevicesAdapter", "Error loading reviews", e);
                holder.ratingContainer.setVisibility(View.GONE);
            });
    }

    private void showReviewsDialog(String deviceName, float averageRating, int reviewCount, List<Review> reviews) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_device_reviews, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(dialogView);

        TextView deviceNameText = dialogView.findViewById(R.id.deviceNameText);
        RatingBar averageRatingBar = dialogView.findViewById(R.id.averageRatingBar);
        TextView averageRatingText = dialogView.findViewById(R.id.averageRatingText);
        RecyclerView reviewsRecyclerView = dialogView.findViewById(R.id.reviewsRecyclerView);
        Button closeButton = dialogView.findViewById(R.id.closeButton);

        deviceNameText.setText(deviceName);
        averageRatingBar.setRating(averageRating);
        averageRatingText.setText(String.format(Locale.getDefault(),
            "%.1f (%d %s)",
            averageRating,
            reviewCount,
            reviewCount == 1 ? "beoordeling" : "beoordelingen"));

        // Setup RecyclerView
        reviewsRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        ReviewAdapter adapter = new ReviewAdapter(context);
        reviewsRecyclerView.setAdapter(adapter);
        adapter.setReviews(reviews);

        AlertDialog dialog = builder.create();
        closeButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
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

    // Methode om te controleren of een periode beschikbaar is
    private void checkAvailability(String deviceId, Date startDate, Date endDate, OnAvailabilityCheckListener listener) {
        firestore.collection("reservations")
            .whereEqualTo("deviceId", deviceId)
            .whereEqualTo("status", "accepted")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                boolean isAvailable = true;
                String conflictMessage = null;

                for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                    Reservation existingReservation = document.toObject(Reservation.class);
                    if (existingReservation != null) {
                        // Check if dates overlap
                        if (!(endDate.before(existingReservation.getStartDate()) || 
                            startDate.after(existingReservation.getEndDate()))) {
                            isAvailable = false;
                            SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", new Locale("nl"));
                            conflictMessage = String.format("Dit apparaat is al gereserveerd van %s tot %s",
                                dateFormat.format(existingReservation.getStartDate()),
                                dateFormat.format(existingReservation.getEndDate()));
                            break;
                        }
                    }
                }
                
                listener.onResult(isAvailable, conflictMessage);
            })
            .addOnFailureListener(e -> {
                listener.onResult(false, "Fout bij het controleren van beschikbaarheid: " + e.getMessage());
            });
    }

    // Interface voor availability check callback
    private interface OnAvailabilityCheckListener {
        void onResult(boolean isAvailable, String message);
    }

    // Methode om totaalprijs te berekenen en bij te werken
    private void updateTotalPrice(Calendar startDate, Calendar endDate, double pricePerDay, TextView totalPriceText) {
        if (startDate != null && endDate != null && 
            startDate.getTime().after(new Date(System.currentTimeMillis() - 1000 * 60 * 60 * 24))) {
            
            // Bereken het aantal dagen (inclusief dezelfde dag)
            long diffInMillis = endDate.getTimeInMillis() - startDate.getTimeInMillis();
            int days = (int) (diffInMillis / (1000 * 60 * 60 * 24)) + 1;
            double totalPrice = days * pricePerDay;
            totalPriceText.setText(String.format(Locale.getDefault(), "€%.2f", totalPrice));
        } else {
            totalPriceText.setText("€0.00");
        }
    }

    // Methode om de reserveringsdialog te tonen
    private void showReservationDialog(Device device) {
        // Dialog aanmaken
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_reservation, null);
        builder.setView(dialogView);

        // Dialog elementen ophalen
        com.google.android.material.textfield.TextInputEditText startDateInput = dialogView.findViewById(R.id.startDateInput);
        com.google.android.material.textfield.TextInputEditText endDateInput = dialogView.findViewById(R.id.endDateInput);
        TextView totalPriceText = dialogView.findViewById(R.id.totalPriceText);
        MaterialButton submitButton = dialogView.findViewById(R.id.submitButton);

        // Calendar instanties voor datum selectie
        Calendar startDate = Calendar.getInstance();
        Calendar endDate = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());

        // Click listeners voor datum velden
        startDateInput.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(context,
                    (view, year, month, dayOfMonth) -> {
                        startDate.set(year, month, dayOfMonth);
                        startDateInput.setText(dateFormat.format(startDate.getTime()));
                        // Reset end date als die voor de start date ligt
                        if (endDate.getTimeInMillis() < startDate.getTimeInMillis()) {
                            endDate.setTimeInMillis(startDate.getTimeInMillis());
                            endDateInput.setText(dateFormat.format(endDate.getTime()));
                        }
                        updateTotalPrice(startDate, endDate, device.getPricePerDay(), totalPriceText);
                    },
                    startDate.get(Calendar.YEAR),
                    startDate.get(Calendar.MONTH),
                    startDate.get(Calendar.DAY_OF_MONTH));
            datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
            datePickerDialog.show();
        });

        endDateInput.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(context,
                    (view, year, month, dayOfMonth) -> {
                        endDate.set(year, month, dayOfMonth);
                        endDateInput.setText(dateFormat.format(endDate.getTime()));
                        updateTotalPrice(startDate, endDate, device.getPricePerDay(), totalPriceText);
                    },
                    endDate.get(Calendar.YEAR),
                    endDate.get(Calendar.MONTH),
                    endDate.get(Calendar.DAY_OF_MONTH));
            datePickerDialog.getDatePicker().setMinDate(startDate.getTimeInMillis());
            datePickerDialog.show();
        });

        // Dialog aanmaken en tonen
        android.app.AlertDialog dialog = builder.create();
        dialog.show();

        // Submit button click listener
        submitButton.setOnClickListener(v -> {
            if (startDateInput.getText().toString().isEmpty() || endDateInput.getText().toString().isEmpty()) {
                Toast.makeText(context, "Selecteer eerst begin- en einddatum", Toast.LENGTH_SHORT).show();
                return;
            }

            // Controleer beschikbaarheid voordat we de reservering maken
            checkAvailability(device.getId(), startDate.getTime(), endDate.getTime(), (isAvailable, message) -> {
                if (!isAvailable) {
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show();
                    return;
                }

                // Bereken totale prijs (inclusief dezelfde dag)
                long diffInMillis = endDate.getTimeInMillis() - startDate.getTimeInMillis();
                int days = (int) (diffInMillis / (1000 * 60 * 60 * 24)) + 1;
                double totalPrice = days * device.getPricePerDay();

                // Maak nieuwe reservering
                String currentUserId = auth.getCurrentUser().getUid();
                Reservation reservation = new Reservation(
                    device.getId(),
                    currentUserId,
                    device.getOwnerId(),
                    startDate.getTime(),
                    endDate.getTime(),
                    totalPrice
                );

                // Sla reservering op in Firebase
                firestore.collection("reservations")
                    .add(reservation)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(context, "Reserveringsaanvraag verzonden!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(context, "Fout bij verzenden aanvraag: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                    });
            });
        });
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    // ViewHolder Klasse voor referentie naar de UI elementen
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, subtitle, price, city, status, ratingText;
        ImageView pic, favoriteIcon;
        MaterialButton reserveButton;
        RatingBar averageRatingBar;
        View ratingContainer;

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
            reserveButton = itemView.findViewById(R.id.reserveButton);
            averageRatingBar = itemView.findViewById(R.id.averageRatingBar);
            ratingText = itemView.findViewById(R.id.ratingText);
            ratingContainer = itemView.findViewById(R.id.ratingContainer);
        }
    }

    // Methode om de lijst met apparaten bij te werken
    public void updateDevices(ArrayList<Device> newDevices) {
        this.devices = newDevices;
        notifyDataSetChanged();
    }
}
