package com.example.studyplan;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TaskParser {

    private static final Pattern P_DATE_ABSOLUTE = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})");
    private static final Pattern P_DATE_RELATIVE = Pattern.compile("(今天|明天|后天)");
    private static final Pattern P_DATE_WEEKDAY = Pattern.compile("(周[一二三四五六日]|星期[一二三四五六日])");
    private static final Pattern P_TIME_RANGE = Pattern.compile("(早上|上午|中午|下午|傍晚|晚上|深夜)");
    private static final Pattern P_SPECIFIC_TIME = Pattern.compile("(\\d{1,2})[：:](\\d{2})|(\\d{1,2})点(\\d{2})分?|(\\d{1,2})点半|(\\d{1,2})点");
    private static final Pattern P_DURATION = Pattern.compile("(\\d+)\\s*(分钟|min|mins)");
    private static final Pattern P_PAGES_CHAPTER = Pattern.compile("第\\s*(\\d+)\\s*章");
    private static final Pattern P_PAGES = Pattern.compile("(\\d+)\\s*页");

    public static List<Task> parse(String text, String todayStr, List<String> subjects) {
        List<Task> tasks = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) return tasks;

        LocalDate today;
        try {
            today = LocalDate.parse(todayStr);
        } catch (Exception e) {
            today = LocalDate.now();
            todayStr = today.toString();
        }

        String[] segments = text.split("[，,;；\\n、和以及]+");

        for (String segment : segments) {
            segment = segment.trim();
            if (segment.isEmpty()) continue;

            String date = null;
            String timeRange = null;
            String specificTime = null;
            String subject = null;
            int duration = 0;
            int pages = 0;
            int priority = 0;

            Matcher mAbs = P_DATE_ABSOLUTE.matcher(segment);
            if (mAbs.find()) {
                date = mAbs.group(1);
                segment = segment.replace(mAbs.group(), "").trim();
            }

            if (date == null) {
                Matcher mRel = P_DATE_RELATIVE.matcher(segment);
                if (mRel.find()) {
                    String rel = mRel.group(1);
                    if ("今天".equals(rel)) date = todayStr;
                    else if ("明天".equals(rel)) date = today.plusDays(1).toString();
                    else if ("后天".equals(rel)) date = today.plusDays(2).toString();
                    segment = segment.replace(mRel.group(), "").trim();
                }
            }

            if (date == null) {
                Matcher mWd = P_DATE_WEEKDAY.matcher(segment);
                if (mWd.find()) {
                    String wd = mWd.group(1);
                    date = resolveWeekday(today, wd);
                    segment = segment.replace(mWd.group(), "").trim();
                }
            }

            Matcher mTr = P_TIME_RANGE.matcher(segment);
            if (mTr.find()) {
                timeRange = mTr.group(1);
                segment = segment.replace(mTr.group(), "").trim();
            }

            Matcher mSt = P_SPECIFIC_TIME.matcher(segment);
            if (mSt.find()) {
                int hour = 0, minute = 0;
                boolean valid = false;
                if (mSt.group(1) != null && mSt.group(2) != null) {
                    hour = Integer.parseInt(mSt.group(1));
                    minute = Integer.parseInt(mSt.group(2));
                    valid = (hour >= 0 && hour < 24 && minute >= 0 && minute < 60);
                } else if (mSt.group(3) != null) {
                    hour = Integer.parseInt(mSt.group(3));
                    minute = mSt.group(4) != null ? Integer.parseInt(mSt.group(4)) : 0;
                    valid = (hour >= 0 && hour < 24 && minute >= 0 && minute < 60);
                } else if (mSt.group(5) != null) {
                    hour = Integer.parseInt(mSt.group(5));
                    minute = 30;
                    valid = (hour >= 0 && hour < 24);
                } else if (mSt.group(6) != null) {
                    hour = Integer.parseInt(mSt.group(6));
                    minute = 0;
                    valid = (hour >= 0 && hour < 24);
                }
                if (valid) {
                    if (timeRange != null && hour < 12) {
                        if ("下午".equals(timeRange) || "傍晚".equals(timeRange) || "晚上".equals(timeRange) || "深夜".equals(timeRange)) {
                            hour += 12;
                        }
                    }
                    specificTime = String.format(Locale.US, "%02d:%02d", hour, minute);
                }
                segment = segment.replace(mSt.group(), "").trim();
            }

            Matcher mDur = P_DURATION.matcher(segment);
            if (mDur.find()) {
                try {
                    duration = Integer.parseInt(mDur.group(1));
                } catch (NumberFormatException ignored) {}
                segment = segment.replace(mDur.group(), "").trim();
            }

            Matcher mChap = P_PAGES_CHAPTER.matcher(segment);
            if (mChap.find()) {
                try {
                    pages = Integer.parseInt(mChap.group(1));
                } catch (NumberFormatException ignored) {}
                segment = segment.replace(mChap.group(), "").trim();
            } else {
                java.util.regex.Pattern P_PAGE_RANGE = java.util.regex.Pattern.compile("(\\d+)\\s*(?:-|到|~)\\s*(\\d+)\\s*(?:页|pages|p\\b)", java.util.regex.Pattern.CASE_INSENSITIVE);
                java.util.regex.Matcher mRange = P_PAGE_RANGE.matcher(segment);
                if (mRange.find()) {
                    int start = Integer.parseInt(mRange.group(1));
                    int end = Integer.parseInt(mRange.group(2));
                    pages = Math.max(0, end - start + 1);
                } else {
                    java.util.regex.Matcher mPages = P_PAGES.matcher(segment);
                    if (mPages.find()) {
                        pages = Integer.parseInt(mPages.group(1));
                    }
                }
            }

            if (subjects != null) {
                for (String s : subjects) {
                    if (segment.contains(s)) {
                        subject = s;
                        segment = segment.replace(s, "").trim();
                        break;
                    }
                }
            }

            if (segment.contains("优先") || segment.contains("重要") || segment.contains("紧急") || segment.contains("必须")) {
                priority = 2;
                segment = segment.replace("优先", "").replace("重要", "").replace("紧急", "").replace("必须", "").trim();
            } else if (segment.contains("复习") || segment.contains("完成")) {
                priority = 1;
            }

            // 过滤无意义前缀词，修剪内容
            segment = segment.replaceAll("^(学习|完成|做点|写点|看点|学点|背点|做|写|阅读|复习|背诵|看|打卡)", "").trim();

            String content = segment;
            if (content.isEmpty()) {
                content = subject != null ? subject : "学习任务";
            }

            if (date == null) date = todayStr;
            if (subject == null) subject = "";

            if (specificTime == null) {
                if (timeRange != null) {
                    switch (timeRange) {
                        case "早上": specificTime = "08:00"; break;
                        case "上午": specificTime = "09:00"; break;
                        case "中午": specificTime = "12:00"; break;
                        case "下午": specificTime = "14:00"; break;
                        case "傍晚": specificTime = "17:00"; break;
                        case "晚上": specificTime = "19:00"; break;
                        case "深夜": specificTime = "22:00"; break;
                        default:    specificTime = "08:00"; break;
                    }
                } else {
                    specificTime = "08:00";
                }
            }

            tasks.add(new Task(date, timeRange, subject, content, duration, pages, priority, specificTime));
        }
        return tasks;
    }

    private static String resolveWeekday(LocalDate today, String weekdayStr) {
        DayOfWeek target;
        switch (weekdayStr) {
            case "周一": case "星期一": target = DayOfWeek.MONDAY; break;
            case "周二": case "星期二": target = DayOfWeek.TUESDAY; break;
            case "周三": case "星期三": target = DayOfWeek.WEDNESDAY; break;
            case "周四": case "星期四": target = DayOfWeek.THURSDAY; break;
            case "周五": case "星期五": target = DayOfWeek.FRIDAY; break;
            case "周六": case "星期六": target = DayOfWeek.SATURDAY; break;
            case "周日": case "星期日": target = DayOfWeek.SUNDAY; break;
            default: return today.toString();
        }
        return today.with(TemporalAdjusters.nextOrSame(target)).toString();
    }

}
