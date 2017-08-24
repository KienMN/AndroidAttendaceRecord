package com.example.kienmaingoc.attendancerecord;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.rekognition.AmazonRekognitionClient;
import com.amazonaws.services.rekognition.model.FaceMatch;
import com.amazonaws.services.rekognition.model.Image;
import com.amazonaws.services.rekognition.model.SearchFacesByImageRequest;
import com.amazonaws.services.rekognition.model.SearchFacesByImageResult;
import com.amazonaws.util.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class CheckInActivity extends AppCompatActivity {

    static final int REQUEST_TAKE_PHOTO = 1;
    String mCurrentImagePath;
    TextView notificationTextView;
    ImageView imageView2;
    Button verifyButton;
    ProgressBar spinner2;
    String nameGetFromDatabase = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_in);

        notificationTextView = (TextView) findViewById(R.id.notificationTextView);
        imageView2 = (ImageView) findViewById(R.id.imageView2);
        verifyButton = (Button) findViewById(R.id.verifyButton);
        spinner2 = (ProgressBar) findViewById(R.id.spinner2);
        spinner2.setVisibility(View.INVISIBLE);
        dispatchTakeImageIntent();
    }

    public class UserCheckIn {
        String imagePath;
        Date checkInTime;

        UserCheckIn(String path, Date time) {
            imagePath = path;
            checkInTime = time;
        }

        public String getImagePath() {
            return imagePath;
        }

        public Date getCheckInTime() {
            return checkInTime;
        }
    }


    private void dispatchTakeImageIntent() {
        Intent takeImageIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there is a camera activity to handle the intent
        if (takeImageIntent.resolveActivity(getPackageManager()) != null) {
            // Create a file where image should go
            File imageFile = null;
            try {
                imageFile = createImageFile();

                //Continue only if the File was successfully created

                if (imageFile != null) {
                    Uri imageUri = FileProvider.getUriForFile(
                            getApplicationContext(),
                            "com.example.kienmaingoc.attendancerecord",
                            imageFile
                    );
                    takeImageIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                    startActivityForResult(takeImageIntent, REQUEST_TAKE_PHOTO);

                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
        Toast.makeText(getApplicationContext(), "Thank you", Toast.LENGTH_LONG).show();
        Bitmap bitmap = BitmapFactory.decodeFile(mCurrentImagePath);
        addImageToGallery(mCurrentImagePath, this);
        Log.i("mCurrentImagePath", mCurrentImagePath);
        imageView2.setImageBitmap(bitmap);
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp +"_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
        // Save image
        mCurrentImagePath = image.getAbsolutePath();
        return image;
    }

    private void addImageToGallery(String imagePath, Context context) {
        ContentValues values = new ContentValues();

        values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.MediaColumns.DATA, imagePath);

        context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

    }

    public void verify(View view) {
        RekognitionTask task = new RekognitionTask();
        try {
//            String name = task.execute(mCurrentImagePath).get();
//            notificationTextView.setText("Hello " + name);
            task.execute(mCurrentImagePath);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class RekognitionTask extends AsyncTask<String, Void, String> {
        // Create credentials
        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                getString(R.string.cognito_identity_pool),
                Regions.EU_WEST_1
        );
        // Create rekognition client
        AmazonRekognitionClient rekognitionClient = new AmazonRekognitionClient(credentialsProvider);

        @Override
        protected void onPreExecute() {
            spinner2.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onPostExecute(String s) {
            spinner2.setVisibility(View.INVISIBLE);
            notificationTextView.setVisibility(View.INVISIBLE);
            if (s != "") {
                notificationTextView.setVisibility(View.VISIBLE);
                notificationTextView.setText("Hello " + nameGetFromDatabase);
            } else {
                Toast.makeText(getApplicationContext(), "Some error occured", Toast.LENGTH_LONG).show();
            }
        }

        @Override
        protected String doInBackground(String... params) {

            String imagePath = params[0];
            String username = "";
            try {
                FileInputStream inputStream = new FileInputStream(imagePath);
                ByteBuffer byteBuffer = ByteBuffer.wrap(IOUtils.toByteArray(inputStream));
                Image image = new Image().withBytes(byteBuffer);
                SearchFacesByImageRequest request = new SearchFacesByImageRequest()
                        .withCollectionId(getString(R.string.default_collection_id))
                        .withImage(image)
                        .withMaxFaces(1)
                        .withFaceMatchThreshold(Float.valueOf(getString(R.string.threshold)));

                SearchFacesByImageResult result = rekognitionClient.searchFacesByImage(request);
                if (result != null) {
                    List<FaceMatch> faceMatchList =result.getFaceMatches();
                    for (FaceMatch face: faceMatchList) {
                        username = face.getFace().getExternalImageId();
                        nameGetFromDatabase = username;
                    }
                }
                else {
                    nameGetFromDatabase = "stranger";
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            return username;
        }
    }
}
