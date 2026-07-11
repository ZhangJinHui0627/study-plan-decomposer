package com.example.studyplan;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import androidx.core.content.ContextCompat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.TimeZone;

public class CalendarHelper {
    
    public static boolean hasCalendarPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_CALENDAR) 
                == PackageManager.PERMISSION_GRANTED;
    }

    public static void addToCalendar(Context context, Task task) {
        if (!hasCalendarPermission(context)) return;
        try {
            ContentResolver cr = context.getContentResolver();
            long calId = 1; // 默认使用第一个日历账户
            
            Uri uri = CalendarContract.Calendars.CONTENT_URI;
            String[] projection = new String[]{CalendarContract.Calendars._ID};
            String selection = CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL + " >= " + CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR;
            Cursor cursor = cr.query(uri, projection, selection, null, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    calId = cursor.getLong(0);
                }
                cursor.close();
            }

            // 日期解析
            LocalDate localDate = LocalDate.parse(task.date);
            LocalTime localTime = null;
            if (task.specificTime != null && !task.specificTime.isEmpty()) {
                try {
                    localTime = LocalTime.parse(task.specificTime);
                } catch (Exception ignored) {}
            }
            if (localTime == null) {
                int hour = 9; // 默认早上 9 点
                if ("下午".equals(task.timeRange)) hour = 14;
                else if ("晚上".equals(task.timeRange)) hour = 19;
                else if ("中午".equals(task.timeRange)) hour = 12;
                else if ("深夜".equals(task.timeRange)) hour = 22;
                localTime = LocalTime.of(hour, 0);
            }

            LocalDateTime start = localDate.atTime(localTime);
            long startMillis = start.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            long endMillis = start.plusMinutes(task.duration > 0 ? task.duration : 60)
                    .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

            ContentValues values = new ContentValues();
            values.put(CalendarContract.Events.DTSTART, startMillis);
            values.put(CalendarContract.Events.DTEND, endMillis);
            values.put(CalendarContract.Events.TITLE, "[" + task.subject + "] " + task.content);
            values.put(CalendarContract.Events.DESCRIPTION, "来自学习计划智能拆解App。时长: " + task.duration + "分钟, 页数: " + task.pages + "页");
            values.put(CalendarContract.Events.CALENDAR_ID, calId);
            values.put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().getID());

            Uri eventUri = cr.insert(CalendarContract.Events.CONTENT_URI, values);
            if (eventUri != null) {
                long eventId = Long.parseLong(eventUri.getLastPathSegment());
                ContentValues reminderValues = new ContentValues();
                reminderValues.put(CalendarContract.Reminders.MINUTES, 10); // 提前10分钟提醒
                reminderValues.put(CalendarContract.Reminders.EVENT_ID, eventId);
                reminderValues.put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT);
                cr.insert(CalendarContract.Reminders.CONTENT_URI, reminderValues);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void updateCalendarStatus(Context context, Task task) {
        if (!hasCalendarPermission(context)) return;
        try {
            android.content.ContentResolver cr = context.getContentResolver();
            android.net.Uri uri = CalendarContract.Events.CONTENT_URI;
            String oldTitle1 = "[" + task.subject + "] " + task.content;
            String oldTitle2 = "[已完成] [" + task.subject + "] " + task.content;
            String newTitle = (task.status == 1 ? "[已完成] " : "") + "[" + task.subject + "] " + task.content;
            android.content.ContentValues values = new android.content.ContentValues();
            values.put(CalendarContract.Events.TITLE, newTitle);
            String where = "(" + CalendarContract.Events.TITLE + "=? OR " + CalendarContract.Events.TITLE + "=?) AND "
                    + CalendarContract.Events.DESCRIPTION + " LIKE ?";
            String[] selectionArgs = new String[]{oldTitle1, oldTitle2, "%来自学习计划智能拆解App%"};
            cr.update(uri, values, where, selectionArgs);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void deleteFromCalendar(Context context, Task task) {
        if (!hasCalendarPermission(context)) return;
        try {
            android.content.ContentResolver cr = context.getContentResolver();
            android.net.Uri uri = CalendarContract.Events.CONTENT_URI;
            String title1 = "[" + task.subject + "] " + task.content;
            String title2 = "[已完成] [" + task.subject + "] " + task.content;
            String where = "(" + CalendarContract.Events.TITLE + "=? OR " + CalendarContract.Events.TITLE + "=?) AND "
                    + CalendarContract.Events.DESCRIPTION + " LIKE ?";
            String[] selectionArgs = new String[]{
                title1,
                title2,
                "%来自学习计划智能拆解App%"
            };
            cr.delete(uri, where, selectionArgs);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
