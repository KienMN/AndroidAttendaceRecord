package com.example.kienmaingoc.attendancerecord;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.rekognition.AmazonRekognitionClient;
import com.amazonaws.services.rekognition.model.Attribute;
import com.amazonaws.services.rekognition.model.DeleteFacesRequest;
import com.amazonaws.services.rekognition.model.DetectLabelsRequest;
import com.amazonaws.services.rekognition.model.DetectLabelsResult;
import com.amazonaws.services.rekognition.model.FaceRecord;
import com.amazonaws.services.rekognition.model.Image;
import com.amazonaws.services.rekognition.model.IndexFacesRequest;
import com.amazonaws.services.rekognition.model.IndexFacesResult;
import com.amazonaws.services.rekognition.model.ListCollectionsRequest;
import com.amazonaws.services.rekognition.model.ListCollectionsResult;
import com.amazonaws.services.rekognition.model.SearchFacesRequest;
import com.amazonaws.util.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;

// Add a new user (containing a face image and name) to the database
// Database uses FaceCollection of AWS Rekognition

public class AddNewUserActivity extends AppCompatActivity {

    final int CHOOSE_IMAGE_CODE = 1;
    final int ADD_USER_SUCCESSFULLY = 2;
    final int ADD_USER_FAIL = 3;
    Button chooseImageButton;
    Button addNewButton;
    TextView usernameTextView;
    TextView collectionIdTextView;
    EditText usernameEditText;
    EditText collectionIdEditText;
    ImageView imageView;
    Uri uri;
    ProgressBar spinner3;
    private String resultNotification;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_new_user);

        // Set up layout of screen
        collectionIdTextView = (TextView) findViewById(R.id.collectionIdTextView);
        collectionIdEditText = (EditText) findViewById(R.id.collectionIdEditText);
        collectionIdEditText.setText(getString(R.string.default_collection_id));
        collectionIdEditText.setEnabled(false);

        usernameTextView = (TextView) findViewById(R.id.usernameTextView);
        usernameEditText = (EditText) findViewById(R.id.usernameEditText);

        chooseImageButton = (Button) findViewById(R.id.chooseImageButton);
        addNewButton = (Button) findViewById(R.id.addNewButton);

        imageView = (ImageView) findViewById(R.id.imageView);

        spinner3 = (ProgressBar) findViewById(R.id.spinner3);
        spinner3.setVisibility(View.INVISIBLE);
    }

    // Choose an image from gallery
    public void chooseImage(View view) {
        Intent chooseImageIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(chooseImageIntent, CHOOSE_IMAGE_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Display image to screen
        if (resultCode == RESULT_OK) {
            uri = data.getData();
//            Log.i("uri", uri.toString());
            Bitmap bitmap = null;
            try {
                bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(uri));
                imageView.setImageBitmap(bitmap);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    // Add new face to face collection of AWS Rekognition
    public void addNew(View view) {
        if (uri == null) {
            Toast.makeText(getApplicationContext(), "You must choose an image", Toast.LENGTH_LONG).show();
        } else {
            try {
                // Get path of image
                String[] proj = {MediaStore.Images.Media.DATA};
                Cursor cursor = getContentResolver().query(uri, proj, null, null, null);
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                cursor.moveToFirst();
                String imagePath = cursor.getString(column_index);
                // Get external id (name of user) of image
                String imageExternalId = String.valueOf(usernameEditText.getText());
                // Upload to database
                RekognitionTask task = new RekognitionTask();
                task.execute(imagePath, imageExternalId);
                Toast.makeText(getApplicationContext(), resultNotification, Toast.LENGTH_LONG).show();
                finish();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    // Manage methods of AWS Rekognition
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
            spinner3.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onPostExecute(String s) {
            spinner3.setVisibility(View.INVISIBLE);
        }

        // Index face to face collection
        @Override
        protected String doInBackground(String... params) {
            String res = "";
            try {
                // Create an image to put to rekognition
                FileInputStream fileInputStream = new FileInputStream(params[0]);
                ByteBuffer byteBuffer = ByteBuffer.wrap(IOUtils.toByteArray(fileInputStream));
                Image image = new Image().withBytes(byteBuffer);
                // Add an external id to image
                String externalId = params[1];
                // Create index faces request
                IndexFacesRequest request = new IndexFacesRequest()
                        .withCollectionId(getString(R.string.default_collection_id))
                        .withImage(image)
                        .withExternalImageId(externalId)
                        .withDetectionAttributes(String.valueOf(Attribute.ALL));
                // Get result
                IndexFacesResult result = rekognitionClient.indexFaces(request);
                if (result != null) {
                    List<FaceRecord> faceRecords = result.getFaceRecords();
                    for (FaceRecord faceRecord : faceRecords) {
                        faceRecord.getFace().getFaceId();
                        faceRecord.getFace().getExternalImageId();
                        // Insert to Database
                    }
                    res = "Create new user successfully";
                    resultNotification = res;
                } else {
                    res = "Failure. Try again later";
                    resultNotification = res;
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            return res;
        }
    }
}
