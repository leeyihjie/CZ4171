package com.example.cz4171_project;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Environment;
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

import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import cz.msebera.android.httpclient.Header;


public class MainActivity extends AppCompatActivity {


    ImageView imagePreview;
    Button buttonCamera;
    Button buttonUpload;
    Button buttonPredict;
    TextView textPrediction;

    private static final int camImagePreviewID = 0;
    private static final int uploadImagePreviewID = 1;

    String ImagePath;

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

        // Function for camera button when pressed
        buttonCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchTakePictureIntent();
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


    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        ImagePath = image.getAbsolutePath();
        return image;
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        Log.e("intent", "camera intent done" );

        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        BuildConfig.APPLICATION_ID + ".provider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, camImagePreviewID);
            }
        }
    }

    // This method will help to retrieve the image taken by the camera or uploaded by user
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == camImagePreviewID) {
            if (resultCode == RESULT_OK && data != null) {
                Bitmap cameraImageBM = BitmapFactory.decodeFile(ImagePath);
                // Display Camera Image in Image Preview
                imagePreview.setImageBitmap(cameraImageBM);
            }
        }
        // Do this if image is uploaded from gallery
        if (requestCode == uploadImagePreviewID) {
            if (resultCode == RESULT_OK && data != null) {
                Uri selectedPhoto = data.getData();
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

    // Capitalize response
    public String toFirstCharUpperAll(String string){
        StringBuffer sb=new StringBuffer(string);
        for(int i=0;i<sb.length();i++)
            if(i==0 || sb.charAt(i-1)==' ')//first letter to uppercase by default
                sb.setCharAt(i, Character.toUpperCase(sb.charAt(i)));
        return sb.toString();
    }

    // Method to get path to uploaded/captured image
    public String getPath(Uri uri) {
        String[] projection = {MediaStore.MediaColumns.DATA};
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
        cursor.moveToFirst();
        String imagePath = cursor.getString(column_index);
        cursor.close();

        return imagePath;
    }

    void connectServer() {
        String ipv4Address = "192.168.1.1";
        String portNumber = "5001";

        // URL of server page to get predictions
        String postUrl = "http://" + ipv4Address + ":" + portNumber + "/predict";

        // Prepare image to be sent over to server
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.RGB_565;

        // Read BitMap by file path
        Log.e("Image path", "---------------->" + ImagePath);

        Bitmap bitmap = BitmapFactory.decodeFile(ImagePath, options);
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);

        // Create a temporary file to store image to be uploaded
        File tempImageFile = null;
        try {
            tempImageFile = File.createTempFile("image", ".jpg");
        } catch (IOException e) {
            e.printStackTrace();
        }
        OutputStream outImageFile = null;
        try {
            outImageFile = new FileOutputStream(tempImageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outImageFile);
            outImageFile.flush();
            outImageFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        RequestParams rParams = new RequestParams();

        try {
            rParams.put("file", tempImageFile);
            Log.e("RequestParams", rParams.toString());
        } catch (FileNotFoundException e) {}
        Log.e("Request Sent", "Sending Request to Server");

        // post method called from separate thread (NetworkUtil) as connecting to server and making
        // request on current thread (MainActivity) causes "android.os.networkonmainthreadexception"
        // error
        NetworkUtil.post(postUrl, rParams, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                // Response from server: returns JSONObject contain prediction or error message
                Log.e("Response", "---------------->" + response);
                try {
                    // Get message based on key on JSONObject
                    // Error message string
                    // If JSONObject returned has Error key, display this message to user
                    if (response.has("Error")) {
                        String error_key_value = response.getString("Error");
                        Log.e("Error Message", "---------------->" + error_key_value);

                        // textPrediction = (TextView) findViewById(R.id.textPrediction);
                        textPrediction.setText(error_key_value);
                    }
                    else if (response.has("Prediction")) {
                        String pred_value = response.getString("Prediction");
                        Log.e("Prediction Message", "---------------->" + pred_value);
                        pred_value = pred_value.replace("_", " ");
                        pred_value = toFirstCharUpperAll(pred_value);
                        textPrediction.setText(pred_value);
                    }
                    else {
                        // Response null/empty/etc.
                        // textPrediction = (TextView) findViewById(R.id.textPrediction);
                        textPrediction.setText("Please upload or take another picture!");
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}