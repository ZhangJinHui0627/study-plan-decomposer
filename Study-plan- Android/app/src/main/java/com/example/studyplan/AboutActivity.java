package com.example.studyplan;

import android.app.AlertDialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        VersionConfig.init(this);
        setContentView(R.layout.activity_about);

        View aboutLayout = findViewById(R.id.about_layout);
        ViewCompat.setOnApplyWindowInsetsListener(aboutLayout, (v, insets) -> {
            int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);
            if (toolbar != null) {
                toolbar.setPadding(0, statusBarHeight, 0, 0);
            }
            return insets;
        });

        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        // 开发者信息中的版本号动态设置
        TextView tvDevVersion = findViewById(R.id.tv_dev_version);
        if (tvDevVersion != null) {
            tvDevVersion.setText("版本：" + VersionConfig.getVersionName());
        }

        // 顶部版本号动态设置
        TextView tvVersion = findViewById(R.id.tv_version);
        if (tvVersion != null) {
            tvVersion.setText(VersionConfig.getVersionName());
        }

        // 版本历史折叠展开控制
        View itemVersionHistory = findViewById(R.id.item_version_history);
        android.widget.ImageView ivArrowVersion = findViewById(R.id.iv_arrow_version);
        View llVersionHistoryDetails = findViewById(R.id.ll_version_history_details);

        if (itemVersionHistory != null && ivArrowVersion != null && llVersionHistoryDetails != null) {
            itemVersionHistory.setOnClickListener(v -> {
                if (llVersionHistoryDetails.getVisibility() == View.VISIBLE) {
                    llVersionHistoryDetails.setVisibility(View.GONE);
                    ivArrowVersion.animate().rotation(0f).setDuration(200).start();
                } else {
                    llVersionHistoryDetails.setVisibility(View.VISIBLE);
                    ivArrowVersion.animate().rotation(90f).setDuration(200).start();
                }
            });
        }

    }

    @Override
    public void finish() {
        super.finish();
    }
}
