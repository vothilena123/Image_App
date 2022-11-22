package com.vothilena.imageapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CameraActivity extends AppCompatActivity {

    protected ImageView showPicture;
    protected Button btnOpenCamera;
    protected TextView description;
    protected ImageButton btnBack, btnNext;

    protected ArrayList<Uri> _pictureList = new ArrayList<>();
    protected int _currentPosition = 0;

    protected static final int CAMERA_REQUEST_CODE = 100;
    protected static final int CAMERA_REQUEST_CODE_PERMISSIONS = 125;
    protected static final String[] CAMERA_REQUIRED_PERMISSIONS = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    protected final int TIME_LOCATION_REFRESH = 15000; // 15 seconds to update.
    protected final int DISTANCE_LOCATION_REFRESH = 500; // 500 meters to update.
    protected final int GPS_REQUEST_CODE_PERMISSIONS = 105;
    protected final String[] GPS_REQUIRED_PERMISSIONS = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    protected Location currentPlace;
    protected LocationManager locationManagement;
    protected LocationListener locationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        showPicture = findViewById(R.id.showPicture);
        description = findViewById(R.id.tvDescription);
        btnBack = findViewById(R.id.fabBack);
        btnNext = findViewById(R.id.fabNext);
        btnOpenCamera = findViewById(R.id.btnOpenCamera);

        btnOpenCamera.setOnClickListener(v -> openCamera());
        btnNext.setOnClickListener(v -> nextPicture());
        btnBack.setOnClickListener(v -> previousPicture());

        turnOnGPS();
        loadPicture();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CAMERA_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK && null != data) {
                Uri uri = data.getData();

                if (null == uri) {
                    Bitmap bitmap = (Bitmap) data.getExtras().get("data");
                    uri = savePicture(bitmap);
                    writePictureDetail(uri);
                }

                _pictureList.add(uri);
                _currentPosition = _pictureList.size() - 1;
                loadPicture();

                return;
            }

            Toast.makeText(CameraActivity.this, "Select Image Failed.", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadPicture() {
        if (_pictureList == null || _pictureList.size() == 0) {
            Toast.makeText(CameraActivity.this, "No Image !!!", Toast.LENGTH_SHORT).show();
            return;
        }

        Uri imageUri = _pictureList.get(_currentPosition);

        showPicture.setImageURI(imageUri);

        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                InputStream input = getContentResolver().openInputStream(imageUri);
                ExifInterface eInterface = new ExifInterface(input);

                String addressTag = "Can not found";

                String dateTag = eInterface.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL);
                String latitudeTag = eInterface.getAttribute(ExifInterface.TAG_GPS_DEST_LATITUDE_REF);
                String longitudeTag = eInterface.getAttribute(ExifInterface.TAG_GPS_DEST_LONGITUDE_REF);

                if (longitudeTag != null && !longitudeTag.isEmpty() &&
                        latitudeTag != null && !latitudeTag.isEmpty()) {
                    double _latitude = Double.parseDouble(latitudeTag);
                    double _longitude = Double.parseDouble(longitudeTag);

                    Geocoder gcd = new Geocoder(getBaseContext(), Locale.getDefault());
                    List<Address> addresses = gcd.getFromLocation(_latitude, _longitude, 1);

                    if (addresses.size() > 0) {
                        addressTag = addresses.get(0).getAddressLine(0);
                    }
                }

                String _description = "Picture took on " + dateTag + " at " + addressTag + ".";
                description.setText(_description);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("MissingPermission")
    private void turnOnGPS() {
        if (!allPermissions_GPS()) {
            ActivityCompat.requestPermissions(this, GPS_REQUIRED_PERMISSIONS, GPS_REQUEST_CODE_PERMISSIONS);
            return;
        }

        locationListener = location -> {
            currentPlace = location;
            Toast.makeText(CameraActivity.this, "Successfully obtained the current location.", Toast.LENGTH_SHORT).show();
        };

        locationManagement = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManagement.requestLocationUpdates(LocationManager.GPS_PROVIDER, TIME_LOCATION_REFRESH, DISTANCE_LOCATION_REFRESH, locationListener);
    }

    private void previousPicture() {
        ++_currentPosition;
        loadPicture();
    }

    private void nextPicture() {
        --_currentPosition;
        loadPicture();
    }

    private void openCamera() {
        if (!allPermissions_CAMERA()) {
            ActivityCompat.requestPermissions(CameraActivity.this, CAMERA_REQUIRED_PERMISSIONS, CAMERA_REQUEST_CODE_PERMISSIONS);
            return;
        }

        Intent pictureCaptureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        Intent fileIntent = new Intent(Intent.ACTION_GET_CONTENT);
        fileIntent.setType("image/*");

        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryIntent.setType("image/*");

        Intent chooserIntent = Intent.createChooser(pictureCaptureIntent, "Select Image");
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{fileIntent, galleryIntent});

        startActivityForResult(chooserIntent, CAMERA_REQUEST_CODE);
    }

    private boolean allPermissions_CAMERA() {
        for (String permission : CAMERA_REQUIRED_PERMISSIONS)
            if (ContextCompat.checkSelfPermission(CameraActivity.this, permission) != PackageManager.PERMISSION_GRANTED)
                return false;
        return true;
    }

    private boolean allPermissions_GPS() {
        for (String permission : GPS_REQUIRED_PERMISSIONS)
            if (ContextCompat.checkSelfPermission(CameraActivity.this, permission) != PackageManager.PERMISSION_GRANTED)
                return false;

        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_REQUEST_CODE_PERMISSIONS) {
            if (!allPermissions_CAMERA()) {
                Toast.makeText(CameraActivity.this, "User does not allow camera access.", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        if (requestCode == GPS_REQUEST_CODE_PERMISSIONS) {
            if (allPermissions_GPS()) {
                turnOnGPS();
                return;
            }

            Toast.makeText(CameraActivity.this, "User does not allow GPS access.", Toast.LENGTH_SHORT).show();
        }
    }

    private void writePictureDetail(Uri uri) {
        try (ParcelFileDescriptor picturePfd = getContentResolver().openFileDescriptor(uri, "rw")) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                ExifInterface exif = new ExifInterface(picturePfd.getFileDescriptor());

                exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, new Date().toString());
                exif.setAttribute(ExifInterface.TAG_ARTIST, "Serco Honan");
                exif.setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, "Codes from Serco Honan source.");

                if (null != currentPlace) {
                    exif.setAttribute(ExifInterface.TAG_GPS_DEST_LATITUDE_REF, String.valueOf(currentPlace.getLatitude()));
                    exif.setAttribute(ExifInterface.TAG_GPS_DEST_LONGITUDE_REF, String.valueOf(currentPlace.getLongitude()));
                }

                exif.saveAttributes();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Uri savePicture(Bitmap bitmap) {
        String fileName = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US).format(new Date());
        String path = Environment.DIRECTORY_PICTURES + File.separator + "Picture-From-Camera";

        ContentValues cv = new ContentValues();
        cv.put(MediaStore.MediaColumns.RELATIVE_PATH, path);
        cv.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        cv.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");

        ContentResolver resolve = getContentResolver();
        Uri pictureUri = resolve.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv);

        try (ParcelFileDescriptor parcelDescriptor = resolve.openFileDescriptor(pictureUri, "w")) {
            FileDescriptor fileDescriptor = parcelDescriptor.getFileDescriptor();

            try (OutputStream outputStream = new FileOutputStream(fileDescriptor)) {
                // Perform operations on "stream".
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            }

            // Sync data with disk. It's mandatory to be able later to call writeExif.
            fileDescriptor.sync();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Picture saved fail.", Toast.LENGTH_SHORT).show();
            return null;
        }

        return pictureUri;
    }
}