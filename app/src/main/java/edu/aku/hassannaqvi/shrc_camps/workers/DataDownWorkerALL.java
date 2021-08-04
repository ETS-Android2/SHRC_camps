package edu.aku.hassannaqvi.shrc_camps.workers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.icu.util.Calendar;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import edu.aku.hassannaqvi.shrc_camps.R;
import edu.aku.hassannaqvi.shrc_camps.core.MainApp;

import static edu.aku.hassannaqvi.shrc_camps.core.MainApp.PROJECT_NAME;


public class DataDownWorkerALL extends Worker {

    private static final String APP_NAME = PROJECT_NAME;
    private final String TAG = "DataWorkerEN()";

    private final int position;
    private final String uploadTable;
    private final String uploadFilter;
    private final String uploadSelect;
    private final URL serverURL = null;
    HttpURLConnection urlConnection;
    private String nTitle = "Naunehal: Data Download";
    private ProgressDialog pd;
    private int length;
    private Data data;

    public DataDownWorkerALL(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        // to be initialised by workParams
        uploadTable = workerParams.getInputData().getString("table");
        position = workerParams.getInputData().getInt("position", -2);
        Log.d(TAG, "DataDownWorkerALL: position " + position);
        uploadSelect = workerParams.getInputData().getString("select");
        uploadFilter = workerParams.getInputData().getString("filter");
        nTitle = uploadTable.toUpperCase();


    }

    /*
     * This method is responsible for doing the work
     * so whatever work that is needed to be performed
     * we will put it here
     *
     * For example, here I am calling the method displayNotification()
     * It will display a notification
     * So that we will understand the work is executed
     * */

    @NonNull
    @Override
    public Result doWork() {

        Log.d(TAG, "doWork: Starting");
        displayNotification(nTitle, "Starting download", 0);

        StringBuilder result = new StringBuilder();

        URL url = null;
        try {
            if (serverURL == null) {
                url = new URL(MainApp._HOST_URL + MainApp._SERVER_GET_URL);
            } else {
                url = serverURL;
            }
            Log.d(TAG, "doWork: Connecting...");
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setReadTimeout(100000 /* milliseconds */);
            urlConnection.setConnectTimeout(150000 /* milliseconds */);
            urlConnection.setRequestMethod("POST");
            urlConnection.setDoOutput(true);
            urlConnection.setDoInput(true);
            urlConnection.setRequestProperty("Content-Type", "application/json");
            urlConnection.setRequestProperty("charset", "utf-8");
            urlConnection.setUseCaches(false);
            urlConnection.connect();
            Log.d(TAG, "downloadURL: " + url);

            JSONArray jsonSync = new JSONArray();

            DataOutputStream wr = new DataOutputStream(urlConnection.getOutputStream());

            JSONObject jsonTable = new JSONObject();
            JSONArray jsonParam = new JSONArray();

            jsonTable.put("table", uploadTable);
            jsonTable.put("select", uploadSelect);
            jsonTable.put("filter", uploadFilter);
            //jsonTable.put("limit", "3");
            //jsonTable.put("orderby", "rand()");
            //jsonSync.put(uploadData);
            jsonParam
                    .put(jsonTable);
            // .put(jsonSync);

            Log.d(TAG, "Upload Begins: " + jsonTable.toString());


            wr.writeBytes(String.valueOf(jsonTable));
            wr.flush();
            wr.close();

            Log.d(TAG, "doInBackground: " + urlConnection.getResponseCode());

            if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                Log.d(TAG, "Connection Response: " + urlConnection.getResponseCode());
                displayNotification(nTitle, "Connection Established",0);

                length = urlConnection.getContentLength();
                Log.d(TAG, "Content Length: " + length);

                InputStream in = new BufferedInputStream(urlConnection.getInputStream());

                BufferedReader reader = new BufferedReader(new InputStreamReader(in));

                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                    displayNotification(nTitle, "Received Data", result.length()*100/length);

                }

                if (result.equals("[]")) {
                    Log.d(TAG, "No data received from server: " + result);

                    data = new Data.Builder()
                            .putString("error", "No data received from server: " + result)
                            .putInt("position", this.position)
                            .build();
                    return Result.failure(data);
                }
                displayNotification(nTitle, "Received Data", 100);
                Log.d(TAG, "doWork(EN): " + result.toString());
            } else {

                Log.d(TAG, "Connection Response (Server Failure): " + urlConnection.getResponseCode());

                data = new Data.Builder()
                        .putString("error", String.valueOf(urlConnection.getResponseCode()))
                        .putInt("position", this.position)
                        .build();
                return Result.failure(data);
            }
        } catch (java.net.SocketTimeoutException e) {
            Log.d(TAG, "doWork (Timeout): " + e.getMessage());
            displayNotification(nTitle, "Timeout Error: " + e.getMessage(), 0);
            data = new Data.Builder()
                    .putString("error", String.valueOf(e.getMessage()))
                    .putInt("position", this.position)
                    .build();
            return Result.failure(data);

        } catch (IOException | JSONException e) {
            Log.d(TAG, "doWork (IO Error): " + e.getMessage());
            displayNotification(nTitle, "IO Error: " + e.getMessage(),0);
            data = new Data.Builder()
                    .putString("error", String.valueOf(e.getMessage()))
                    .putInt("position", this.position)
                    .build();

            return Result.failure(data);

        } finally {
//            urlConnection.disconnect();
        }

        //Do something with the JSON string
        if (result != null) {
            displayNotification(nTitle, "Starting Data Processing", 100);

            //String json = result.toString();
            /*if (json.length() > 0) {*/
            //(nTitle, "Data Size: " + result.length());


            // JSONArray jsonArray = new JSONArray(json);


            //JSONObject jsonObjectCC = jsonArray.getJSONObject(0);
            ///BE CAREFULL DATA.BUILDER CAN HAVE ONLY 1024O BYTES. EACH CHAR HAS 8 bits

            MainApp.downloadData[this.position] = String.valueOf(result);

            data = new Data.Builder()
                    //     .putString("data", String.valueOf(result))
                    .putInt("position", this.position)
                    .build();


            displayNotification(nTitle, "Downloaded successfully", 100);
            Log.d(TAG, "doWork: " + result);
            Log.d(TAG, "doWork (success) : position " + data.getInt("position", -1));
            return Result.success(data);

        } else {
            data = new Data.Builder()
                    .putString("error", String.valueOf(result))
                    .putInt("position", this.position)
                    .build();
            displayNotification(nTitle, "Error Received",0);
            return Result.failure(data);
        }


    }

    /*
     * The method is doing nothing but only generating
     * a simple notification
     * If you are confused about it
     * you should check the Android Notification Tutorial
     * */
    private void displayNotification(String title, String task, int updtProgress) {
        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(String.valueOf(position), APP_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder notification = new NotificationCompat.Builder(getApplicationContext(), String.valueOf(position))
                .setContentTitle(title)
                .setSubText("Data Size: "+length )
                .setContentText(task)
                .setWhen(Calendar.getInstance().getTimeInMillis())
                .setColorized(true)
                .setSmallIcon(R.drawable.app_icon);

        final int maxProgress = 100;
        int curProgress = updtProgress;
        notification.setProgress(length, curProgress, false);

        notificationManager.notify(position, notification.build());
    }
}