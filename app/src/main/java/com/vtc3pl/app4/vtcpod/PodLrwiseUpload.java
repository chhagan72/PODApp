package com.vtc3pl.app4.vtcpod;

import android.Manifest;
import android.app.DatePickerDialog;
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
import android.os.StrictMode;
import android.provider.MediaStore;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import android.text.Editable;
import static android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION;
import static android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
import static androidx.core.content.FileProvider.getUriForFile;

public class PodLrwiseUpload extends AppCompatActivity {
    public static final int RequestPermissionCode = 100;
    static final int REQUEST_IMAGE_CAPTURE = 101;
    static final int REQUEST_IMAGE_PICK = 102;
    ProgressDialog dialog = null;
    int serverResponseCode = 0;
    List<Integer> pos = new ArrayList<Integer>();
    List<Integer> posv = new ArrayList<Integer>();
    List<Integer> posb = new ArrayList<Integer>();
    private String upLoadServerUri = "https://vtc3pl.com/Upload_POD.php";
    private String pictureImagePath = "", drsno = "";
    private OkHttpClient client;
    private Map<String, String> drsdtMap = new HashMap<>();
    private Map<String, String> deliveryDateMap = new HashMap<>();
    private Map<String, String> drsnoMap = new HashMap<>();
    private EditText edtDeliveryDate;
    private ImageView imgCal;
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    HashMap<String, Integer> approveStatusMap = new HashMap<>();
    private AutoCompleteTextView editTextLR;
    private Button btnGetLR;

    private ArrayAdapter<String> lrAdapter;
    private List<String> lrList = new ArrayList<>();
    private ProgressDialog loader;
    private android.os.Handler searchHandler = new android.os.Handler();
    private Runnable searchRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.podlrwiseupload);
        Intent intent1 = getIntent();
        String selectedDepo = intent1.getStringExtra("Depo");

        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        Button btn1 = findViewById(R.id.btn1);

        Button uploadfromfile = findViewById(R.id.btnUploadFile);
        if (selectedDepo != null && selectedDepo.toUpperCase().startsWith("BWD")) {
            uploadfromfile.setEnabled(true);
            uploadfromfile.setAlpha(1.0f);
        } else {
            uploadfromfile.setEnabled(false);
            uploadfromfile.setAlpha(0.4f);
        }

        Button btn2 = findViewById(R.id.btn2);
        Button btnaUpload = findViewById(R.id.btnaUpload);

        EnableRuntimePermission();

        Calendar calendar = Calendar.getInstance();
        int currentMonth = calendar.get(Calendar.MONTH);
        Log.e("CurrentMonth", String.valueOf(Calendar.MONTH));

        client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
        fetchDepoData();

        final Spinner spinner1 = findViewById(R.id.spinner1);
        spinner1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

                Object sel = spinner1.getSelectedItem();
                if (sel == null) {
                    edtDeliveryDate.setText("");
                    return;
                }
                String lrno = sel.toString();
                String ddate = deliveryDateMap.get(lrno);
                if (ddate == null) ddate = "";
                edtDeliveryDate.setText(ddate);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                edtDeliveryDate.setText("");
            }
        });

        editTextLR = findViewById(R.id.editTextLR);
        btnGetLR = findViewById(R.id.btnGetLR);

        lrAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line,
                lrList);

        editTextLR.setAdapter(lrAdapter);
//        editTextLR.setThreshold(1);

        edtDeliveryDate = findViewById(R.id.edtDeliveryDate);
        imgCal = findViewById(R.id.imgCal);

        Spinner spinner_2 = findViewById(R.id.spinner1);
        spinner_2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                String lrno = spinner_2.getSelectedItem().toString();
                String drsDateStr = drsdtMap.get(lrno);
                String deliveryDateStr = deliveryDateMap.get(lrno);

                Integer approveStatus = approveStatusMap.get(lrno);
                if (approveStatus == null) approveStatus = 0;

                boolean deliveryAvailable = deliveryDateStr != null &&
                        !deliveryDateStr.trim().equals("") &&
                        !deliveryDateStr.equals("0000-00-00") &&
                        !deliveryDateStr.equalsIgnoreCase("null");

                boolean isOlderThan4Days = false;

                if (drsDateStr != null && !drsDateStr.trim().equals("")) {
                    try {
                        Date drsDate = sdf.parse(drsDateStr);
                        long diff = new Date().getTime() - drsDate.getTime();
                        long days = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);
                        isOlderThan4Days = (days > 4);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                if (approveStatus == 1) {
                    enableField();
                    if (deliveryAvailable)
                        edtDeliveryDate.setText(deliveryDateStr);
                    else
                        edtDeliveryDate.setText("");
                    return;   // IMPORTANT: stop further checks
                }

                // ✔ RULE 2: If DeliveryDate exists → enable and set date
                if (deliveryAvailable) {
                    enableField();
                    edtDeliveryDate.setText(deliveryDateStr);
                }
                else {
                    // ✔ RULE 3: If older than 4 days → disable
                    if (isOlderThan4Days) {
                        disableField();
                        edtDeliveryDate.setText("");
                    }
                    else {
                        // ✔ RULE 4: Otherwise enable
                        enableField();
                        edtDeliveryDate.setText("");
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        uploadfromfile.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(
                    Intent.createChooser(intent, "Select Image"),
                    REQUEST_IMAGE_PICK
            );
        });

        imgCal.setOnClickListener(v -> {
            Object sel = spinner1.getSelectedItem();
            if (sel == null) {
                Toast.makeText(PodLrwiseUpload.this, "Please select LR first", Toast.LENGTH_SHORT).show();
                return;
            }
            String lrno = sel.toString();
            String drsdtStr = drsdtMap.get(lrno);
            if (drsdtStr == null || drsdtStr.equals("")) {
                Toast.makeText(PodLrwiseUpload.this, "No DRS date available for this LR", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                Date drsdt = sdf.parse(drsdtStr);
                Calendar minCal = Calendar.getInstance();
                minCal.setTime(drsdt);
                Calendar maxCal = Calendar.getInstance();
                maxCal.setTime(drsdt);
                maxCal.add(Calendar.DATE, 3);

                Calendar today = Calendar.getInstance();

                int y = today.get(Calendar.YEAR);
                int m = today.get(Calendar.MONTH);
                int d = today.get(Calendar.DAY_OF_MONTH);

                DatePickerDialog dpd = new DatePickerDialog(PodLrwiseUpload.this, (DatePicker view, int year, int month, int dayOfMonth) -> {
                    Calendar chosen = Calendar.getInstance();
                    chosen.set(year, month, dayOfMonth, 0, 0, 0);
                    Date chosenDate = chosen.getTime();

                    if (chosenDate.before(minCal.getTime()) || chosenDate.after(maxCal.getTime())) {
                        Toast.makeText(PodLrwiseUpload.this, "Allowed date must be between " + sdf.format(minCal.getTime()) + " and " + sdf.format(maxCal.getTime()), Toast.LENGTH_LONG).show();
                        return;
                    }
                    String selected = sdf.format(chosenDate);
                    edtDeliveryDate.setText(selected);

                    String drsnoForLR = drsnoMap.get(lrno);
                    if (drsnoForLR == null) drsnoForLR = "";
                    if (drsnoForLR.equals("")) {
                        // try fallback to class-level drsno (if user used DRS construct button)
                        if (drsno != null && !drsno.equals("")) {
                            drsnoForLR = drsno;
                        }
                    }

                    if (drsnoForLR.equals("")) {
                        Toast.makeText(PodLrwiseUpload.this, "DRS number is not set. Please get LR list first.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // call server to update delivery date (async)
                    updateDeliveryDateOnServerAsync(lrno, drsnoForLR, selected);
                }, y, m, d);

                dpd.getDatePicker().setMinDate(minCal.getTimeInMillis());
                dpd.getDatePicker().setMaxDate(maxCal.getTimeInMillis());
                dpd.show();

            } catch (ParseException e) {
                e.printStackTrace();
                Toast.makeText(PodLrwiseUpload.this, "Invalid DRS date format.", Toast.LENGTH_SHORT).show();
            }
        });

        btn1.setOnClickListener(view -> {
            try {
                openCamera();
            } catch (Exception ex) {
                Toast.makeText(getApplicationContext(), ex.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        btn2.setOnClickListener(view -> {
            Spinner spinner_1 = findViewById(R.id.spinner1);
            if (spinner_1.getSelectedItem() == null) {
                Toast.makeText(getApplicationContext(), "Please Select LR No.", Toast.LENGTH_SHORT).show();
                return;
            }

            String lrno = spinner_1.getSelectedItem().toString();

            Integer approveStatus = approveStatusMap.get(lrno);
            if (approveStatus == null) approveStatus = 0;

            String drsDateStr = drsdtMap.get(lrno);
            String deliveryDateStr = deliveryDateMap.get(lrno);

            boolean deliveryAvailable = false;
            if (deliveryDateStr != null) {
                String d = deliveryDateStr.trim();
                if (!d.equals("") && !d.equals("0000-00-00") && !d.equalsIgnoreCase("null")) {
                    deliveryAvailable = true;
                }
            }

            boolean isOlderThan4Days = false;
            if (!deliveryAvailable) {
                if (drsDateStr != null && !drsDateStr.trim().equals("")) {
                    try {
                        Date drsDate = sdf.parse(drsDateStr);

                        long diff = new Date().getTime() - drsDate.getTime();
                        long days = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);

                        if (days > 4) {
                            isOlderThan4Days = true;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                if (isOlderThan4Days && approveStatus == 0 ) {
                    new AlertDialog.Builder(PodLrwiseUpload.this)
                            .setTitle("Approval Required")
                            .setMessage("DRS क्रमांकाची तारीख ४ दिवसांपेक्षा जुनी आहे. तुम्ही POD अपलोड करू शकणार नाही. POD अपलोड करण्यासाठी कृपया राजेश पवार सर यांची मंजुरी (approval) घ्या आणि त्यानंतरच अपलोड करा.")
                            .setPositiveButton("OK", null)
                            .show();
                    return;
                }
            }

            if (approveStatus == 2) {
                new AlertDialog.Builder(PodLrwiseUpload.this)
                        .setTitle("Upload Rejected")
                        .setMessage("हा DRS क्रमांक नाकारलेला (Rejected) आहे. तुम्ही POD अपलोड करू शकत नाही. कृपया राजेश पवार सरांशी संपर्क साधा.")
                        .setPositiveButton("OK", null)
                        .show();
                return;
            }

            if (posv.contains(spinner_1.getSelectedItemPosition())) {
                Toast.makeText(getApplicationContext(), "LRNo image is already verified. Please select other LRNo.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (pictureImagePath.equals("")) {
                Toast.makeText(getApplicationContext(), "please take a photo.", Toast.LENGTH_SHORT).show();
                return;
            }

            dialog = new ProgressDialog(PodLrwiseUpload.this);
            dialog.setMessage("Uploading POD...");
            dialog.setCancelable(false);
            dialog.show();
            PodLrwiseUpload.BackgroundWorker backgroundWorker = new PodLrwiseUpload.BackgroundWorker(PodLrwiseUpload.this);
            backgroundWorker.execute();
        });

        btnaUpload.setOnClickListener(view -> {
            Intent myIntent = new Intent(PodLrwiseUpload.this, Main4Activity.class);
            startActivity(myIntent);
        });

        editTextLR.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }

                searchRunnable = () -> {

                    String term = s.toString().trim();

                    // only search when 4+ characters typed
                    if (term.length() >= 3) {
                        searchLRFromServer(term);
                    }

                };

                // wait 500ms after typing stops
                searchHandler.postDelayed(searchRunnable, 500);

            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        editTextLR.setOnItemClickListener((parent, view, position, id) -> {

            String lrno = parent.getItemAtPosition(position).toString();

            editTextLR.setText(lrno);
            editTextLR.setSelection(lrno.length());
            editTextLR.dismissDropDown();

            fetchLRDetails(lrno);
        });

        btnGetLR.setOnClickListener(v -> {

            String lrno = editTextLR.getText().toString().trim();

            if (lrno.length() < 3) {
                Toast.makeText(PodLrwiseUpload.this, "Enter valid LR No", Toast.LENGTH_SHORT).show();
                return;
            }

            lrno = lrno.toUpperCase(); // IMPORTANT
            fetchLRDetails(lrno);

        });
    }
    // PROPER ENABLE / DISABLE FUNCTIONS
    private void enableField() {
        edtDeliveryDate.setEnabled(true);
        edtDeliveryDate.setFocusable(true);
        edtDeliveryDate.setFocusableInTouchMode(true);
        edtDeliveryDate.setClickable(true);

        imgCal.setEnabled(true);
        imgCal.setClickable(true);
    }

    private void disableField() {
        edtDeliveryDate.setEnabled(false);
        edtDeliveryDate.setFocusable(false);
        edtDeliveryDate.setFocusableInTouchMode(false);
        edtDeliveryDate.setClickable(false);

        imgCal.setEnabled(false);
        imgCal.setClickable(false);
    }

    private void searchLRFromServer(String term) {
        lrList.clear();
        if (loader == null) {
            loader = new ProgressDialog(PodLrwiseUpload.this);
            loader.setMessage("Searching LR...");
            loader.setCancelable(false);
        }

        Log.e("LR_SEARCH", term);
        if (!loader.isShowing()) {
            loader.show();
        }
        String url = "https://vtc3pl.com/search_lrnoPOD.php?term=" + Uri.encode(term);
        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {

                runOnUiThread(() -> {
                    if (loader != null && loader.isShowing())
                        loader.dismiss();

                    Toast.makeText(PodLrwiseUpload.this,
                            "Search failed",
                            Toast.LENGTH_SHORT).show();
                });

            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {

                if (response.isSuccessful() && response.body() != null) {

                    try {

                        JSONArray arr = new JSONArray(response.body().string());

                        lrList.clear();

                        for (int i = 0; i < arr.length(); i++) {
                            lrList.add(arr.getString(i));
                        }

                        runOnUiThread(() -> {

                            if (loader != null && loader.isShowing())
                                loader.dismiss();

                            lrAdapter.clear();
                            lrAdapter.addAll(lrList);
                            lrAdapter.notifyDataSetChanged();

                            editTextLR.showDropDown();
                        });

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void fetchLRDetails(String lrno){

        RequestBody formBody = new FormBody.Builder()
                .add("lrno", lrno.trim())
                .build();

        Request request = new Request.Builder()
                .url("https://vtc3pl.com/getlrno_pod_by_lr.php")
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {

                runOnUiThread(() ->
                        Toast.makeText(PodLrwiseUpload.this,
                                "Failed to load LR : " + e.getMessage(),
                                Toast.LENGTH_LONG).show());

            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {

                if(response.body()==null){
                    runOnUiThread(() ->
                            Toast.makeText(PodLrwiseUpload.this,
                                    "Server returned empty response",
                                    Toast.LENGTH_LONG).show());
                    return;
                }

                String result = response.body().string();

                Log.e("LR_RESPONSE", result); // DEBUG

                runOnUiThread(() -> {

                    try {

                        if(result == null || result.trim().equals("") || result.trim().equals("[]")){
                            Toast.makeText(PodLrwiseUpload.this,
                                    "LR not found",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        loadIntoSpinner(result);

                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(PodLrwiseUpload.this,
                                "JSON Parse Error",
                                Toast.LENGTH_LONG).show();
                    }

                });

            }

        });

    }

    private void fetchDepoData() {
        Request request = new Request.Builder()
                .url("https://vtc3pl.com/fetch_depotcode_and_names_for_pod_app.php")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> Toast.makeText(PodLrwiseUpload.this, "Failed to fetch Depot data", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    if (response.body() != null) {
                        try {
                            String responseBody = response.body().string();
                            JSONArray jsonArray = new JSONArray(responseBody);
                            ArrayList<String> depoList = new ArrayList<>();
                            for (int i = 0; i < jsonArray.length(); i++) {
                                String fullString = jsonArray.getString(i);
                                String trimmedPart = fullString.split("-")[0].trim();
                                depoList.add(trimmedPart);
                            }
                            runOnUiThread(() -> {
                                ArrayAdapter<String> arrayAdapterDepo = new ArrayAdapter<>(PodLrwiseUpload.this, android.R.layout.simple_spinner_dropdown_item, depoList);
                                Log.e("Values", arrayAdapterDepo.toString());
                            });
                        } catch (JSONException e) {
                            runOnUiThread(() -> Toast.makeText(PodLrwiseUpload.this, "Failed to parse Depot data", Toast.LENGTH_SHORT).show());
                        }
                    } else {
                        runOnUiThread(() -> Toast.makeText(PodLrwiseUpload.this, "Response body is null", Toast.LENGTH_SHORT).show());
                    }
                } else {
                    runOnUiThread(() -> Toast.makeText(PodLrwiseUpload.this, "Failed to fetch Depot data", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // 📷 CAMERA
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            File imgFile = new File(getFilesDir(), "images/temp.jpg");
            handleImageFromPath(imgFile);
            return;
        }

        // 📂 FILE / GALLERY
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri == null) return;

            try {
                File imagePath = new File(getFilesDir(), "images");
                if (!imagePath.exists()) imagePath.mkdirs();

                File destFile = new File(imagePath, "temp.jpg");

                InputStream inputStream = getContentResolver().openInputStream(uri);
                FileOutputStream outputStream = new FileOutputStream(destFile);

                byte[] buffer = new byte[4096];
                int len;
                while ((len = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, len);
                }

                inputStream.close();
                outputStream.close();

                handleImageFromPath(destFile);

            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void handleImageFromPath(File imgFile) {

        if (!imgFile.exists()) {
            Toast.makeText(this, "Image file not found.", Toast.LENGTH_SHORT).show();
            return;
        }

        pictureImagePath = imgFile.getAbsolutePath();

        Bitmap bitmap = BitmapFactory.decodeFile(pictureImagePath);
        if (bitmap == null) {
            Toast.makeText(this, "Failed to decode image.", Toast.LENGTH_SHORT).show();
            return;
        }
        Bitmap scaledBitmap = scaleDown(bitmap, 1280, true);

        ImageView imageView1 = findViewById(R.id.imageView1);
        imageView1.setVisibility(View.VISIBLE);
        imageView1.setImageBitmap(scaledBitmap);

        try {
            FileOutputStream fos = new FileOutputStream(imgFile);
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos);
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        ScrollView mScrollView = findViewById(R.id.mScrollView);
        mScrollView.fullScroll(ScrollView.FOCUS_DOWN);
    }

    private String uploadFileWithOkHttp(String lrno, String drsnoLocal, String picturePath) {
        if (picturePath == null || picturePath.isEmpty())
            return "No file path provided";

        File file = new File(picturePath);
        if (!file.exists()) return "File not found: " + picturePath;

        MediaType mediaType = MediaType.parse("image/jpeg");
        RequestBody fileBody = RequestBody.create(mediaType, file);

        MultipartBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                // name must match $_FILES index in PHP
                .addFormDataPart("uploaded_file", file.getName(), fileBody)
                .addFormDataPart("image_name", lrno)
                .addFormDataPart("DRSNO", drsnoLocal == null ? "" : drsnoLocal)
                .build();

        Request request = new Request.Builder()
                .url(upLoadServerUri)
                .post(requestBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return "HTTP Error: " + response.code() + " - " + response.message();
            }
            ResponseBody rb = response.body();
            String bodyStr = rb != null ? rb.string() : "No server response";
            Log.e("UploadResponse", bodyStr);
            return bodyStr;
        } catch (Exception e) {
            Log.e("UploadException", e.getMessage(), e);
            return "Exception: " + e.getMessage();
        }
    }

    private void openCamera() {
        try {
            File imagePath = new File(getFilesDir(), "images");
            if (!imagePath.exists()) {
                imagePath.mkdirs();
            }

            File file = new File(imagePath, "temp.jpg");
            pictureImagePath = file.getAbsolutePath();

            Uri outputFileUri;
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                outputFileUri = getUriForFile(
                        getApplicationContext(),
                        "com.vtc3pl.app4.vtcpod.fileprovider",
                        file
                );
                cameraIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                outputFileUri = Uri.fromFile(file);
            }

            List<ResolveInfo> resolvedIntentActivities = getPackageManager()
                    .queryIntentActivities(cameraIntent, PackageManager.MATCH_DEFAULT_ONLY);
            for (ResolveInfo resolvedIntentInfo : resolvedIntentActivities) {
                String packageName = resolvedIntentInfo.activityInfo.packageName;
                grantUriPermission(packageName, outputFileUri,
                        FLAG_GRANT_WRITE_URI_PERMISSION | FLAG_GRANT_READ_URI_PERMISSION);
            }

            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
            startActivityForResult(cameraIntent, REQUEST_IMAGE_CAPTURE);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error opening camera: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
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
        if (ActivityCompat.shouldShowRequestPermissionRationale(PodLrwiseUpload.this,
                Manifest.permission.CAMERA)) {
        } else {
            ActivityCompat.requestPermissions(PodLrwiseUpload.this, new String[]{
                    Manifest.permission.CAMERA}, RequestPermissionCode);
        }
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    private void loadIntoSpinner(String json) throws JSONException {
        int nextpos = 0;
        JSONArray jsonArray;

        try {
            jsonArray = new JSONArray(json);
        } catch (JSONException e) {
            Log.e("JSON_ERROR", json);
            Toast.makeText(this,"Invalid server response",Toast.LENGTH_LONG).show();
            return;
        }

        Spinner dropdown = findViewById(R.id.spinner1);
        dropdown.setAdapter(null);
        pos.clear();
        posv.clear();
        posb.clear();
        String[] LRNO = new String[jsonArray.length()];

        // NEW: clear maps
        drsdtMap.clear();
        deliveryDateMap.clear();
        drsnoMap.clear();

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject obj = jsonArray.getJSONObject(i);

            LRNO[i] = obj.getString("lrno");
            if (obj.optInt("uploaded", 0) == 1)
                pos.add(i);
            if (obj.optInt("uploaded", 0) == 0 && nextpos == 0)
                nextpos = i;
            if (obj.optInt("verified", 0) == 1)
                posv.add(i);
            if (obj.optInt("verified", 0) == 2)
                posb.add(i);

            String drsdtStr = obj.optString("drsdt", "");
            String deliveryDateStr = obj.optString("deliverydate", "");
            drsdtMap.put(LRNO[i], drsdtStr);
            deliveryDateMap.put(LRNO[i], deliveryDateStr);

            if (obj.has("DRSNO")) {
                drsnoMap.put(LRNO[i], obj.getString("DRSNO"));
            } else {
                drsnoMap.put(LRNO[i], "");
            }

            if (obj.has("ApproveStatus")) {
                int approveStatusVal = obj.getInt("ApproveStatus");
                approveStatusMap.put(LRNO[i], approveStatusVal);
            }
        }

        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(PodLrwiseUpload.this, android.R.layout.simple_spinner_dropdown_item, LRNO) {
            @Override
            public boolean isEnabled(int position) {
                if (posv.contains(position)) {
                    return false;
                } else {
                    return true;
                }
            }

            @Override
            public View getDropDownView(int position, View convertView,
                                        ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView tv = (TextView) view;
                if (posv.contains(position)) {
                    tv.setTextColor(Color.GREEN);
                } else if (posb.contains(position)) {
                    tv.setTextColor(Color.RED);
                } else if (pos.contains(position)) {
                    tv.setTextColor(Color.BLUE);
                } else {
                    tv.setTextColor(Color.BLACK);
                }
                return view;
            }
        };
        dropdown.setAdapter(arrayAdapter);
        dropdown.setSelection(nextpos,true);

        String selectedLR = LRNO[nextpos];

        String deliveryDate = deliveryDateMap.get(selectedLR);

        if (deliveryDate != null && !deliveryDate.equals("") &&
                !deliveryDate.equals("0000-00-00")) {

            edtDeliveryDate.setText(deliveryDate);

        } else {
            edtDeliveryDate.setText("");
        }
        Toast.makeText(getApplicationContext(), "LR No. Loaded in the List.", Toast.LENGTH_SHORT).show();
    }

    // NEW: asynchronous update to server for DeliveryDate (non-blocking)
    private void updateDeliveryDateOnServerAsync(String lrno, String drsnoParam, String newDate) {
        RequestBody formBody = new FormBody.Builder()
                .add("lrno", lrno)
                .add("drsno", drsnoParam)
                .add("deliverydate", newDate)
                .build();

        Request request = new Request.Builder()
                .url("https://vtc3pl.com/update_delivery_date.php")
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> Toast.makeText(PodLrwiseUpload.this, "Delivery date update failed.", Toast.LENGTH_SHORT).show());
            }
            @Override public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    String body = response.body() != null ? response.body().string() : "";
                    if (body.trim().equals("OK")) {
                        deliveryDateMap.put(lrno, newDate);
                        runOnUiThread(() -> Toast.makeText(PodLrwiseUpload.this, "Delivery date updated.", Toast.LENGTH_SHORT).show());
                    } else {
                        runOnUiThread(() -> Toast.makeText(PodLrwiseUpload.this, "Delivery date update failed: " + body, Toast.LENGTH_LONG).show());
                    }
                } else {
                    runOnUiThread(() -> Toast.makeText(PodLrwiseUpload.this, "Delivery date update failed.", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    public class BackgroundWorker extends AsyncTask<String, Void, String> {
        Context context;
        AlertDialog.Builder alertDialog;

        BackgroundWorker(Context ctx) {
            context = ctx;
        }

        @Override
        protected String doInBackground(String... params) {
            String result = "";

            Spinner spinner = (Spinner) findViewById(R.id.spinner1);
            if (spinner.getSelectedItem() == null) {
                runOnUiThread(() -> {
                    if (dialog != null && dialog.isShowing()) dialog.dismiss();
                    Toast.makeText(PodLrwiseUpload.this, "Please select LR before upload.", Toast.LENGTH_SHORT).show();
                });
                return "No LR selected.";
            }

            String lrno = spinner.getSelectedItem().toString();

            // Get DRS date and DeliveryDate
            String drsdtStr = drsdtMap.get(lrno);
            String deliveryStr = deliveryDateMap.get(lrno);
            String drsnoLocal = drsnoMap.get(lrno);
            if (drsnoLocal == null || drsnoLocal.equals("")) {
                // fallback to class-level drsno if map doesn't have it
                if (drsno != null) drsnoLocal = drsno;
            }
            if (drsnoLocal == null) drsnoLocal = "";
            if (drsdtStr == null) drsdtStr = "";
            if (deliveryStr == null) deliveryStr = "";

            Date drsDate = null;
            try {
                if (!drsdtStr.equals("")) {
                    drsDate = sdf.parse(drsdtStr);
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }

            long diffDays = 0;
            if (drsDate != null) {
                long diffMillis = (new Date()).getTime() - drsDate.getTime();
                diffDays = diffMillis / (24 * 60 * 60 * 1000);
                if (diffDays < 0) diffDays = 0;
            }

            // If DRSDT > 3 days (diffDays > 4 in your original) then user must choose DeliveryDate
            if (drsDate != null && diffDays > 4) {
                String edtDateVal = edtDeliveryDate.getText().toString();
                if (edtDateVal.equals("")) {
                    final String msg = "तुमची वितरण तारीख ४ दिवसांपेक्षा जास्त आहे. त्यामुळे तुम्ही फोटो अपलोड करू शकणार नाही. कृपया वितरण तारीख फील्ड वापरून तारीख बदला (DRS तारखेपासून ४ दिवसांच्या आत तारीख निवडा). तसेच वितरण तारीख रिकामी असल्यास, कृपया तारीख निवडा आणि नंतर फोटो अपलोड करा.";
                    runOnUiThread(() -> {
                        if (dialog != null && dialog.isShowing()) dialog.dismiss();
                        new AlertDialog.Builder(PodLrwiseUpload.this).setTitle("Delivery Date Required")
                                .setMessage(msg).setPositiveButton("OK", null).show();
                    });
                    return "तुमची वितरण तारीख ४ दिवसांपेक्षा जास्त आहे. त्यामुळे तुम्ही फोटो अपलोड करू शकणार नाही. कृपया वितरण तारीख फील्ड वापरून तारीख बदला (DRS तारखेपासून ४ दिवसांच्या आत तारीख निवडा). तसेच वितरण तारीख रिकामी असल्यास, कृपया तारीख निवडा आणि नंतर फोटो अपलोड करा.";
                } else {
                    try {
                        Date chosen = sdf.parse(edtDateVal);
                        Calendar maxCal = Calendar.getInstance();
                        maxCal.setTime(drsDate);
                        maxCal.add(Calendar.DATE, 4);
                        if (chosen.before(drsDate) || chosen.after(maxCal.getTime())) {
                            Date finalDrsDate = drsDate;
                            runOnUiThread(() -> {
                                if (dialog != null && dialog.isShowing()) dialog.dismiss();
                                Toast.makeText(PodLrwiseUpload.this, "Selected DeliveryDate must be between " + sdf.format(finalDrsDate) + " and " + sdf.format(maxCal.getTime()), Toast.LENGTH_LONG).show();
                            });
                            return "तुमची डिलिव्हरी तारीख ४ दिवसांपेक्षा जास्त आहे. त्यामुळे तुम्ही फोटो अपलोड करू शकणार नाही. कृपया डिलिव्हरी तारीख फील्ड वापरून तारीख बदला (DRS तारखेपासून ४ दिवसांच्या आत तारीख निवडा). तसेच डिलिव्हरी तारीख रिकामी असल्यास, कृपया तारीख निवडा आणि नंतर फोटो अपलोड करा.";
                        } else {
                            if (!deliveryStr.equals(edtDateVal)) {
                                // synchronous update call using drsnoLocal
                                boolean ok = updateDeliveryDateOnServerSync(lrno, drsnoLocal, edtDateVal);
                                if (!ok) {
                                    runOnUiThread(() -> {
                                        if (dialog != null && dialog.isShowing()) dialog.dismiss();
                                        Toast.makeText(PodLrwiseUpload.this, "Failed to update DeliveryDate on server.", Toast.LENGTH_LONG).show();
                                    });
                                    return "Failed to update DeliveryDate on server";
                                } else {
                                    deliveryDateMap.put(lrno, edtDateVal);
                                }
                            }
                        }
                    } catch (ParseException e) {
                        e.printStackTrace();
                        runOnUiThread(() -> {
                            if (dialog != null && dialog.isShowing()) dialog.dismiss();
                            Toast.makeText(PodLrwiseUpload.this, "Invalid selected date.", Toast.LENGTH_SHORT).show();
                        });
                        return "निवडलेली तारीख योग्य नाही.";
                    }
                }
            } else {
                // if delivery date blank — set today on server before upload.
                if (deliveryStr == null || deliveryStr.equals("")) {
                    String todayStr = sdf.format(new Date());
                    boolean ok = updateDeliveryDateOnServerSync(lrno, drsnoLocal, todayStr);
                    if (!ok) {
                        runOnUiThread(() -> {
                            if (dialog != null && dialog.isShowing()) dialog.dismiss();
                            Toast.makeText(PodLrwiseUpload.this, "Failed to set DeliveryDate on server.", Toast.LENGTH_LONG).show();
                        });
                        return "Failed to set DeliveryDate on server.";
                    } else {
                        deliveryDateMap.put(lrno, todayStr);
                        runOnUiThread(() -> edtDeliveryDate.setText(todayStr));
                    }
                }
            }

            // finally upload image
            String uploadResult = uploadFileWithOkHttp(lrno, drsnoLocal, pictureImagePath);
            return uploadResult;
        }

        @Override
        protected void onPreExecute() {
            alertDialog = new AlertDialog.Builder(context);
            alertDialog.setTitle("Upload Status");
            alertDialog.setPositiveButton("OK", null);
        }

        @Override
        protected void onPostExecute(String result) {
            Spinner spinner1 = findViewById(R.id.spinner1);
            ImageView imageView1 = findViewById(R.id.imageView1);
            if (dialog != null && dialog.isShowing()) dialog.dismiss();
            alertDialog.setMessage(result);
            alertDialog.show();
            if (result != null && result.matches(".*Successfully Uploaded Photo.*")) {
                pictureImagePath = "";
                pictureImagePath = "";

                imageView1.setImageDrawable(null);

                editTextLR.setText("");
                edtDeliveryDate.setText("");

                lrList.clear();
                lrAdapter.notifyDataSetChanged();

                Spinner spinner = findViewById(R.id.spinner1);
                spinner.setAdapter(null);

                Toast.makeText(PodLrwiseUpload.this,
                        "Upload Successful. Page refreshed.",
                        Toast.LENGTH_LONG).show();
                int nextpos = 0;
                try {
                    List<String> sitems = new ArrayList<String>();
                    if (!pos.contains(spinner1.getSelectedItemPosition())) {
                        pos.add(spinner1.getSelectedItemPosition());
                    }
                    SpinnerAdapter Sadapter = spinner1.getAdapter();
                    for (int i = 0; i < Sadapter.getCount(); i++) {
                        sitems.add(Sadapter.getItem(i).toString());
                        if (!pos.contains(i) && nextpos == 0)
                            nextpos = i;
                    }
                    String[] LRNO = new String[sitems.size()];
                    sitems.toArray(LRNO);
                    ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(PodLrwiseUpload.this, android.R.layout.simple_spinner_dropdown_item, LRNO) {
                        @Override
                        public boolean isEnabled(int position) {
                            if (posv.contains(position)) {
                                return false;
                            } else {
                                return true;
                            }
                        }

                        @Override
                        public View getDropDownView(int position, View convertView,
                                                    ViewGroup parent) {
                            View view = super.getDropDownView(position, convertView, parent);
                            TextView tv = (TextView) view;
                            if (posv.contains(position)) {
                                tv.setTextColor(Color.GREEN);
                            } else if (posb.contains(position)) {
                                tv.setTextColor(Color.RED);
                            } else if (pos.contains(position)) {
                                tv.setTextColor(Color.BLUE);
                            } else {
                                tv.setTextColor(Color.BLACK);
                            }
                            return view;
                        }
                    };
                    spinner1.setAdapter(arrayAdapter);
                    spinner1.setSelection(nextpos);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                imageView1.setImageDrawable(null);
            }
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }
    }

    private boolean updateDeliveryDateOnServerSync(String lrno, String drsnoParam, String newDate) {
        RequestBody formBody = new FormBody.Builder()
                .add("lrno", lrno)
                .add("drsno", drsnoParam == null ? "" : drsnoParam)
                .add("deliverydate", newDate)
                .build();

        Request request = new Request.Builder()
                .url("https://vtc3pl.com/update_delivery_date.php")
                .post(formBody)
                .build();
        try {
            Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                String body = response.body() != null ? response.body().string() : "";
                Log.e("UpdateDeliveryResp", body);
                return body.trim().equals("OK");
            } else {
                Log.e("UpdateDeliveryHTTP", "code=" + response.code());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    class GetJSON extends AsyncTask<String, Void, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }
        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            try {
                loadIntoSpinner(s);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        @Override
        protected String doInBackground(String... params) {
            String drsnoParam = params[0];
            String weburl = "https://vtc3pl.com/getlrno_pod.php";
            try {
                URL url = new URL(weburl);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.setDoOutput(true);
                httpURLConnection.setDoInput(true);
                OutputStream outputStream = httpURLConnection.getOutputStream();
                BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8"));
                String post_data = URLEncoder.encode("drsno", "UTF-8") + "=" + URLEncoder.encode(drsnoParam, "UTF-8");
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
}