package com.example.studyplan;

public class Task {
    public long id;
    public String date;
    public String timeRange;
    public String subject;
    public String content;
    public int duration;
    public int pages;
    public int priority;
    public int status;
    public String specificTime;

    public Task() {}

    public Task(String date, String timeRange, String subject,
                String content, int duration, int pages, int priority) {
        this.date = date;
        this.timeRange = timeRange;
        this.subject = subject;
        this.content = content;
        this.duration = duration;
        this.pages = pages;
        this.priority = priority;
        this.status = 0;
        this.specificTime = null;
    }

    public Task(String date, String timeRange, String subject,
                String content, int duration, int pages, int priority, String specificTime) {
        this.date = date;
        this.timeRange = timeRange;
        this.subject = subject;
        this.content = content;
        this.duration = duration;
        this.pages = pages;
        this.priority = priority;
        this.status = 0;
        this.specificTime = specificTime;
    }
}
