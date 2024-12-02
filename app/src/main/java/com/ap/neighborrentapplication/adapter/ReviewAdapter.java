package com.ap.neighborrentapplication.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ap.neighborrentapplication.R;
import com.ap.neighborrentapplication.models.Review;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ViewHolder> {
    private static final String TAG = "ReviewAdapter";
    private List<Review> reviews;
    private Context context;
    private SimpleDateFormat dateFormat;
    private FirebaseAuth auth;

    public ReviewAdapter(Context context) {
        this.context = context;
        this.reviews = new ArrayList<>();
        this.dateFormat = new SimpleDateFormat("dd MMM yyyy", new Locale("nl"));
        this.auth = FirebaseAuth.getInstance();
    }

    public void setReviews(List<Review> reviews) {
        this.reviews = reviews;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_review, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Review review = reviews.get(position);
        
        // Set rating and comment
        holder.ratingBar.setRating(review.getRating());
        holder.commentText.setText(review.getComment());
        
        // Format and set date if available
        if (review.getCreatedAt() != null) {
            String formattedDate = dateFormat.format(review.getCreatedAt());
            holder.dateText.setText(formattedDate);
        }

        // Show edit button for current user's reviews
        if (auth.getCurrentUser() != null && review.getUserId() != null) {
            String currentUserId = auth.getCurrentUser().getUid();
            Log.d(TAG, "Checking edit button visibility - Current user: " + currentUserId + ", Review user: " + review.getUserId());
            
            if (currentUserId.equals(review.getUserId())) {
                Log.d(TAG, "Showing edit button for review: " + review.getId());
                holder.editButton.setVisibility(View.VISIBLE);
                holder.editButton.setOnClickListener(v -> showEditDialog(review));
            } else {
                Log.d(TAG, "Hiding edit button - user IDs don't match");
                holder.editButton.setVisibility(View.GONE);
            }
        } else {
            Log.d(TAG, "Hiding edit button - auth or review user ID is null");
            holder.editButton.setVisibility(View.GONE);
        }

        // Load reviewer name
        if (review.getUserId() != null) {
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(review.getUserId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String firstName = documentSnapshot.getString("firstName");
                        String lastName = documentSnapshot.getString("lastName");
                        if (firstName != null && lastName != null) {
                            String fullName = firstName + " " + lastName;
                            holder.reviewerNameText.setText(fullName);
                        } else {
                            holder.reviewerNameText.setText("Onbekende gebruiker");
                        }
                    } else {
                        holder.reviewerNameText.setText("Gebruiker niet gevonden");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading reviewer name", e);
                    holder.reviewerNameText.setText("Fout bij laden gebruiker");
                });
        } else {
            holder.reviewerNameText.setText("Geen gebruiker ID");
        }
    }

    private void showEditDialog(Review review) {
        Log.d(TAG, "Showing edit dialog for review: " + review.getId());
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_review, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(dialogView);

        RatingBar ratingBar = dialogView.findViewById(R.id.ratingBar);
        EditText commentEdit = dialogView.findViewById(R.id.commentEditText);
        Button submitButton = dialogView.findViewById(R.id.submitButton);
        Button cancelButton = dialogView.findViewById(R.id.cancelButton);

        // Set current values
        ratingBar.setRating(review.getRating());
        commentEdit.setText(review.getComment());

        AlertDialog dialog = builder.create();

        submitButton.setOnClickListener(v -> {
            float newRating = ratingBar.getRating();
            String newComment = commentEdit.getText().toString().trim();

            if (newRating == 0) {
                Toast.makeText(context, "Geef een beoordeling", Toast.LENGTH_SHORT).show();
                return;
            }

            // Update review in Firestore
            FirebaseFirestore.getInstance()
                .collection("reviews")
                .document(review.getId())
                .update(
                    "rating", newRating,
                    "comment", newComment
                )
                .addOnSuccessListener(aVoid -> {
                    // Update local review object
                    review.setRating(newRating);
                    review.setComment(newComment);
                    notifyDataSetChanged();
                    dialog.dismiss();
                    Toast.makeText(context, "Beoordeling bijgewerkt", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating review", e);
                    Toast.makeText(context, "Fout bij bijwerken beoordeling", Toast.LENGTH_SHORT).show();
                });
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    @Override
    public int getItemCount() {
        return reviews.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final RatingBar ratingBar;
        final TextView commentText;
        final TextView dateText;
        final TextView reviewerNameText;
        final ImageButton editButton;

        ViewHolder(View itemView) {
            super(itemView);
            ratingBar = itemView.findViewById(R.id.reviewRatingBar);
            commentText = itemView.findViewById(R.id.reviewComment);
            dateText = itemView.findViewById(R.id.reviewDate);
            reviewerNameText = itemView.findViewById(R.id.reviewerName);
            editButton = itemView.findViewById(R.id.editButton);
        }
    }
}
