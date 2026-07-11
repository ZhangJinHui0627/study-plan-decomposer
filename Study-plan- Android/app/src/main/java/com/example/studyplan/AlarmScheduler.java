package com.example.studyplan;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

public class AlarmScheduler {

    public static void setAlarm(Context context, Task task) {
        cancelAlarm(context, task.id);
        if (task.status != 0) return;
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        // API 31+ 检查是否可以设置精确闹钟
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (!am.canScheduleExactAlarms()) {
                // 如果没有权限，降级使用 setAndAllowWhileIdle 或者引导用户开启权限
                // 这里简单处理，暂时继续尝试（系统可能会抛异常或忽略）
            }
        }

        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra("task_id", task.id);
        intent.putExtra("task_subject", task.subject);
        intent.putExtra("task_content", task.content);

        int requestCode = (int) (task.id ^ (task.id >>> 32)); // 更安全的 long to int

        PendingIntent pi = PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        long triggerTime = calculateTriggerTime(task.date, task.timeRange, task.specificTime);
        if (triggerTime > System.currentTimeMillis()) {
            try {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pi);
            } catch (SecurityException e) {
                // 如果 setExact 报错，降级使用普通闹钟
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pi);
            }
        }
    }

    public static void cancelAlarm(Context context, long taskId) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        Intent intent = new Intent(context, AlarmReceiver.class);
        int requestCode = (int) (taskId ^ (taskId >>> 32));
        PendingIntent pi = PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_NO_CREATE
        );
        if (pi != null) {
            am.cancel(pi);
            pi.cancel();
        }
    }

    private static long calculateTriggerTime(String date, String timeRange, String specificTime) {
        java.time.LocalDate ld = java.time.LocalDate.parse(date);
        java.time.LocalTime lt;
        if (specificTime != null && !specificTime.isEmpty()) {
            try {
                lt = java.time.LocalTime.parse(specificTime);
            } catch (Exception e) {
                lt = timeToLocalTime(timeRange);
            }
        } else {
            lt = timeToLocalTime(timeRange);
        }
        return ld.atTime(lt).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private static LocalTime timeToLocalTime(String range) {
        if (range == null) return LocalTime.of(8, 0);
        switch (range) {
            case "早上": return LocalTime.of(8, 0);
            case "上午": return LocalTime.of(9, 0);
            case "中午": return LocalTime.of(12, 0);
            case "下午": return LocalTime.of(14, 0);
            case "傍晚": return LocalTime.of(17, 0);
            case "晚上": return LocalTime.of(19, 0);
            case "深夜": return LocalTime.of(22, 0);
            default: return LocalTime.of(8, 0);
        }
    }
}
