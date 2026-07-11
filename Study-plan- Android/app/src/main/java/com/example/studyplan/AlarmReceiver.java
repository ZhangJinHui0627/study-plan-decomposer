package com.example.studyplan;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())
                || "android.intent.action.QUICKBOOT_POWERON".equals(intent.getAction())
                || Intent.ACTION_TIMEZONE_CHANGED.equals(intent.getAction())
                || "android.intent.action.TIME_SET".equals(intent.getAction()))) {
            TaskDatabaseHelper db = new TaskDatabaseHelper(context);
            java.util.List<Task> tasks = db.getAllTasks();
            for (Task task : tasks) {
                if (task.status == 0) {
                    AlarmScheduler.setAlarm(context, task);
                }
            }
            return;
        }

        long taskId = intent.getLongExtra("task_id", -1);
        String subject = intent.getStringExtra("task_subject");
        String content = intent.getStringExtra("task_content");

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel channel = new NotificationChannel(
                "study_plan_channel", "学习提醒",
                NotificationManager.IMPORTANCE_HIGH
        );
        nm.createNotificationChannel(channel);

        android.content.Intent clickIntent = new android.content.Intent(context, MainActivity.class);
        clickIntent.setFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP | android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP);
        android.app.PendingIntent pendingIntent = android.app.PendingIntent.getActivity(
                context, (int) taskId, clickIntent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "study_plan_channel")
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle("学习提醒: " + (subject != null ? subject : ""))
                .setContentText((content != null && !content.trim().isEmpty()) ? content : "开始您的学习计划吧！")
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        nm.notify((int) taskId, builder.build());
    }
}
