package com.vtc3pl.app4.vtcpod;

import android.Manifest;
import android.annotation.SuppressLint;
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
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
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

import static android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION;
import static android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
import static androidx.core.content.FileProvider.getUriForFile;

public class Main2Activity extends AppCompatActivity {

    public static final int RequestPermissionCode = 1;
    static final int REQUEST_IMAGE_CAPTURE = 1;
    ProgressDialog dialog = null;
    int serverResponseCode = 0;
    List<Integer> pos = new ArrayList<Integer>();
    List<Integer> posv = new ArrayList<Integer>();
    List<Integer> posb = new ArrayList<Integer>();
    private String upLoadServerUri = "https://vtc3pl.com/Upload_POD.php";
    private String pictureImagePath = "", drsno = "";
    private Spinner spinnerDepo;
    private OkHttpClient client;

    // NEW: maps to hold drsdt and deliverydate per LRNO
    private Map<String, String> drsdtMap = new HashMap<>();
    private Map<String, String> deliveryDateMap = new HashMap<>();
    private Map<String, String> drsnoMap = new HashMap<>();

    private EditText edtDeliveryDate;
    private ImageView imgCal;

    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    HashMap<String, Integer> approveStatusMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        Button btn1 = findViewById(R.id.btn1);
        Button btn2 = findViewById(R.id.btn2);
        Button btn3 = findViewById(R.id.btn3);
        Button btnaUpload = findViewById(R.id.btnaUpload);

        Spinner spinnerYear = findViewById(R.id.spinnerYear);
        spinnerDepo = findViewById(R.id.Depo);
        Spinner spinnerVehicleHO = findViewById(R.id.VehicleHO);
        Spinner spinnerMonth = findViewById(R.id.Month);


        EnableRuntimePermission();
        String[] VehicleHO = {"H", "O"};

        String[] Month = {"April - A", "May - B", "June - C", "July - D", "August - E", "September - F", "October - G", "November - H", "December - I", "January - J", "February - K", "March - L"};

        String[] Year = {"24-25", "25-26", "26-27", "27-28", "28-29", "29-30", "30-31", "31-32"};

        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(Main2Activity.this, android.R.layout.simple_spinner_dropdown_item, Year);
        ArrayAdapter<String> arrayAdapterVehicleHO = new ArrayAdapter<>(Main2Activity.this, android.R.layout.simple_spinner_dropdown_item, VehicleHO);
        ArrayAdapter<String> arrayAdapterMonth = new ArrayAdapter<>(Main2Activity.this, android.R.layout.simple_spinner_dropdown_item, Month);
        spinnerYear.setAdapter(arrayAdapter);
        spinnerYear.setSelection(2);

        spinnerVehicleHO.setAdapter(arrayAdapterVehicleHO);
        spinnerVehicleHO.setSelection(0);

        spinnerMonth.setAdapter(arrayAdapterMonth);

        Calendar calendar = Calendar.getInstance();
        int currentMonth = calendar.get(Calendar.MONTH);
        Log.e("CurrentMonth", String.valueOf(Calendar.MONTH));

        int monthIndex;
        switch (currentMonth) {
            case Calendar.JANUARY:
                monthIndex = 9;
                break;
            case Calendar.FEBRUARY:
                monthIndex = 10;
                break;
            case Calendar.MARCH:
                monthIndex = 11;
                break;
            case Calendar.APRIL:
                monthIndex = 0;
                break;
            case Calendar.MAY:
                monthIndex = 1;
                break;
            case Calendar.JUNE:
                monthIndex = 2;
                break;
            case Calendar.JULY:
                monthIndex = 3;
                break;
            case Calendar.AUGUST:
                monthIndex = 4;
                break;
            case Calendar.SEPTEMBER:
                monthIndex = 5;
                break;
            case Calendar.OCTOBER:
                monthIndex = 6;
                break;
            case Calendar.NOVEMBER:
                monthIndex = 7;
                break;
            case Calendar.DECEMBER:
                monthIndex = 8;
                break;
            default:
                monthIndex = 0;
        }

        spinnerMonth.setSelection(monthIndex);

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

        edtDeliveryDate = findViewById(R.id.edtDeliveryDate);
        imgCal = findViewById(R.id.imgCal);

        // When user selects LR No (spinner item selection)
        Spinner spinner_2 = findViewById(R.id.spinner1);

//        spinner_2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//            @Override
//            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
//
//                String lrno = spinner_2.getSelectedItem().toString();
//
//                String drsDateStr = drsdtMap.get(lrno);
//                String deliveryDateStr = deliveryDateMap.get(lrno);
//
//                boolean deliveryAvailable = false;
//
//                // CHECK DELIVERY DATE
//                if (deliveryDateStr != null) {
//                    String d = deliveryDateStr.trim();
//                    if (!d.equals("") && !d.equals("0000-00-00") && !d.equalsIgnoreCase("null")) {
//                        deliveryAvailable = true;
//                    }
//                }
//
//                // CHECK DRS OLDER THAN 4 DAYS
//                boolean isOlderThan4Days = false;
//
//                if (drsDateStr != null && !drsDateStr.trim().equals("")) {
//                    try {
//                        Date drsDate = sdf.parse(drsDateStr);
//                        long diff = new Date().getTime() - drsDate.getTime();
//                        long days = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);
//
//                        if (days > 4) {
//                            isOlderThan4Days = true;
//                        }
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                }
//
//                // FINAL CONDITION ENABLE / DISABLE
//                if (!deliveryAvailable || isOlderThan4Days) {
//                    edtDeliveryDate.setEnabled(true);
//                    edtDeliveryDate.setFocusable(true);
//                    edtDeliveryDate.setFocusableInTouchMode(true);
//                    edtDeliveryDate.setClickable(true);
//
//                    imgCal.setEnabled(true);
//                    imgCal.setClickable(true);
//
//                    // DISABLE
//                    edtDeliveryDate.setEnabled(false);
//                    edtDeliveryDate.setFocusable(false);
//                    edtDeliveryDate.setFocusableInTouchMode(false);
//                    edtDeliveryDate.setClickable(false);
//
//                    imgCal.setEnabled(false);
//                    imgCal.setClickable(false);
//
//                } else {
//
//                    // ENABLE
//                    edtDeliveryDate.setEnabled(true);
//                    edtDeliveryDate.setFocusable(true);
//                    edtDeliveryDate.setFocusableInTouchMode(true);
//                    edtDeliveryDate.setClickable(true);
//
//                    imgCal.setEnabled(true);
//                    imgCal.setClickable(true);
//                }
//            }
//
//            @Override
//            public void onNothingSelected(AdapterView<?> parent) {
//
//            }
//        });

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

                //-------------------------------------------------
                //  FINAL UPDATED LOGIC
                //-------------------------------------------------

                // ✔ RULE 1: If ApproveStatus == 1 → ALWAYS ENABLE
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

        imgCal.setOnClickListener(v -> {
            Object sel = spinner1.getSelectedItem();
            if (sel == null) {
                Toast.makeText(Main2Activity.this, "Please select LR first", Toast.LENGTH_SHORT).show();
                return;
            }
            String lrno = sel.toString();
            String drsdtStr = drsdtMap.get(lrno);
            if (drsdtStr == null || drsdtStr.equals("")) {
                Toast.makeText(Main2Activity.this, "No DRS date available for this LR", Toast.LENGTH_SHORT).show();
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

                DatePickerDialog dpd = new DatePickerDialog(Main2Activity.this, (DatePicker view, int year, int month, int dayOfMonth) -> {
                    Calendar chosen = Calendar.getInstance();
                    chosen.set(year, month, dayOfMonth, 0, 0, 0);
                    Date chosenDate = chosen.getTime();

                    if (chosenDate.before(minCal.getTime()) || chosenDate.after(maxCal.getTime())) {
                        Toast.makeText(Main2Activity.this, "Allowed date must be between " + sdf.format(minCal.getTime()) + " and " + sdf.format(maxCal.getTime()), Toast.LENGTH_LONG).show();
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
                        Toast.makeText(Main2Activity.this, "DRS number is not set. Please get LR list first.", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(Main2Activity.this, "Invalid DRS date format.", Toast.LENGTH_SHORT).show();
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
                    new AlertDialog.Builder(Main2Activity.this)
                            .setTitle("Approval Required")
                            .setMessage("DRS क्रमांकाची तारीख ४ दिवसांपेक्षा जुनी आहे. तुम्ही POD अपलोड करू शकणार नाही. POD अपलोड करण्यासाठी कृपया राजेश पवार सर यांची मंजुरी (approval) घ्या आणि त्यानंतरच अपलोड करा.")
                            .setPositiveButton("OK", null)
                            .show();
                    return;
                }
            }

            if (approveStatus == 2) {
                new AlertDialog.Builder(Main2Activity.this)
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

            dialog = ProgressDialog.show(Main2Activity.this, "", "Uploading file...", true);
            BackgroundWorker backgroundWorker = new BackgroundWorker(Main2Activity.this);
            backgroundWorker.execute();
        });

        btn3.setOnClickListener(view -> {
            Spinner spinnerDepo = findViewById(R.id.Depo);
//            Spinner spinnerYear = findViewById(R.id.spinnerYear);
//            Spinner spinnerVehicleHO = findViewById(R.id.VehicleHO);
//            Spinner spinnerMonth = findViewById(R.id.Month);
            EditText editText1 = findViewById(R.id.editText1);

            String depoStr = spinnerDepo.getSelectedItem().toString();
            String yearStr = spinnerYear.getSelectedItem().toString();
            String vehicleHOStr = spinnerVehicleHO.getSelectedItem().toString();
            String monthStr = spinnerMonth.getSelectedItem().toString();
            String lastTwoCharsOfYear = yearStr.substring(yearStr.length() - 2);

            String str = vehicleHOStr + monthStr.charAt(monthStr.length() - 1) + depoStr + lastTwoCharsOfYear + editText1.getText().toString();
            if (str.matches("")) {
                Toast.makeText(getApplicationContext(), "please enter DRS NO.", Toast.LENGTH_LONG).show();
                return;
            }
            drsno = "D" + str;
            Toast.makeText(getApplicationContext(), "DRS No: " + drsno, Toast.LENGTH_LONG).show();
            GetJSON getJSON = new GetJSON();
            getJSON.execute(drsno);
        });

        btnaUpload.setOnClickListener(view -> {
            Intent myIntent = new Intent(Main2Activity.this, Main4Activity.class);
            startActivity(myIntent);
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

    private void fetchDepoData() {
        Request request = new Request.Builder()
                .url("https://vtc3pl.com/fetch_depotcode_and_names_for_pod_app.php")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> Toast.makeText(Main2Activity.this, "Failed to fetch Depot data", Toast.LENGTH_SHORT).show());
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
                                ArrayAdapter<String> arrayAdapterDepo = new ArrayAdapter<>(Main2Activity.this, android.R.layout.simple_spinner_dropdown_item, depoList);
                                Log.e("Values", arrayAdapterDepo.toString());
                                spinnerDepo.setAdapter(arrayAdapterDepo);
                            });
                        } catch (JSONException e) {
                            runOnUiThread(() -> Toast.makeText(Main2Activity.this, "Failed to parse Depot data", Toast.LENGTH_SHORT).show());
                        }
                    } else {
                        runOnUiThread(() -> Toast.makeText(Main2Activity.this, "Response body is null", Toast.LENGTH_SHORT).show());
                    }
                } else {
                    runOnUiThread(() -> Toast.makeText(Main2Activity.this, "Failed to fetch Depot data", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    @SuppressLint("MissingSuperCall")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            File imagePath = new File(getFilesDir(), "images");
            File imgFile = new File(imagePath, "temp.jpg");
            if (imgFile.exists()) {
                pictureImagePath = imgFile.getAbsolutePath();
                Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                Bitmap newBitmap = scaleDown(myBitmap, 1280, true);
                ImageView imageView1 = findViewById(R.id.imageView1);
                imageView1.setVisibility(View.VISIBLE);
                imageView1.setImageBitmap(newBitmap);
                try {
                    FileOutputStream fos = new FileOutputStream(imgFile);
                    newBitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos);
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ScrollView mScrollView = findViewById(R.id.mScrollView);
                mScrollView.fullScroll(ScrollView.FOCUS_DOWN);
            } else {
                Toast.makeText(this, "Image file not found after camera capture.", Toast.LENGTH_LONG).show();
            }
        }
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
        if (ActivityCompat.shouldShowRequestPermissionRationale(Main2Activity.this,
                Manifest.permission.CAMERA)) {
        } else {
            ActivityCompat.requestPermissions(Main2Activity.this, new String[]{
                    Manifest.permission.CAMERA}, RequestPermissionCode);
        }
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    private void loadIntoSpinner(String json) throws JSONException {
        int nextpos = 0;
        JSONArray jsonArray = new JSONArray(json);

        if (jsonArray.length() == 0) {
            Toast.makeText(getApplicationContext(), "DRS No. not found.", Toast.LENGTH_SHORT).show();
            drsno = "";
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

        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(Main2Activity.this, android.R.layout.simple_spinner_dropdown_item, LRNO) {
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
        dropdown.setSelection(nextpos);
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
                runOnUiThread(() -> Toast.makeText(Main2Activity.this, "Delivery date update failed.", Toast.LENGTH_SHORT).show());
            }
            @Override public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    String body = response.body() != null ? response.body().string() : "";
                    if (body.trim().equals("OK")) {
                        deliveryDateMap.put(lrno, newDate);
                        runOnUiThread(() -> Toast.makeText(Main2Activity.this, "Delivery date updated.", Toast.LENGTH_SHORT).show());
                    } else {
                        runOnUiThread(() -> Toast.makeText(Main2Activity.this, "Delivery date update failed: " + body, Toast.LENGTH_LONG).show());
                    }
                } else {
                    runOnUiThread(() -> Toast.makeText(Main2Activity.this, "Delivery date update failed.", Toast.LENGTH_SHORT).show());
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
                    Toast.makeText(Main2Activity.this, "Please select LR before upload.", Toast.LENGTH_SHORT).show();
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
                        new AlertDialog.Builder(Main2Activity.this).setTitle("Delivery Date Required")
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
                                Toast.makeText(Main2Activity.this, "Selected DeliveryDate must be between " + sdf.format(finalDrsDate) + " and " + sdf.format(maxCal.getTime()), Toast.LENGTH_LONG).show();
                            });
                            return "तुमची डिलिव्हरी तारीख ४ दिवसांपेक्षा जास्त आहे. त्यामुळे तुम्ही फोटो अपलोड करू शकणार नाही. कृपया डिलिव्हरी तारीख फील्ड वापरून तारीख बदला (DRS तारखेपासून ४ दिवसांच्या आत तारीख निवडा). तसेच डिलिव्हरी तारीख रिकामी असल्यास, कृपया तारीख निवडा आणि नंतर फोटो अपलोड करा.";
                        } else {
                            if (!deliveryStr.equals(edtDateVal)) {
                                // synchronous update call using drsnoLocal
                                boolean ok = updateDeliveryDateOnServerSync(lrno, drsnoLocal, edtDateVal);
                                if (!ok) {
                                    runOnUiThread(() -> {
                                        if (dialog != null && dialog.isShowing()) dialog.dismiss();
                                        Toast.makeText(Main2Activity.this, "Failed to update DeliveryDate on server.", Toast.LENGTH_LONG).show();
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
                            Toast.makeText(Main2Activity.this, "Invalid selected date.", Toast.LENGTH_SHORT).show();
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
                            Toast.makeText(Main2Activity.this, "Failed to set DeliveryDate on server.", Toast.LENGTH_LONG).show();
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
                    ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(Main2Activity.this, android.R.layout.simple_spinner_dropdown_item, LRNO) {
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