package com.example.studyplan;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.List;

public class StatsFragment extends Fragment {

    private TextView tvTotal, tvDone, tvRate, tvTotalDuration, tvTotalPages, tvProgressText, tvInsight, tvFocusHint, tvStatusOverview;
    private View viewProgressDone, viewProgressUndone;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_stats, container, false);

        tvTotal = view.findViewById(R.id.tv_total);
        tvDone = view.findViewById(R.id.tv_done);
        tvRate = view.findViewById(R.id.tv_rate);
        tvTotalDuration = view.findViewById(R.id.tv_total_duration);
        tvTotalPages = view.findViewById(R.id.tv_total_pages);
        tvProgressText = view.findViewById(R.id.tv_progress_text);
        tvInsight = view.findViewById(R.id.tv_insight);
        tvFocusHint = view.findViewById(R.id.tv_focus_hint);
        tvStatusOverview = view.findViewById(R.id.tv_status_overview);
        viewProgressDone = view.findViewById(R.id.view_progress_done);
        viewProgressUndone = view.findViewById(R.id.view_progress_undone);

        loadStats();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadStats();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && isAdded()) {
            loadStats();
        }
    }

    void loadStats() {
        if (getContext() == null) return;

        TaskDatabaseHelper db = new TaskDatabaseHelper(getContext());
        List<Task> tasks = db.getAllTasks();

        int total = tasks.size();
        int done = 0;
        int totalDuration = 0;
        int totalPages = 0;
        int pending = 0;

        for (Task t : tasks) {
            if (t.status == 1) {
                done++;
                totalDuration += t.duration;
                totalPages += t.pages;
            } else {
                pending++;
            }
        }

        int rate = total > 0 ? (int) (done * 100.0 / total) : 0;

        tvTotal.setText(String.valueOf(total));
        tvDone.setText(String.valueOf(done));
        tvRate.setText(rate + "%");
        tvTotalDuration.setText(totalDuration + " 分钟");
        tvTotalPages.setText(totalPages + " 页");
        if (total == 0) {
            tvInsight.setText("开始你的第一个学习计划吧");
        } else if (pending == 0) {
            tvInsight.setText("全部任务已完成，今天的节奏很棒！");
        } else {
            tvInsight.setText("还有 " + pending + " 项待完成，先专注 25 分钟吧");
        }
        if (total == 0) {
            tvFocusHint.setText("添加一个小目标，开始今天的专注");
        } else if (pending == 0) {
            tvFocusHint.setText("今日计划已清零，可以安心休息了");
        } else if (done == 0) {
            tvFocusHint.setText("先完成最简单的一项，建立启动惯性");
        } else {
            tvFocusHint.setText("已完成 " + rate + "%，再坚持一小步就更接近目标");
        }
        tvStatusOverview.setText(total == 0
                ? "暂无任务数据"
                : "全部 " + total + " 项 · 已完成 " + done + " 项 · 待完成 " + pending + " 项");

        // 进度条
        if (total > 0) {
            int donePct = rate;
            if (done > 0 && donePct == 0) {
                donePct = 1;
            }
            LinearLayout.LayoutParams doneLp = (LinearLayout.LayoutParams) viewProgressDone.getLayoutParams();
            doneLp.weight = donePct;
            viewProgressDone.setLayoutParams(doneLp);

            LinearLayout.LayoutParams undoneLp = (LinearLayout.LayoutParams) viewProgressUndone.getLayoutParams();
            undoneLp.weight = 100 - donePct;
            viewProgressUndone.setLayoutParams(undoneLp);

            tvProgressText.setText("已完成 " + done + " / " + total + " 项任务");
        } else {
            LinearLayout.LayoutParams doneLp = (LinearLayout.LayoutParams) viewProgressDone.getLayoutParams();
            doneLp.weight = 0;
            viewProgressDone.setLayoutParams(doneLp);
            LinearLayout.LayoutParams undoneLp = (LinearLayout.LayoutParams) viewProgressUndone.getLayoutParams();
            undoneLp.weight = 100;
            viewProgressUndone.setLayoutParams(undoneLp);
            tvProgressText.setText("还没有任务，快去添加吧");
        }
    }
}
