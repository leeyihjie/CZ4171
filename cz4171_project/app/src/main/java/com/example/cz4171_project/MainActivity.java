package com.example.cz4171_project;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.provider.MediaStore;
import android.net.Uri;
import android.Manifest;


import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;


import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

//import retrofit2.Retrofit;
//import retrofit2.converter.gson.GsonConverterFactory;
//import retrofit2.converter.scalars.ScalarsConverterFactory;
//import retrofit2.Call;
//import retrofit2.Callback;
//import retrofit2.Response;
//import retrofit2.http.POST;

public class MainActivity extends AppCompatActivity {
    // private static Retrofit retrofit;


    ImageView imagePreview;
    Button buttonCamera;
    Button buttonUpload;
    Button buttonPredict;
    TextView textPrediction;

    private static final int camImagePreviewID = 0;
    private static final int uploadImagePreviewID = 1;


    String ImagePath;
    private Bitmap photoBitmap;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imagePreview = (ImageView) findViewById(R.id.imagePreview);
        buttonCamera = (Button) findViewById(R.id.buttonCamera);
        buttonUpload = (Button) findViewById(R.id.buttonUpload);
        buttonPredict = (Button) findViewById(R.id.buttonPredict);
        textPrediction = (TextView) findViewById(R.id.textPrediction);

        if (Build.VERSION.SDK_INT >= 23) {
            requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);
        }

        // Function for Camera Button when pressed
        buttonCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent camera_intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(camera_intent, camImagePreviewID);
            }
        });

        // Function for upload image button when pressed
        buttonUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent upload_intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(upload_intent, uploadImagePreviewID);
            }
        });

        //Function for predict class of image button when pressed
        buttonPredict.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // To send image from either the gallery or camera to the server
                connectServer();
            }
        });

    }

    // This method will help to retrieve the image taken by the camera or uploaded by user
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Match the request 'camImagePreviewID' with requestCode
        // Do this if image is taken by camera
        if (requestCode == camImagePreviewID) {
            if (resultCode == RESULT_OK && data != null) {
                // BitMap is data structure of image file which store the image in memory
                photoBitmap = (Bitmap) data.getExtras().get("data");
                // Set the image in imageview for display
                imagePreview.setImageBitmap(photoBitmap);

                // Need to convert captured image to URI path for use in predict function later
            }
        }
        // Do this if image is uploaded from gallery
        else if (requestCode == uploadImagePreviewID) {
            if (resultCode == RESULT_OK && data != null) {
                Uri selectedPhoto = data.getData();
                // Set the image in imageview for display
                // imagePreview.setImageURI(selectedPhoto);

                // Get absolute file path for uploaded image in device from URI
                ImagePath = getPath(selectedPhoto);

                try {
                    Bitmap uploadBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedPhoto);
                    imagePreview.setImageBitmap(uploadBitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    // Method to get path to uploaded image
    public String getPath(Uri uri) {
        String[] projection = {MediaStore.MediaColumns.DATA};
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
        cursor.moveToFirst();
        String imagePath = cursor.getString(column_index);
        cursor.close();

        return imagePath;
    }

    void connectServer(){
        String ipv4Address = "192.168.1.1";
        String portNumber = "5001";

        String postUrl= "http://"+ipv4Address+":"+portNumber+"/predict/";

        // Prepare image to be sent over to server
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.RGB_565;

        // Read BitMap by file path
        Log.e("path", "---------------->" + ImagePath);

        Bitmap bitmap = BitmapFactory.decodeFile(ImagePath, options);
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] byteArray = stream.toByteArray();


        RequestBody postBodyImage = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image", "cat1.jpeg", RequestBody.create(MediaType.parse("image/*jpeg"), byteArray))
                .build();

        File file = new File(ImagePath);
        Log.e("file name", "---------------->" + file.getName());

//        RequestBody requestBody = RequestBody.create(MediaType.parse("image/*"), file);
//        MultipartBody.Part parts = MultipartBody.Part.createFormData("file", file.getName(), requestBody);
//
//        OkHttpClient okHttpClient = new OkHttpClient.Builder().build();
//        if (retrofit == null) {
//            retrofit = new Retrofit.Builder()
//                    .baseUrl(postUrl)
//                    .addConverterFactory(ScalarsConverterFactory.create())
//                    .client(okHttpClient)
//                    .build();
//        }
//        NetworkUtil networkUtil = retrofit.create(NetworkUtil.class);
//        Call<String> call = networkUtil.uploadImage(parts);
//        sendImage(postUrl, call);
        sendImage(postUrl, postBodyImage);
    }


    // Method to be called when predict button is pressed to upload image to server
    // Establish connection with server first
    // Then send image selected to server for classification
    private void sendImage(String postUrl, /*Call<String> call*/ RequestBody postBody) {
         //Get absolute file path of image to be uploaded
         File filePath = new File(ImagePath);

        Log.e("connection", "Attempting to connect to server......");

        OkHttpClient okhttpclient = new OkHttpClient();

        Request request = new Request.Builder().url(postUrl).post(postBody).build();

        // making call asynchronously
        okhttpclient.newCall(request).enqueue(new Callback() {
        // call.enqueue(new Callback() {
            @Override
            // called if we get a response from the server
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                Log.e("good", "connected");
                if (response.body() == null) {
                    Log.e("null response", "response from server is null");

                }
                if (response.body() != null) {
                    textPrediction.setText(response.body().toString());
                }
            }

            @Override
            // called if server is unreachable
            public void onFailure(Call call, IOException t) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.e("bad", "not connected");
                        Toast.makeText(MainActivity.this, "server down", Toast.LENGTH_SHORT).show();
                        textPrediction.setText("Error connecting to the server. Please try again.");
                    }
                });
            }

        });

    }
}