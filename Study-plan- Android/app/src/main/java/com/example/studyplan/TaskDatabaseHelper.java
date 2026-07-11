package com.example.studyplan;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class TaskDatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "study_plan.db";
    private static final int DB_VERSION = 3;
    public static final String TABLE_NAME = "tasks";

    private static final String CREATE_SUBJECTS_TABLE =
            "CREATE TABLE IF NOT EXISTS subjects (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "name TEXT UNIQUE NOT NULL)";

    private static final String CREATE_TABLE =
            "CREATE TABLE tasks (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "date TEXT NOT NULL, " +
                    "time_range TEXT, " +
                    "subject TEXT NOT NULL, " +
                    "content TEXT NOT NULL, " +
                    "duration INTEGER DEFAULT 0, " +
                    "pages INTEGER DEFAULT 0, " +
                    "priority INTEGER DEFAULT 0, " +
                    "status INTEGER DEFAULT 0, " +
                    "specific_time TEXT)";

    public TaskDatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);
        db.execSQL(CREATE_SUBJECTS_TABLE);
        // 初始化默认学科数据
        String[] defaultSubjects = {"高数", "英语", "C语言", "线代", "概率论", "大物", "数据结构", "计组", "操作系统", "政治", "马原"};
        for (String subject : defaultSubjects) {
            ContentValues cv = new ContentValues();
            cv.put("name", subject);
            db.insert("subjects", null, cv);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL(CREATE_SUBJECTS_TABLE);
            String[] defaultSubjects = {"高数", "英语", "C语言", "线代", "概率论", "大物", "数据结构", "计组", "操作系统", "政治", "马原"};
            for (String subject : defaultSubjects) {
                ContentValues cv = new ContentValues();
                cv.put("name", subject);
                db.insert("subjects", null, cv);
            }
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE tasks ADD COLUMN specific_time TEXT");
        }
    }

    public long insertTask(Task task) {
        SQLiteDatabase db = getWritableDatabase();
        return insertTask(db, task);
    }

    public long insertTask(SQLiteDatabase db, Task task) {
        ContentValues cv = new ContentValues();
        cv.put("date", task.date);
        cv.put("time_range", task.timeRange);
        cv.put("subject", task.subject);
        cv.put("content", task.content);
        cv.put("duration", task.duration);
        cv.put("pages", task.pages);
        cv.put("priority", task.priority);
        cv.put("status", task.status);
        cv.put("specific_time", task.specificTime);
        return db.insert(TABLE_NAME, null, cv);
    }

    public List<Task> getAllTasks() {
        List<Task> tasks = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = null;
        try {
            c = db.rawQuery("SELECT * FROM " + TABLE_NAME + " ORDER BY date ASC, specific_time ASC, priority DESC", null);
            while (c.moveToNext()) {
                tasks.add(cursorToTask(c));
            }
        } finally {
            if (c != null) c.close();
        }
        return tasks;
    }

    public void updateTaskStatus(long id, int status) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("status", status);
        db.update(TABLE_NAME, cv, "id=?", new String[]{String.valueOf(id)});
    }

    /** 切换任务完成状态，返回新状态 */
    public int toggleTaskStatus(long id) {
        SQLiteDatabase db = getWritableDatabase();
        Cursor c = null;
        int current = 0;
        try {
            c = db.rawQuery("SELECT status FROM " + TABLE_NAME + " WHERE id=?", new String[]{String.valueOf(id)});
            if (c.moveToFirst()) {
                current = c.getInt(0);
            }
        } finally {
            if (c != null) c.close();
        }
        int newStatus = current == 1 ? 0 : 1;
        updateTaskStatus(id, newStatus);
        return newStatus;
    }

    public void deleteTask(long id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_NAME, "id=?", new String[]{String.valueOf(id)});
    }

    public Task getTaskById(long id) {
        android.database.sqlite.SQLiteDatabase db = getReadableDatabase();
        android.database.Cursor c = null;
        try {
            c = db.rawQuery("SELECT * FROM " + TABLE_NAME + " WHERE id=?", new String[]{String.valueOf(id)});
            if (c.moveToFirst()) {
                return cursorToTask(c);
            }
        } finally {
            if (c != null) c.close();
        }
        return null;
    }

    public void deleteAllTasks() {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_NAME, null, null);
        try {
            db.execSQL("VACUUM");
        } catch (Exception ignored) {}
    }

    private Task cursorToTask(Cursor c) {
        Task t = new Task();
        t.id = c.getLong(c.getColumnIndexOrThrow("id"));
        t.date = c.getString(c.getColumnIndexOrThrow("date"));
        t.timeRange = c.getString(c.getColumnIndexOrThrow("time_range"));
        t.subject = c.getString(c.getColumnIndexOrThrow("subject"));
        t.content = c.getString(c.getColumnIndexOrThrow("content"));
        t.duration = c.getInt(c.getColumnIndexOrThrow("duration"));
        t.pages = c.getInt(c.getColumnIndexOrThrow("pages"));
        t.priority = c.getInt(c.getColumnIndexOrThrow("priority"));
        t.status = c.getInt(c.getColumnIndexOrThrow("status"));
        t.specificTime = c.getString(c.getColumnIndexOrThrow("specific_time"));
        return t;
    }

    public List<String> getAllSubjects() {
        List<String> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = null;
        try {
            c = db.rawQuery("SELECT name FROM subjects", null);
            while (c.moveToNext()) {
                list.add(c.getString(0));
            }
        } finally {
            if (c != null) c.close();
        }
        return list;
    }

    public long insertSubject(String name) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        return db.insertWithOnConflict("subjects", null, cv, SQLiteDatabase.CONFLICT_IGNORE);
    }

    public void deleteSubject(String name) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete("subjects", "name=?", new String[]{name});
    }
}
