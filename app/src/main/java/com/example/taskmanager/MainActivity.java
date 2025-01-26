package com.example.taskmanager;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

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
}
