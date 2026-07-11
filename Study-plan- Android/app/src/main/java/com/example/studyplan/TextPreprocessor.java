package com.example.studyplan;

public class TextPreprocessor {

    public static String clean(String rawInput) {
        if (rawInput == null || rawInput.trim().isEmpty()) return "";

        String result = rawInput
                .replaceAll("(?i)当然可以[，,].*?[：:]", "")
                .replaceAll("(?i)以下是.*?学习计划[：:]", "")
                .replaceAll("(?i)好的[，,].*?[：:]", "")
                .replaceAll("(?i)收到[，,].*?[：:]", "")
                .replaceAll("(?i)没问题[，,].*?[：:]", "");

        result = result.trim()
                .replace("，", ",")
                .replace("；", ";");

        return result;
    }
}
