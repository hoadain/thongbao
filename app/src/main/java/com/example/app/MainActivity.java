package com.example.app;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    private TextView tvLastId;
    private Button btnFetch;
    private RequestQueue requestQueue;
    private int lastId = -1;
    private static final String API_LAST_ID = "https://57kmt.duckdns.org/android/api.aspx?action=last_id";
    private static final String API_GET_ID = "https://57kmt.duckdns.org/android/api.aspx?action=get_id&id=";
    private Handler handler = new Handler();
    private Timer timer;

    private static final int REQUEST_CODE_POST_NOTIFICATIONS = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvLastId = findViewById(R.id.tvLastId);
        btnFetch = findViewById(R.id.btnFetch);
        requestQueue = Volley.newRequestQueue(this);

        btnFetch.setOnClickListener(v -> fetchLastId());

        // Kiểm tra quyền trên Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_CODE_POST_NOTIFICATIONS);
            }
        }

        // Chạy timer mỗi 30s
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                handler.post(MainActivity.this::fetchLastId);
            }
        }, 0, 30000);
    }

    private void fetchLastId() {
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, API_LAST_ID, null,
                response -> {
                    try {
                        int newLastId = response.getInt("last_id");
                        if (newLastId > lastId) {
                            lastId = newLastId;
                            tvLastId.setText("Last ID: " + lastId);
                            fetchMessageById(lastId);
                        }
                    } catch (JSONException e) {
                        Log.e("API", "Lỗi JSON", e);
                    }
                },
                error -> Log.e("API", "Lỗi API", error)
        );
        requestQueue.add(request);
    }

    private void fetchMessageById(int id) {
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, API_GET_ID + id, null,
                response -> {
                    try {
                        String message = response.getString("msg");
                        showNotification("Thông báo mới", message);
                        playNotificationSound();
                    } catch (JSONException e) {
                        Log.e("API", "Lỗi JSON", e);
                    }
                },
                error -> Log.e("API", "Lỗi API", error)
        );
        requestQueue.add(request);
    }

    private void showNotification(String title, String message) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.e("Notification", "Không có quyền POST_NOTIFICATIONS");
                return;
            }
        }

        String channelId = "channel_id";
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Thông báo", NotificationManager.IMPORTANCE_HIGH);
            manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        manager.notify(1, builder.build());
    }

    private void playNotificationSound() {
        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        Ringtone ringtone = RingtoneManager.getRingtone(getApplicationContext(), soundUri);
        ringtone.play();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_POST_NOTIFICATIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Quyền thông báo đã được cấp!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Quyền thông báo bị từ chối!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timer != null) {
            timer.cancel();
        }
    }
}
