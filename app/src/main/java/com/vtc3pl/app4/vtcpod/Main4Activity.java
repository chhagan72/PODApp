package com.vtc3pl.app4.vtcpod;

import static android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION;
import static android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
import static androidx.core.content.FileProvider.getUriForFile;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
//import android.os.StrictMode;
import android.provider.MediaStore;
import android.util.Log;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.vision.Frame;
//import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
;

public class Main4Activity extends AppCompatActivity {
    static final int REQUEST_IMAGE_CAPTURE = 1;
    private String pictureImagePath = "";
    ProgressDialog dialog = null;
    public static final int RequestPermissionCode = 1;
    int serverResponseCode = 0;
    List<Integer> pos= new ArrayList<Integer>();
    List<Integer> posv= new ArrayList<Integer>();
    List<Integer> posb= new ArrayList<Integer>();
    String upLoadServerUri = "https://vtc3pl.com/uploadpod.php";
    String depo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main4);

//        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
//        StrictMode.setVmPolicy(builder.build());
        Button btnPhoto = findViewById(R.id.btnPhoto);
        Button btnUpload = findViewById(R.id.btnUpload);
        Button btnmUpload = findViewById(R.id.btnmUpload);
        Button btnmlrUpload = findViewById(R.id.btnmlrUpload);
        //TextView barcodeno= findViewById(R.id.barcodeno);
        depo = getIntent().getStringExtra("Depo");
        Log.d("Main4Activity", "Received Depo: " + depo);

        EnableRuntimePermission();

        btnPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    openCamera();
                } catch (Exception ex) {
                    Toast.makeText(getApplicationContext(), ex.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TextView barcodeno = findViewById(R.id.barcodeno);
                if (barcodeno.getText().toString().equals("Barcode not detected.")) {
                    Toast.makeText(getApplicationContext(), "Invalid Barcode! please take a photo again.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (pictureImagePath.isEmpty()) {
                    Toast.makeText(getApplicationContext(), "please take a photo.", Toast.LENGTH_SHORT).show();
                    return;
                }
                dialog = ProgressDialog.show(Main4Activity.this, "", "Uploading file...", true);
                Main4Activity.BackgroundWorker backgroundWorker = new Main4Activity.BackgroundWorker(Main4Activity.this);
                backgroundWorker.execute();
            }
        });

        btnmUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent myIntent = new Intent(Main4Activity.this, Main2Activity.class);
                myIntent.putExtra("Depo", depo);
                startActivity(myIntent);
            }
        });
        btnmlrUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent myIntent = new Intent(Main4Activity.this, PodLrwiseUpload.class);
                myIntent.putExtra("Depo", depo);
                startActivity(myIntent);
            }
        });
    }

    private void openCamera() {
        try {
            // Create image directory in app's internal files
            File imagePath = new File(getFilesDir(), "images");
            if (!imagePath.exists()) {
                imagePath.mkdirs();
            }

            // Create image file
            File file = new File(imagePath, "IMG_" + System.currentTimeMillis() + ".jpg");
            pictureImagePath = file.getAbsolutePath();

            Uri outputFileUri;
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            // For Android 7.0+ use FileProvider
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                outputFileUri = getUriForFile(
                        getApplicationContext(),
                        "com.vtc3pl.app4.vtcpod.fileprovider",
                        file
                );
                cameraIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                // For older devices use direct file URI
                outputFileUri = Uri.fromFile(file);
            }

            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);

            // Grant temporary permissions to camera app
            List<ResolveInfo> resolvedIntentActivities = getPackageManager()
                    .queryIntentActivities(cameraIntent, PackageManager.MATCH_DEFAULT_ONLY);
            for (ResolveInfo resolvedIntentInfo : resolvedIntentActivities) {
                String packageName = resolvedIntentInfo.activityInfo.packageName;
                grantUriPermission(packageName, outputFileUri,
                        FLAG_GRANT_WRITE_URI_PERMISSION | FLAG_GRANT_READ_URI_PERMISSION);
            }

            // Launch camera
            startActivityForResult(cameraIntent, REQUEST_IMAGE_CAPTURE);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error opening camera: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {

            TextView barcodeno = findViewById(R.id.barcodeno);
            ImageView imageView1 = findViewById(R.id.imageView1);

            File imgFile = new File(pictureImagePath);

            dialog = ProgressDialog.show(Main4Activity.this, "", "Scanning barcode...", true);
            dialog.setCancelable(false);

            if (imgFile.exists()) {

                Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                bitmap = scaleDown(bitmap, 1600, true); // increase resolution
                bitmap = enhanceBitmap(bitmap);

                // ✅ Configure scanner for ONLY barcodes (NO QR)
                BarcodeScannerOptions options =
                        new BarcodeScannerOptions.Builder()
                                .setBarcodeFormats(
                                        Barcode.FORMAT_CODE_128,
                                        Barcode.FORMAT_CODE_39,
                                        Barcode.FORMAT_CODE_93,
                                        Barcode.FORMAT_CODABAR,
                                        Barcode.FORMAT_EAN_13,
                                        Barcode.FORMAT_EAN_8,
                                        Barcode.FORMAT_ITF,
                                        Barcode.FORMAT_UPC_A,
                                        Barcode.FORMAT_UPC_E
                                )
                                .build();

                InputImage image = InputImage.fromBitmap(bitmap, 0);
                BarcodeScanner scanner = BarcodeScanning.getClient(options);

                // ⏳ Timeout Handler (5 sec)
                final boolean[] isDetected = {false};
                final boolean[] isTimeout = {false};

                new android.os.Handler().postDelayed(() -> {
                    if (!isDetected[0]) {
                        isTimeout[0] = true;

                        if (dialog != null && dialog.isShowing()) dialog.dismiss();

                        barcodeno.setText("Scan timeout. Please retake photo.");
                        Toast.makeText(getApplicationContext(),
                                "Barcode not clear. Move closer & retake.",
                                Toast.LENGTH_LONG).show();
                    }
                }, 5000);

                scanner.process(image)
                        .addOnSuccessListener(barcodes -> {

                            if (isTimeout[0]) return; // 🚀 STOP if timeout already happened

                            if (dialog != null && dialog.isShowing()) dialog.dismiss();

                            if (!barcodes.isEmpty()) {

                                isDetected[0] = true;

                                Barcode barcode = barcodes.get(0);

                                if (barcode.getFormat() == Barcode.FORMAT_QR_CODE) {
                                    barcodeno.setText("QR Code not allowed");
                                    return;
                                }

                                String value = barcode.getRawValue();
                                barcodeno.setText(value);

                            } else {
                                barcodeno.setText("Barcode not detected.");
                            }
                        })
                        .addOnFailureListener(e -> {
                            if (dialog != null && dialog.isShowing()) dialog.dismiss();
                            barcodeno.setText("Scan failed");
                        });

                // 🖼 Show Image
                imageView1.setVisibility(View.VISIBLE);
                imageView1.setImageBitmap(bitmap);

                // 💾 Compress & Save Image
                try {
                    FileOutputStream fos = new FileOutputStream(imgFile);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos);
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private Bitmap enhanceBitmap(Bitmap bitmap) {
        Bitmap enhanced = Bitmap.createScaledBitmap(bitmap,
                bitmap.getWidth(),
                bitmap.getHeight(),
                true);

        android.graphics.ColorMatrix cm = new android.graphics.ColorMatrix();
        cm.set(new float[]{
                1.5f, 0, 0, 0, 0,
                0, 1.5f, 0, 0, 0,
                0, 0, 1.5f, 0, 0,
                0, 0, 0, 1, 0
        });

        android.graphics.Paint paint = new android.graphics.Paint();
        paint.setColorFilter(new android.graphics.ColorMatrixColorFilter(cm));

        android.graphics.Canvas canvas = new android.graphics.Canvas(enhanced);
        canvas.drawBitmap(enhanced, 0, 0, paint);

        return enhanced;
    }

    public Bitmap scaleDown(Bitmap realImage, float maxImageSize, boolean filter) {
        if (realImage.getWidth() < realImage.getHeight()) {
            Matrix matrix = new Matrix();
            matrix.postRotate(-90);
            realImage = Bitmap.createBitmap(realImage, 0, 0, realImage.getWidth(), realImage.getHeight(), matrix, true);
        }
        float ratio = Math.min((float) maxImageSize / realImage.getWidth(),
                (float) maxImageSize / realImage.getHeight());
        if (ratio > 1)
            ratio = 1;
        int width = Math.round((float) ratio * realImage.getWidth());
        int height = Math.round((float) ratio * realImage.getHeight());

        Bitmap newBitmap = Bitmap.createScaledBitmap(realImage, width, height, filter);
        return newBitmap;
    }

    public void EnableRuntimePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(Main4Activity.this,
                Manifest.permission.CAMERA)) {
        } else {
            ActivityCompat.requestPermissions(Main4Activity.this, new String[]{
                    Manifest.permission.CAMERA}, RequestPermissionCode);
        }
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    class GetJSON extends AsyncTask<String, Void, String> {
        //this method will be called before execution
        //you can display a progress bar or something
        //so that user can understand that he should wait
        //as network operation may take some time
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        //this method will be called after execution
        //so here we are displaying a toast with the json string
        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            //Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
            try {
                loadIntoListview(s);
                //Toast.makeText(getApplicationContext(),"LR No. Loaded in the List.", Toast.LENGTH_SHORT).show();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        //in this method we are fetching the json string
        @Override
        protected String doInBackground(String... params)
        {
            String drsno = params[0];
            //String weburl = "http://subcranial-minuses.000webhostapp.com/vtc3pl/getlrno.php";
            String weburl = "https://vtc3pl.com/getlrno.php";
            try {
                URL url = new URL(weburl);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.setDoOutput(true);
                httpURLConnection.setDoInput(true);
                OutputStream outputStream = httpURLConnection.getOutputStream();
                BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8"));
                String post_data = URLEncoder.encode("drsno", "UTF-8") + "=" + URLEncoder.encode(drsno, "UTF-8"); //+ "&"
                //+ URLEncoder.encode("password", "UTF-8") + "=" + URLEncoder.encode(drsno, "UTF-8");
                bufferedWriter.write(post_data);
                bufferedWriter.flush();
                bufferedWriter.close();
                outputStream.close();
                InputStream inputStream = httpURLConnection.getInputStream();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "iso-8859-1"));
                String result = "";
                String line = "";
                while ((line = bufferedReader.readLine()) != null) {
                    result += line;
                }
                bufferedReader.close();
                inputStream.close();
                httpURLConnection.disconnect();
                return result;
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }
    }

    public class BackgroundWorker extends AsyncTask<String, Void, String> {
        Context context;
        AlertDialog.Builder alertDialog;

        BackgroundWorker(Context ctx) {
            context = ctx;
        }

        @Override
        protected String doInBackground(String... params) {
            //String result="Photo Not Uploaded. Try again.";
            String result = "";
            //EditText editText1 = findViewById(R.id.editText1);
            TextView barcodeno = findViewById(R.id.barcodeno);
            File sourceFile = new File(pictureImagePath);
            String fileName = barcodeno.getText().toString() + ".jpg";
            HttpURLConnection conn = null;
            DataOutputStream dos = null;
            String lineEnd = "\r\n";
            String twoHyphens = "--";
            String boundary = "*****";
            int bytesRead, bytesAvailable, bufferSize;
            byte[] buffer;
            int maxBufferSize = 1024 * 1024;

            if (!sourceFile.isFile()) {
                if (dialog != null && dialog.isShowing()) {
                    dialog.dismiss();
                }
                Log.e("uploadFile", "Source File not exist :" + pictureImagePath);
                //textView1.append("Source File not exist :" + fileName);
            } else {
                try {
                    // open a URL connection to the Servlet
                    FileInputStream fileInputStream = new FileInputStream(sourceFile);
                    URL url = new URL(upLoadServerUri);
                    // Open a HTTP  connection to  the URL
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setDoInput(true); // Allow Inputs
                    conn.setDoOutput(true); // Allow Outputs
                    conn.setUseCaches(false); // Don't use a Cached Copy
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Connection", "Keep-Alive");
                    conn.setRequestProperty("ENCTYPE", "multipart/form-data");
                    conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                    conn.setRequestProperty("uploaded_file", fileName);

                    dos = new DataOutputStream(conn.getOutputStream());

                    dos.writeBytes(twoHyphens + boundary + lineEnd);
                    dos.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\";filename=\""
                            + fileName + "\"" + lineEnd);

                    dos.writeBytes(lineEnd);

                    // create a buffer of  maximum size
                    bytesAvailable = fileInputStream.available();

                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    buffer = new byte[bufferSize];

                    // read file and write it into form...
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                    while (bytesRead > 0) {
                        dos.write(buffer, 0, bufferSize);
                        bytesAvailable = fileInputStream.available();
                        bufferSize = Math.min(bytesAvailable, maxBufferSize);
                        bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                    }
                    // send multipart form data necesssary after file data...
                    dos.writeBytes(lineEnd);
                    dos.flush();

                    // Upload POST Data
                    dos.writeBytes(twoHyphens + boundary + lineEnd);
                    dos.writeBytes("Content-Disposition: form-data; name=\"image_name\"" + lineEnd);
                    dos.writeBytes("Content-Type: text/plain; charset=UTF-8" + lineEnd);
                    dos.writeBytes(lineEnd);
                    dos.writeBytes(fileName.substring(0, fileName.length() - 4));
                    dos.writeBytes(lineEnd);

                    dos.flush();
                    dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                    dos.flush();
                    dos.close();
                    // Responses from the server (code and message)
                    serverResponseCode = conn.getResponseCode();
                    String serverResponseMessage = conn.getResponseMessage();

                    Log.i("uploadFile", "HTTP Response is : "
                            + serverResponseMessage + ": " + serverResponseCode);

                    InputStream inputStream = conn.getInputStream();
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "iso-8859-1"));
                    String line = "";
                    while ((line = bufferedReader.readLine()) != null) {
                        result += line;
                    }
                    bufferedReader.close();
                    inputStream.close();
                    conn.disconnect();
                    /*if(serverResponseCode==200)
                    {
                        result = serverResponseMessage;
                     //result="Successfully Uploaded Photo.";
                    }*/
                    //close the streams //
                    fileInputStream.close();
                } catch (MalformedURLException ex) {
                    if (dialog != null && dialog.isShowing()) {
                        dialog.dismiss();
                    }
                    ex.printStackTrace();
                    Log.e("Upload file to server", "error: " + ex.getMessage(), ex);
                } catch (Exception e) {
                    if (dialog != null && dialog.isShowing()) {
                        dialog.dismiss();
                    }
                    e.printStackTrace();
                }
                if (dialog != null && dialog.isShowing()) {
                    dialog.dismiss();
                }
                Log.i("program", "program end.");

            } // End else block
            return result;
        }

        @Override
        protected void onPreExecute() {
            alertDialog = new AlertDialog.Builder(context);
            alertDialog.setTitle("Upload Status");
            alertDialog.setPositiveButton("OK", null);
        }

        @Override
        protected void onPostExecute(String result) {
            TextView barcodeno = findViewById(R.id.barcodeno);
            TextView drsno = findViewById(R.id.drsno);
            ImageView imageView1 = findViewById(R.id.imageView1);
            alertDialog.setMessage(result);
            alertDialog.show();
            if (result.contains("Successfully Uploaded Photo")) {
                drsno.setText("DRS No. " + result.substring(44));
                Main4Activity.GetJSON getJSON = new Main4Activity.GetJSON();
                getJSON.execute(result.substring(44));
                //Toast.makeText(getApplicationContext(), result.substring(44), Toast.LENGTH_SHORT).show();
                pictureImagePath = "";
                imageView1.setImageDrawable(null);
                barcodeno.setText("");
            }
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }
    }

    private void loadIntoListview(String json) throws JSONException {
        //creating a json array from the json string
        JSONArray jsonArray = new JSONArray(json);

        if(jsonArray.length()==0)
        {
            Toast.makeText(getApplicationContext(), "DRS NO. not found.", Toast.LENGTH_SHORT).show();
            return;
        }

        ListView listview1 = findViewById(R.id.listview1);
        listview1.setAdapter(null);
        pos.clear();
        posv.clear();
        //creating a string array for listview
        String[] LRNO = new String[jsonArray.length()];

        //looping through all the elements in json array
        for (int i = 0; i < jsonArray.length(); i++) {

            //getting json object from the json array
            JSONObject obj = jsonArray.getJSONObject(i);

            //getting the name from the json object and putting it inside string array
            LRNO[i] = obj.getString("lrno");
            if(obj.getInt("uploaded") == 1)
                pos.add(i);
            if(obj.getInt("verified") == 1)
                posv.add(i);
            if(obj.getInt("verified") == 2)
                posb.add(i);
        }

        //the array adapter to load data into list
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(Main4Activity.this, android.R.layout.simple_list_item_1, LRNO)
        {
            @Override
            public boolean isEnabled(int position){
                if(posv.contains(position))
                {
                    // Disable the second item from Spinner
                    return false;
                }
                else
                {
                    return true;
                }
            }

            @Override
            public View getView(int position, View convertView,
                                        ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView tv = (TextView) view;
                tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP,20);
                /*LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                params.setMargins(0,0,0,0);
                tv.setPadding(0,0,0,0);
                tv.setLayoutParams(params);*/
                if(posv.contains(position)) {
                    // Set the disable item(Verified LR) text color
                    tv.setTextColor(Color.GREEN);
                }
                else if(posb.contains(position)) {
                    // Set the bad LR item text color
                    tv.setTextColor(Color.RED);
                }
                else if(pos.contains(position)) {
                    // Set the uploaded item text color
                    tv.setTextColor(Color.BLUE);
                }
                else {
                    tv.setTextColor(Color.BLACK);
                }
                return view;
            }
        };
        //attaching adapter to listview
        listview1.setAdapter(arrayAdapter);
        //Toast.makeText(getApplicationContext(),"LR No. Loaded in the List.", Toast.LENGTH_SHORT).show();
    }
}