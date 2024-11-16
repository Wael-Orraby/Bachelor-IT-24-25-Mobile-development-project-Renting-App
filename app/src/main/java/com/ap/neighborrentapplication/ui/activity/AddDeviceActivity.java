package com.ap.neighborrentapplication.ui.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.ap.neighborrentapplication.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.UUID;
import okhttp3.*;

public class AddDeviceActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_IMAGE_PICK = 1001;
    private static final String IMGBB_API_KEY = "0cc82980065cfb6631ebb4f4bade61fc";

    private EditText editTextName, editTextDescription, editTextPrice;
    private Button buttonAddDevice, buttonUploadImage;
    private ImageView imageViewPreview;
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private String imageUrl = "";
    private String ownerId, ownerName, ownerEmail, ownerCity, ownerPostalCode;
    private Uri imageUri;
    private Spinner categorySpinner;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_device);

        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        editTextName = findViewById(R.id.editText_name);
        editTextDescription = findViewById(R.id.editText_description);
        editTextPrice = findViewById(R.id.editText_price);
        buttonAddDevice = findViewById(R.id.button_add_device);
        buttonUploadImage = findViewById(R.id.button_upload_image);
        imageViewPreview = findViewById(R.id.imageView_preview);
        categorySpinner = findViewById(R.id.spinner_category);


        loadOwnerData();

        buttonUploadImage.setOnClickListener(v -> selectImageFromGallery());
        buttonAddDevice.setOnClickListener(v -> {
            if (imageUri != null) {
                uploadImageToImgbb();
            } else {
                Toast.makeText(this, "Selecteer een afbeelding", Toast.LENGTH_SHORT).show();
            }
        });

        setupCategorySpinner();
    }

    private void loadOwnerData() {
        String currentUserId = auth.getCurrentUser().getUid();
        firestore.collection("users").document(currentUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        ownerId = currentUserId;
                        ownerName = documentSnapshot.getString("firstName") + " " + documentSnapshot.getString("lastName");
                        ownerEmail = documentSnapshot.getString("email");
                        ownerCity = documentSnapshot.getString("city");
                        ownerPostalCode = documentSnapshot.getString("postalCode");
                    } else {
                        Toast.makeText(this, "Gebruikersgegevens niet gevonden", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Fout bij ophalen gebruiker: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void selectImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_CODE_IMAGE_PICK);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_IMAGE_PICK && resultCode == RESULT_OK && data != null) {
            imageUri = data.getData();
            imageViewPreview.setImageURI(imageUri);
        }
    }

    private void uploadImageToImgbb() {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            String encodedImage = Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT);

            OkHttpClient client = new OkHttpClient();
            RequestBody formBody = new FormBody.Builder()
                    .add("key", IMGBB_API_KEY)
                    .add("image", encodedImage)
                    .build();

            Request request = new Request.Builder()
                    .url("https://api.imgbb.com/1/upload")
                    .post(formBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> Toast.makeText(AddDeviceActivity.this, "Afbeelding uploaden mislukt: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseBody = response.body().string();
                        imageUrl = extractImageUrlFromResponse(responseBody);
                        runOnUiThread(() -> saveDeviceToFirestore());
                    } else {
                        runOnUiThread(() -> Toast.makeText(AddDeviceActivity.this, "Afbeelding uploaden naar imgbb mislukt", Toast.LENGTH_SHORT).show());
                    }
                }
            });
        } catch (Exception e) {
            Toast.makeText(this, "Fout bij afbeeldingverwerking: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String extractImageUrlFromResponse(String responseBody) {
        try {
            JSONObject json = new JSONObject(responseBody);
            return json.getJSONObject("data").getString("url");
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Fout bij ophalen afbeelding-URL", Toast.LENGTH_SHORT).show();
            return "";
        }
    }

    private void saveDeviceToFirestore() {
        String name = editTextName.getText().toString().trim();
        String description = editTextDescription.getText().toString().trim();
        String priceText = editTextPrice.getText().toString().trim();

        if (name.isEmpty() || description.isEmpty() || priceText.isEmpty() || imageUrl.isEmpty()) {
            Toast.makeText(this, "Vul alle velden in", Toast.LENGTH_SHORT).show();
            return;
        }

        double price;
        try {
            price = Double.parseDouble(priceText);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Ongeldige prijs", Toast.LENGTH_SHORT).show();
            return;
        }

        HashMap<String, Object> deviceData = new HashMap<>();
        deviceData.put("id", UUID.randomUUID().toString());
        deviceData.put("name", name);
        deviceData.put("description", description);
        deviceData.put("imageUrl", imageUrl);
        deviceData.put("pricePerDay", price);
        deviceData.put("available", true);
        deviceData.put("city", ownerCity);
        deviceData.put("ownerId", ownerId);
        deviceData.put("ownerName", ownerName);
        deviceData.put("ownerEmail", ownerEmail);
        deviceData.put("postalCode", ownerPostalCode);

        String categoryId;
        int position = categorySpinner.getSelectedItemPosition();
        if (position == 0) {
            Toast.makeText(this, "Selecteer een categorie", Toast.LENGTH_SHORT).show();
            return;
        }
        switch (position) {
            case 0:
                categoryId = "kitchen";
                break;
            case 1:
                categoryId = "cleaning";
                break;
            case 2:
                categoryId = "garden";
                break;
            default:
                categoryId = "";
        }
        deviceData.put("category", categoryId);
        deviceData.put("categoryName", categorySpinner.getSelectedItem().toString());

        firestore.collection("devices")
                .add(deviceData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Toestel succesvol toegevoegd!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Toevoegen mislukt: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void setupCategorySpinner() {
        String[] categories = {"Selecteer categorie:","Keukenapparaten", "Schoonmaakapparaten", "Tuinapparaten"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                categories
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);
    }
}
