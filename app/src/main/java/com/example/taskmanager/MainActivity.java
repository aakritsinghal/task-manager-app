package com.example.taskmanager;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.app.PendingIntent;
import android.content.Intent;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    private EditText etTask;
    private Button btnAddTask;
    private LinearLayout llTaskList;

    private DatabaseReference databaseRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase Database reference
        databaseRef = FirebaseDatabase.getInstance().getReference("tasks");

        // Initialize UI elements
        etTask = findViewById(R.id.et_task);
        btnAddTask = findViewById(R.id.btn_add_task);
        llTaskList = findViewById(R.id.ll_task_list);

        // Load tasks from Firebase
        loadTasks();

        // Add task to Firebase
        btnAddTask.setOnClickListener(v -> {
            String task = etTask.getText().toString().trim();
            if (!task.isEmpty()) {
                addTask(task);
                etTask.setText(""); // Clear input field
            } else {
                Toast.makeText(MainActivity.this, "Enter a task", Toast.LENGTH_SHORT).show();
            }
        });

        createNotificationChannel();
    }

    private void loadTasks() {
        // Listen for changes in the Firebase database
        databaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                llTaskList.removeAllViews(); // Clear the task list
                for (DataSnapshot taskSnapshot : snapshot.getChildren()) {
                    String taskId = taskSnapshot.getKey();
                    String task = taskSnapshot.getValue(String.class);

                    // Display each task
                    addTaskToView(taskId, task);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(MainActivity.this, "Failed to load tasks", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addTask(String task) {
        // Generate a unique ID for the task
        String taskId = databaseRef.push().getKey();
        if (taskId != null) {
            databaseRef.child(taskId).setValue(task).addOnCompleteListener(task1 -> {
                if (task1.isSuccessful()) {
                    Toast.makeText(MainActivity.this, "Task added!", Toast.LENGTH_SHORT).show();
                    showNotification(task);
                } else {
                    Toast.makeText(MainActivity.this, "Failed to add task", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void addTaskToView(String taskId, String task) {
        // Create a TextView for each task
        TextView taskView = new TextView(this);
        taskView.setText(task);
        taskView.setTextSize(16);
        taskView.setPadding(8, 8, 8, 8);

        // Add click listener to remove the task
        taskView.setOnClickListener(v -> deleteTask(taskId));
        llTaskList.addView(taskView);
    }

    private void deleteTask(String taskId) {
        // Remove task from Firebase
        databaseRef.child(taskId).removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(MainActivity.this, "Task deleted!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "Failed to delete task", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Channel ID and attributes
            String channelId = "task_notifications";
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Task Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Notifications for task updates");
    
            // Register the channel
            NotificationManager notificationManager = 
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private void showNotification(String taskName) {
        String channelId = "task_notifications";
        int notificationId = (int) System.currentTimeMillis(); // Unique ID for each notification

        // Create an explicit intent for an Activity in your app
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground) // Replace with your app icon
                .setContentTitle("You have recently added a New Task")
                .setContentText("Task: " + taskName)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true); // Remove notification when tapped

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        // Show the notification
        notificationManager.notify(notificationId, builder.build());
    }


}
