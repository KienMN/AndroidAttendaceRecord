package com.example.kienmaingoc.attendancerecord;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.rekognition.AmazonRekognitionClient;
import com.amazonaws.services.rekognition.model.CompareFacesRequest;
import com.amazonaws.services.rekognition.model.CreateCollectionRequest;
import com.amazonaws.services.rekognition.model.CreateCollectionResult;
import com.amazonaws.services.rekognition.model.DeleteCollectionRequest;
import com.amazonaws.services.rekognition.model.DeleteCollectionResult;
import com.amazonaws.services.rekognition.model.Face;
import com.amazonaws.services.rekognition.model.Image;
import com.amazonaws.services.rekognition.model.ListFacesRequest;
import com.amazonaws.services.rekognition.model.ListFacesResult;
import com.amazonaws.services.rekognition.model.SearchFacesByImageRequest;
import com.amazonaws.services.rekognition.model.SearchFacesByImageResult;
import com.amazonaws.services.rekognition.model.SearchFacesRequest;
import com.amazonaws.util.IOUtils;

import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class SettingActivity extends AppCompatActivity {

    final int LIST_FACE_REQUEST = 1;
    final int DELETE_FACE_COLLECTION_REQUEST = 2;
    final int CREATE_FACE_COLLECTION_REQUEST = 3;
    final int DELETE_FACE_REQUEST = 4;
    final static int SEARCH_FACE_REQUEST = 5;
    final static int RESULT_OK = 200;
    final static int RESULT_FAILURE = 0;

    static ArrayList<String> facesInCollection = new ArrayList<>();

    ProgressBar spinner;
    Button listFacesButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        spinner = (ProgressBar) findViewById(R.id.spinner);
        spinner.setVisibility(View.INVISIBLE);

        listFacesButton = (Button) findViewById(R.id.listFacesButton);
    }

    private class RekognitionProcess extends AsyncTask<String, Void, Integer> {

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
            spinner.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onPostExecute(Integer integer) {
            spinner.setVisibility(View.INVISIBLE);
        }

        @Override
        protected Integer doInBackground(String... params) {
            Integer requestCode = Integer.valueOf(params[0]);

            if (requestCode.equals(LIST_FACE_REQUEST)) {
                facesInCollection.clear();
                ListFacesRequest request = new ListFacesRequest().withCollectionId(getString(R.string.default_collection_id));
                ListFacesResult result = rekognitionClient.listFaces(request);
                if (result != null) {

                    Log.i("result", result.toString());
                    List<Face> faces = result.getFaces();
                    for (Face face : faces) {
                        facesInCollection.add(face.getExternalImageId());
                    }
                    Intent i = new Intent(getApplicationContext(), ListFacesActivity.class);
                    startActivity(i);
                    return RESULT_OK;
                }
            }

            else if (requestCode.equals(DELETE_FACE_COLLECTION_REQUEST)) {
                DeleteCollectionResult result = null;
                try {
                    DeleteCollectionRequest request = new DeleteCollectionRequest().withCollectionId(getString(R.string.default_collection_id));
                    result = rekognitionClient.deleteCollection(request);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (result != null) {
                    return RESULT_OK;
                }
            }

            else if (requestCode.equals(CREATE_FACE_COLLECTION_REQUEST)) {
                CreateCollectionRequest request = new CreateCollectionRequest().withCollectionId(getString(R.string.default_collection_id));
                CreateCollectionResult result = rekognitionClient.createCollection(request);
                if (result != null) {
                    return RESULT_OK;
                }
            }

            else if (requestCode.equals(SEARCH_FACE_REQUEST)) {
                String imagePath = params[1];
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
                        return RESULT_OK;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return RESULT_FAILURE;
        }
    }

    public void listFaces(View view) {
        RekognitionProcess process = new RekognitionProcess();
        process.execute(String.valueOf(LIST_FACE_REQUEST));
    }

    public void deleteCollection(View view) {
        RekognitionProcess process = new RekognitionProcess();
        try {
            Integer resultCode = process.execute(String.valueOf(DELETE_FACE_COLLECTION_REQUEST)).get();
            if (resultCode.equals(RESULT_OK)) {
                Toast.makeText(getApplicationContext(), "Successful!", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getApplicationContext(), "Failure!", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void createCollection(View view) {
        RekognitionProcess process = new RekognitionProcess();
        try {
            Integer resultCode = process.execute(String.valueOf(CREATE_FACE_COLLECTION_REQUEST)).get();
            if (resultCode.equals(RESULT_OK)) {
                Toast.makeText(getApplicationContext(), "Successful!", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getApplicationContext(), "Failure!", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void deleteFace(View view) {
        Toast.makeText(getApplicationContext(), "This feature is being built", Toast.LENGTH_LONG).show();
    }
}
