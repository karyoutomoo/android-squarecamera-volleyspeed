package com.example.irfan.squarecamera;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.wonderkiln.camerakit.CameraKitError;
import com.wonderkiln.camerakit.CameraKitEvent;
import com.wonderkiln.camerakit.CameraKitEventListener;
import com.wonderkiln.camerakit.CameraKitImage;
import com.wonderkiln.camerakit.CameraKitVideo;
import com.wonderkiln.camerakit.CameraView;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.pedant.SweetAlert.SweetAlertDialog;

import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;

public class PredictActivity extends AppCompatActivity {

    long start;
    long end;
    String message;
    long probability;
    String validation;
    EditText etNrp;
    EditText etPassword;
    private static String TAG = CameradActivity.class.getSimpleName();
    private CameraView camerad;
    private CameraKitEventListener cameradListener;
    private Button btnCapture;
    private TextView tvHint;
    private TextView tvValidasi;
    String RESP;
    protected boolean pakaiKacamata;
    protected int counter = 0;
    private static String BASE_DIR = "camtest/";
    private String hint[];
    //private String nrp = "5115100007";
    private List<String> listPathFile;
    private ArrayList<String> encodedImagesList;
    protected SweetAlertDialog loadingDialog, errorDialog, successDialog;
    protected static String UPLOAD_URL = "http://etc.if.its.ac.id/doPredict/";
    private int requestCounter = 0;
    private boolean hasRequestFailed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_predict);
        init();

        cameradListener = new CameraKitEventListener() {
            @Override
            public void onEvent(CameraKitEvent cameraKitEvent) {

            }

            @Override
            public void onError(CameraKitError cameraKitError) {

            }

            @Override
            public void onImage(CameraKitImage cameraKitImage) {
                start = System.nanoTime();
                File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
                //File pictureFile = getFilesDir();

                if (pictureFile == null) {
                    Log.d(TAG, "Error creating media file, check storage permissions.");
                    return;
                }

                try {
                    FileOutputStream outStream = new FileOutputStream(pictureFile);
                    byte[] picture = cameraKitImage.getJpeg();
                    Bitmap result = BitmapFactory.decodeByteArray(picture, 0, picture.length);
                    result = Bitmap.createScaledBitmap(result, 512, 512, true);
                    result.compress(Bitmap.CompressFormat.JPEG, 100, outStream);
                    outStream.flush();
                    outStream.close();

                    counter++;

                    if (counter == 1) {
                        showLoadingDialog();
//                        closeLoadingDialog();
//                        showSuccessDialog();
                        if (getEncodedImage()) {
                            uploadFIle();
                        }
                    } else uploadFIle();

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {

                }
            }

            @Override
            public void onVideo(CameraKitVideo cameraKitVideo) {

            }
        };

        camerad = (CameraView) findViewById(R.id.camerad);
        camerad.addCameraKitListener(cameradListener);
        tvValidasi = (TextView) findViewById(R.id.tvValidasi);
        btnCapture = (Button) findViewById(R.id.btn_capture);
        btnCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                camerad.captureImage();
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        camerad.start();
    }

    @Override
    protected void onPause() {
        camerad.stop();
        super.onPause();
    }

    protected void init() {
        etNrp = findViewById(R.id.nrp);
        etPassword = findViewById(R.id.passwrd);
        listPathFile = new ArrayList<>();
        encodedImagesList = new ArrayList<>();
        tvHint = (TextView) findViewById(R.id.tvHint);

        pakaiKacamata = this.getIntent().getBooleanExtra("pakaiKacamata", false);
        //Dialog();
    }

    protected File getOutputMediaFile(int type) {
//        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
//              Environment.DIRECTORY_PICTURES), BASE_DIR + nrp);
        String nrp=etNrp.getText().toString();
        File folder = getFilesDir();
        File mediaStorageDir = new File(folder, BASE_DIR);

        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d(TAG, "sikemas: failed to create directory");
                return null;
            }
        }

        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            String filepath = mediaStorageDir.getPath() + File.separator + (pakaiKacamata ? "pakai_kacamata_" : "tidak_pakai_kacamata_") +
                    nrp + "_" + counter + ".png";
            listPathFile.add(filepath);
            mediaFile = new File(filepath);
        } else {
            return null;
        }

        return mediaFile;
    }

    protected boolean getEncodedImage() {
        loadingDialog.setTitleText("Encoding images");

        Bitmap image;
        ByteArrayOutputStream baos;
        byte[] byteArrayImage;
        String image_base64;

        for (String imagepath : listPathFile) {
            image = BitmapFactory.decodeFile(imagepath);
            baos = new ByteArrayOutputStream();
            image.compress(Bitmap.CompressFormat.JPEG, 100, baos); //bm is the bitmap object
            byteArrayImage = baos.toByteArray();
            image_base64 = Base64.encodeToString(byteArrayImage, Base64.DEFAULT);
            encodedImagesList.add(image_base64);
        }

        return true;
    }

    protected void uploadFIle() {
        loadingDialog.setTitleText("Uploading images");
        StringRequest stringRequest;
        for (int i = 0; i < encodedImagesList.size(); i++) {
            final int index = i;
            stringRequest = new StringRequest(Request.Method.POST, UPLOAD_URL,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            Toast.makeText(getApplicationContext(), "the response : " + response, Toast.LENGTH_LONG).show();
                            Log.d(TAG, "onResponse: "+response);
                            RESP = response;
//                            JSONObject obj = new JSONObject();
//                            message = obj.optString("message");
//                            probability = obj.optLong("probability");
//                            validation = obj.optString("validation");
//                            switch (message) {
//                                case "ok":
//                                    tvValidasi.setText("Hasil Prediksi : " + validation + "\ndengan Probability : " + probability);
//                                default:
//                                    tvValidasi.setText("GAGAL PREDIKSI");
//                            }
                            requestCounter--;
                            if (requestCounter == 0 && !hasRequestFailed) {
                                closeLoadingDialog();
                                showSuccessDialog();
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError volleyError) {
                            hasRequestFailed = true;
                            //Showing toast
                            Toast.makeText(PredictActivity.this, volleyError.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    }) {
                @Override
                protected Map<String, String> getParams() {
                    // Get encoded Image
                    String image = encodedImagesList.get(index);
                    Map<String, String> params = new HashMap<>();
                    String nrp=etNrp.getText().toString();
                    String password=etPassword.getText().toString();
                    // Adding parameters
                    if(nrp!=null && password !=null){

                        params.put("idUser", nrp);
                        params.put("password", password);
                        params.put("image", "data:/image/jpeg;base64," + image);
                    }
                    else Toast.makeText(getApplicationContext(),"Username atau Password belum diisi!",Toast.LENGTH_SHORT).show();

                    //returning parameters
                    return params;
                }
            };
            //Adding request to the queue
            requestCounter++;
            VolleySingleton.getInstance(getApplicationContext()).addToRequestQueue(stringRequest);
        }
    }

    protected void showHintDialog() {
        int hintNumber = getIntent().getIntExtra("counterCount", 0);
        tvHint.setText(hint[hintNumber]);
        new SweetAlertDialog(this)
                .setTitleText("Hint:")
                .setContentText(hint[hintNumber]);
        //.show();
    }

    protected void showLoadingDialog() {
        loadingDialog = new SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE);
        loadingDialog.setTitleText("Uploading Images");
        loadingDialog.setCancelable(false);
        loadingDialog.show();
    }

    protected void closeLoadingDialog() {
        loadingDialog.cancel();
    }

    protected void showSuccessDialog() {
//        end = System.nanoTime();
//        double waktuMulai = (double) start / 1000000000.0;
//        double waktuSelesai = (double) end / 1000000000.0;
//        double waktuUpload = waktuSelesai - waktuMulai;
//
//        String startTime = new DecimalFormat("#.##").format(waktuMulai);
//        String endTime = new DecimalFormat("#.##").format(waktuSelesai);
//        String elapsedTime = new DecimalFormat("#.##").format(waktuUpload);
//
//        Picture picture = new Picture(String.valueOf(getIntent().getIntExtra("counterCount", 0)),
//                startTime, endTime, elapsedTime);
//        //new DecimalFormat("#.##").format(timeElapsed)
        new SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
                .setTitleText("Success! Hasil Prediksi : ")
                .setContentText(RESP)
                .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sweetAlertDialog) {
                        setResult(Activity.RESULT_OK);
                        finish();
                    }
                })
                .show();
    }
}
