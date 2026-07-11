package com.example.studyplan;

import android.content.Context;

/**
 * 版本号统一管理中心
 * 所有版本号引用必须从这里获取，禁止在各处硬编码。
 * 新增版本号相关字段时在此扩展对应接口。
 */
public class VersionConfig {
    public static final int VERSION_CODE = 1;
    private static String VERSION_NAME = "alpha-2";

    public static void init(Context context) {
        VERSION_NAME = context.getString(R.string.version_name);
    }

    public static String getVersionName() {
        return VERSION_NAME;
    }
}
