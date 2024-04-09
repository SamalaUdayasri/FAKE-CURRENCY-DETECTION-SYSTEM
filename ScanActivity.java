package com.fake.currency.app;

import androidx.appcompat.app.AppCompatActivity;
import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.fake.currency.app.Retrofit.IUploadApi;
import com.fake.currency.app.Retrofit.RetrofitClient;
import com.fake.currency.app.Utils.Common;
import com.fake.currency.app.Utils.IUploadCallbacks;
import com.fake.currency.app.Utils.ProgressRequestBody;
import com.itextpdf.text.BadElementException;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.PdfWriter;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;

public class ScanActivity extends AppCompatActivity implements IUploadCallbacks{

    ImageView imageView,btnUpload;
    IUploadApi mService;
    Uri selectedFileUri;
    ProgressDialog dialog;

    TextView ResultTxt;

    ImageView BackBtn;

    private IUploadApi getApiUpload()
    {
        return RetrofitClient.getClient().create(IUploadApi.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        mService = getApiUpload();

        ResultTxt = findViewById(R.id.resulttxt);

        BackBtn = findViewById(R.id.backbtn);

        imageView = (ImageView)findViewById(R.id.image_view);
        btnUpload = (ImageView)findViewById(R.id.button_upload);

        Uri image_uri = getIntent().getData();
        imageView.setImageURI(image_uri);
        selectedFileUri = image_uri;

        BackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(ScanActivity.this, HomeActivity.class);
                startActivity(i);
            }
        });

        btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                uploadFile();
            }
        });



    }

    private void uploadFile() {
        // Now here we check if we have the URI of the file or not
        if (selectedFileUri != null) {
            dialog = new ProgressDialog(ScanActivity.this);
            dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            dialog.setMessage("Uploading..");
            dialog.setIndeterminate(false);
            dialog.setMax(100);
            dialog.setCancelable(false);
            dialog.show();

            // Load actual file from URI
            File file = null;
            try {
                file = new File(Common.getFilePath(this, selectedFileUri));
                long fileSizeInBytes = file.length();

                // Decode the file to a bitmap
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(file.getAbsolutePath(), options);
                int imageHeight = options.outHeight;
                int imageWidth = options.outWidth;

                // Determine the new dimensions
                int newWidth = imageWidth / 4; // Reduce width to half
                int newHeight = imageHeight / 4; // Reduce height to half

                // Decode the file to a resized bitmap
                options.inJustDecodeBounds = false;
                options.inSampleSize = calculateInSampleSize(options, newWidth, newHeight);
                Bitmap resizedBitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), options);

                // Compress the resized image
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream); // Adjust compression quality as needed
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(outputStream.toByteArray());
                fos.flush();
                fos.close();
            } catch (URISyntaxException | IOException e) {
                e.printStackTrace();
            }

            if (file != null) {
                final ProgressRequestBody requestBody = new ProgressRequestBody(this, file);

                final MultipartBody.Part body = MultipartBody.Part.createFormData("image", file.getName(), requestBody);

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        mService.uploadFile(body)
                                .enqueue(new Callback<String>() {
                                    @Override
                                    public void onResponse(Call<String> call, Response<String> response) {
                                        JSONObject jsonResponse = null;
                                        try {
                                            jsonResponse = new JSONObject(response.body());
                                            String result = jsonResponse.getString("result");
                                            ResultTxt.setText("Result: " + result);
                                            BackBtn.setVisibility(View.VISIBLE);
                                            btnUpload.setVisibility(View.INVISIBLE);
                                            Toast.makeText(ScanActivity.this, "" + result, Toast.LENGTH_SHORT).show();
                                        } catch (JSONException e) {
                                            throw new RuntimeException(e);
                                        }
                                        dialog.dismiss();
                                    }

                                    @Override
                                    public void onFailure(Call<String> call, Throwable t) {
                                        Toast.makeText(ScanActivity.this, "" + t.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                }).start();
            }
        } else {
            Toast.makeText(this, "Cannot upload this file..", Toast.LENGTH_SHORT).show();
        }
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 4;
            final int halfWidth = width / 4;

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 4;
            }
        }

        return inSampleSize;
    }

    @Override
    public void onProgressUpdate(int percent) {

    }

    @Override
    public void onBackPressed() {

    }
}
