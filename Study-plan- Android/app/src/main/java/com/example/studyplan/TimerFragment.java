package com.example.studyplan;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.studyplan.DialogHelper;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TimerFragment extends Fragment {

    private TextView tvTimerDisplay, tvTimerTask, tvTodayTotal;
    private View llTimerTaskCard;
    private Button btnStart;
    private android.widget.ProgressBar pbTimerRing;

    private Handler handler;
    private long totalDurationSeconds = 25 * 60;
    private long remainingSeconds = 25 * 60;
    private long startTime = 0;
    private long startRemainingSeconds = 0;
    private long lastElapsedSeconds = 0;
    private boolean running = false;
    private boolean paused = false;
    private Runnable tickRunnable;

    private boolean isCountdownMode = true;
    private boolean allowOverflowMode = false;
    private boolean triggeredOverflowAlarm = false;

    private TaskDatabaseHelper dbHelper;
    private Task activeTask = null;
    private long todayStudySeconds = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_timer, container, false);

        tvTimerDisplay = view.findViewById(R.id.tv_timer_display);
        tvTimerTask = view.findViewById(R.id.tv_timer_task);
        tvTodayTotal = view.findViewById(R.id.tv_today_total);
        llTimerTaskCard = view.findViewById(R.id.ll_timer_task_card);
        btnStart = view.findViewById(R.id.btn_start);
        pbTimerRing = view.findViewById(R.id.pb_timer_ring);

        dbHelper = new TaskDatabaseHelper(requireContext());

        if (savedInstanceState != null) {
            running = savedInstanceState.getBoolean("running");
            paused = savedInstanceState.getBoolean("paused");
            startTime = savedInstanceState.getLong("startTime");
            startRemainingSeconds = savedInstanceState.getLong("startRemainingSeconds");
            remainingSeconds = savedInstanceState.getLong("remainingSeconds");
            totalDurationSeconds = savedInstanceState.getLong("totalDurationSeconds", 25 * 60);
            isCountdownMode = savedInstanceState.getBoolean("isCountdownMode");
            allowOverflowMode = savedInstanceState.getBoolean("allowOverflowMode");
            triggeredOverflowAlarm = savedInstanceState.getBoolean("triggeredOverflowAlarm");
            long activeTaskId = savedInstanceState.getLong("activeTaskId", -1);
            if (activeTaskId != -1) {
                activeTask = dbHelper.getTaskById(activeTaskId);
            }
        }

        handler = new Handler(Looper.getMainLooper());

        llTimerTaskCard.setOnClickListener(v -> showTaskSelectionDialog());

        btnStart.setOnClickListener(v -> {
            if (!running) {
                // 开始 / 继续
                running = true;
                startTime = System.currentTimeMillis();
                if (paused) {
                    // 继续：从暂停时的剩余/已计时间继续
                    startRemainingSeconds = remainingSeconds;
                } else {
                    // 全新开始
                    startRemainingSeconds = isCountdownMode ? totalDurationSeconds : 0;
                    remainingSeconds = startRemainingSeconds;
                }
                lastElapsedSeconds = 0;
                triggeredOverflowAlarm = false;
                btnStart.setText("暂停");
                btnStart.setTextColor(requireContext().getColor(R.color.stat_orange));
                paused = false;
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).refreshScreenGlow();
                }
                startTick();
            } else {
                // 正在运行 → 弹出暂停/完成确认
                pauseOrStopTimer();
            }
        });

        // 恢复或更新界面状态
        if (running && !paused) {
            long elapsedSecs = (System.currentTimeMillis() - startTime) / 1000;
            lastElapsedSeconds = elapsedSecs;
            if (isCountdownMode) {
                remainingSeconds = startRemainingSeconds - elapsedSecs;
            } else {
                remainingSeconds = startRemainingSeconds + elapsedSecs;
            }
            btnStart.setText("暂停");
            btnStart.setTextColor(requireContext().getColor(R.color.stat_orange));
            paused = false;
            startTick();
        } else {
            updateDisplay();
            if (paused) {
                btnStart.setText("继续");
                btnStart.setTextColor(requireContext().getColor(R.color.stat_green));
            } else {
                btnStart.setText("开始");
                btnStart.setTextColor(requireContext().getColor(R.color.stat_green));
            }
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadTodayStudySeconds();
        updateTodayTotalView();
        syncActiveTask();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // 不调用 stopTimer()，保证计时器在 Tab 切换时常驻内存
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("running", running);
        outState.putBoolean("paused", paused);
        outState.putLong("startTime", startTime);
        outState.putLong("startRemainingSeconds", startRemainingSeconds);
        outState.putLong("remainingSeconds", remainingSeconds);
        outState.putLong("totalDurationSeconds", totalDurationSeconds);
        outState.putBoolean("isCountdownMode", isCountdownMode);
        outState.putBoolean("allowOverflowMode", allowOverflowMode);
        outState.putBoolean("triggeredOverflowAlarm", triggeredOverflowAlarm);
        if (activeTask != null) {
            outState.putLong("activeTaskId", activeTask.id);
        }
    }

    private void startTick() {
        if (tickRunnable != null) {
            handler.removeCallbacks(tickRunnable);
        }
        tickRunnable = new Runnable() {
            @Override
            public void run() {
                if (running) {
                    long elapsedMillis = System.currentTimeMillis() - startTime;
                    long elapsedSecs = elapsedMillis / 1000;

                    if (isCountdownMode) {
                        remainingSeconds = startRemainingSeconds - elapsedSecs;
                    } else {
                        remainingSeconds = startRemainingSeconds + elapsedSecs;
                    }

                    // 实时累加今日学习时间（只对实际流逝时间计数）
                    long newSeconds = elapsedSecs - lastElapsedSeconds;
                    if (newSeconds > 0) {
                        todayStudySeconds += newSeconds;
                        lastElapsedSeconds = elapsedSecs;
                        saveTodayStudySeconds();
                        updateTodayTotalView();
                    }

                    if (isCountdownMode) {
                        if (remainingSeconds <= 0) {
                            remainingSeconds = 0;
                            updateDisplay();
                            onCountdownFinished();
                        } else {
                            updateDisplay();
                            handler.postDelayed(this, 200);
                        }
                    } else {
                        // 正计时：勾选溢出后可超过计划时长，否则到达计划时长停止
                        if (elapsedSecs >= totalDurationSeconds && !allowOverflowMode) {
                            remainingSeconds = totalDurationSeconds;
                            updateDisplay();
                            onCountdownFinished();
                            return;
                        }
                        if (elapsedSecs >= totalDurationSeconds && !triggeredOverflowAlarm) {
                            triggeredOverflowAlarm = true;
                            Toast.makeText(requireContext(), "计划已到时，正在记录溢出时间...", Toast.LENGTH_SHORT).show();
                        }
                        updateDisplay();
                        handler.postDelayed(this, 200);
                    }
                }
            }
        };
        handler.post(tickRunnable);
    }

    public void stopTimer() {
        running = false;
        paused = false;
        if (tickRunnable != null) {
            handler.removeCallbacks(tickRunnable);
        }
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).refreshScreenGlow();
        }
    }

    private void pauseOrStopTimer() {
        if (activeTask == null) {
            // 无绑定任务，直接暂停
            running = false;
            paused = true;
            handler.removeCallbacks(tickRunnable);
            btnStart.setText("继续");
            btnStart.setTextColor(requireContext().getColor(R.color.stat_green));
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).refreshScreenGlow();
            }
            return;
        }

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_pause_timer, null);
        TextView tvMsg = dialogView.findViewById(R.id.tv_pause_message);
        tvMsg.setText("是否将当前计划「" + activeTask.content + "」标记为已完成？");

        AlertDialog pauseStopDialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        dialogView.findViewById(R.id.btn_pause_complete).setOnClickListener(v -> {
            dbHelper.updateTaskStatus(activeTask.id, 1);
            activeTask.status = 1;
            CalendarHelper.updateCalendarStatus(requireContext(), activeTask);
            Toast.makeText(requireContext(), "已更新计划状态", Toast.LENGTH_SHORT).show();

            SharedPreferences prefs = requireContext().getSharedPreferences("study_plan_prefs", Context.MODE_PRIVATE);
            prefs.edit().putLong("pref_timing_task_id", -1L).apply();

            stopTimer();
            activeTask = null;
            autoLoadNextTask();

            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).refreshStats();
            }
            pauseStopDialog.dismiss();
        });

        dialogView.findViewById(R.id.btn_pause_later).setOnClickListener(v -> {
            running = false;
            paused = true;
            handler.removeCallbacks(tickRunnable);
            btnStart.setText("继续");
            btnStart.setTextColor(requireContext().getColor(R.color.stat_green));
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).refreshScreenGlow();
            }
            pauseStopDialog.dismiss();
        });

        pauseStopDialog.show();
        if (pauseStopDialog.getWindow() != null) {
            pauseStopDialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            pauseStopDialog.getWindow().setWindowAnimations(0);
        }
    }

    private void updateDisplay() {
        long displaySeconds;
        boolean negative = false;
        if (remainingSeconds < 0) {
            negative = true;
            displaySeconds = Math.abs(remainingSeconds);
        } else {
            displaySeconds = remainingSeconds;
        }
        long h = displaySeconds / 3600;
        long m = (displaySeconds % 3600) / 60;
        long s = displaySeconds % 60;
        String prefix = negative ? "-" : "";
        tvTimerDisplay.setText(String.format(Locale.US, "%s%02d:%02d:%02d", prefix, h, m, s));

        // 更新专注环进度
        if (pbTimerRing != null && totalDurationSeconds > 0) {
            double ratio = (double) remainingSeconds / totalDurationSeconds;
            int progress = (int) (ratio * 1000);
            pbTimerRing.setProgress(Math.max(0, Math.min(1000, progress)));
        }

    }

    private void updateTaskView() {
        if (activeTask != null) {
            tvTimerTask.setText(String.format(Locale.getDefault(),
                    "当前任务: [%s] %s (%d分钟)",
                    activeTask.subject, activeTask.content, activeTask.duration));
        } else {
            tvTimerTask.setText("不绑定任务 (自定义 25分钟)");
        }
    }

    private void updateTodayTotalView() {
        long minutes = todayStudySeconds / 60;
        tvTodayTotal.setText(getString(R.string.timer_today_total, minutes));
    }

    private void loadTodayStudySeconds() {
        if (getContext() == null) return;
        SharedPreferences prefs = getContext().getSharedPreferences("study_plan_prefs", Context.MODE_PRIVATE);
        String key = "pref_today_study_seconds_" + LocalDate.now().toString();
        todayStudySeconds = prefs.getLong(key, 0);
    }

    private void saveTodayStudySeconds() {
        if (getContext() == null) return;
        SharedPreferences prefs = getContext().getSharedPreferences("study_plan_prefs", Context.MODE_PRIVATE);
        String key = "pref_today_study_seconds_" + LocalDate.now().toString();
        prefs.edit().putLong(key, todayStudySeconds).apply();
    }

    private List<Task> getPendingTasks() {
        List<Task> pending = new ArrayList<>();
        if (getContext() == null) return pending;
        TaskDatabaseHelper db = new TaskDatabaseHelper(getContext());
        List<Task> all = db.getAllTasks();
        for (Task t : all) {
            if (t.status == 0 && t.duration > 0) {
                pending.add(t);
            }
        }
        return pending;
    }

    private void syncActiveTask() {
        if (running) return;

        if (activeTask != null) {
            Task dbTask = dbHelper.getTaskById(activeTask.id);
            if (dbTask == null) {
                activeTask = null;
            }
        }

        List<Task> pending = getPendingTasks();
        if (activeTask != null) {
            boolean stillPending = false;
            for (Task t : pending) {
                if (t.id == activeTask.id) {
                    stillPending = true;
                    activeTask = t;
                    totalDurationSeconds = t.duration * 60;
                    break;
                }
            }
            if (!stillPending) {
                activeTask = null;
            }
        }

        if (activeTask == null) {
            if (!pending.isEmpty()) {
                activeTask = pending.get(0);
                totalDurationSeconds = activeTask.duration * 60;
            } else {
                activeTask = null;
                totalDurationSeconds = 25 * 60;
            }
            remainingSeconds = totalDurationSeconds;
        }

        updateTaskView();
        updateDisplay();
    }

    private void showTaskSelectionDialog() {
        if (getContext() == null) return;

        if (running) {
            AlertDialog changeTaskConfirm = new AlertDialog.Builder(requireContext())
                    .setTitle("提示")
                    .setMessage("当前正在计时，切换任务将终止并重置当前计时，确定要切换吗？")
                    .setPositiveButton("确定", (dialog, which) -> {
                        stopTimer();
                        showSelectionList();
                    })
                    .setNegativeButton("取消", null)
                    .create();
            changeTaskConfirm.show();
            DialogHelper.styleDialog(changeTaskConfirm, false);
            if (changeTaskConfirm.getWindow() != null) {
                changeTaskConfirm.getWindow().setWindowAnimations(0);
            }
        } else {
            showSelectionList();
        }
    }

    private void showSelectionList() {
        List<Task> pending = getPendingTasks();
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_timer_task_selection, null);
        android.widget.LinearLayout itemContainer = dialogView.findViewById(R.id.ll_timer_task_items);
        AlertDialog listDialog = new AlertDialog.Builder(requireContext()).setView(dialogView).create();
        addTaskChoice(itemContainer, "不绑定任务（自定义 25 分钟）", v -> selectTimerTask(null, listDialog));
        for (Task task : pending) {
            addTaskChoice(itemContainer,
                    String.format(Locale.getDefault(), "[%s] %s（%d 分钟）", task.subject, task.content, task.duration),
                    v -> selectTimerTask(task, listDialog));
        }
        dialogView.findViewById(R.id.btn_task_selection_close).setOnClickListener(v -> listDialog.dismiss());

        listDialog.show();
        if (listDialog.getWindow() != null) {
            listDialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            listDialog.getWindow().setWindowAnimations(0);
        }
    }

    private void addTaskChoice(android.widget.LinearLayout parent, String label, View.OnClickListener listener) {
        TextView row = new TextView(requireContext());
        row.setText(label);
        row.setTextColor(requireContext().getColor(R.color.text_primary));
        row.setTextSize(14);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(16, 16, 16, 16);
        row.setBackgroundResource(R.drawable.bg_dialog_input);
        row.setOnClickListener(listener);
        android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, 0, 0, 8);
        parent.addView(row, params);
    }

    private void selectTimerTask(Task task, AlertDialog dialog) {
        activeTask = task;
        totalDurationSeconds = task == null ? 25 * 60 : task.duration * 60;
        remainingSeconds = totalDurationSeconds;
        isCountdownMode = true;
        allowOverflowMode = false;
        triggeredOverflowAlarm = false;
        paused = false;
        running = false;
        updateTaskView();
        updateDisplay();
        btnStart.setText("开始");
        btnStart.setTextColor(requireContext().getColor(R.color.stat_green));
        dialog.dismiss();
    }

    private void onCountdownFinished() {
        running = false;
        paused = false;

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).refreshScreenGlow();
        }

        triggerAlert();

        if (getContext() == null || getActivity() == null) return;

        if (activeTask != null) {
            final Task task = activeTask;
            AlertDialog finishDialog = new AlertDialog.Builder(requireContext())
                    .setTitle("学习时间到")
                    .setMessage("恭喜您完成了学习计划「" + task.content + "」！\n是否将其标记为已完成？")
                    .setPositiveButton("是的，已完成", (dialog, which) -> {
                        dbHelper.updateTaskStatus(task.id, 1);
                        task.status = 1;
                        CalendarHelper.updateCalendarStatus(requireContext(), task);

                        SharedPreferences prefs = requireContext().getSharedPreferences("study_plan_prefs", Context.MODE_PRIVATE);
                        prefs.edit().putLong("pref_timing_task_id", -1L).apply();

                        Toast.makeText(requireContext(), "已更新计划状态", Toast.LENGTH_SHORT).show();
                        if (getActivity() instanceof MainActivity) {
                            ((MainActivity) getActivity()).refreshStats();
                        }
                        activeTask = null;
                        autoLoadNextTask();
                    })
                    .setNegativeButton("稍后处理", (dialog, which) -> {
                        remainingSeconds = totalDurationSeconds;
                        updateDisplay();
                        btnStart.setText("开始");
                        btnStart.setTextColor(requireContext().getColor(R.color.stat_green));
                    })
                    .setCancelable(false)
                    .create();
            finishDialog.show();
            DialogHelper.styleDialog(finishDialog, false);
            if (finishDialog.getWindow() != null) {
                finishDialog.getWindow().setWindowAnimations(0);
            }
        } else {
            AlertDialog endDialog = new AlertDialog.Builder(requireContext())
                    .setTitle("时间到")
                    .setMessage("专注时间已结束，休息一下吧！")
                    .setPositiveButton("确定", (dialog, which) -> {
                        remainingSeconds = totalDurationSeconds;
                        updateDisplay();
                        btnStart.setText("开始");
                        btnStart.setTextColor(requireContext().getColor(R.color.stat_green));
                    })
                    .setCancelable(false)
                    .create();
            endDialog.show();
            DialogHelper.styleDialog(endDialog, false);
            if (endDialog.getWindow() != null) {
                endDialog.getWindow().setWindowAnimations(0);
            }
        }
    }

    private void autoLoadNextTask() {
        List<Task> pending = getPendingTasks();
        if (!pending.isEmpty()) {
            activeTask = pending.get(0);
            totalDurationSeconds = activeTask.duration * 60;
        } else {
            activeTask = null;
            totalDurationSeconds = 25 * 60;
        }
        isCountdownMode = true;
        allowOverflowMode = false;
        triggeredOverflowAlarm = false;
        remainingSeconds = totalDurationSeconds;
        updateTaskView();
        updateDisplay();
        btnStart.setText("开始");
        btnStart.setTextColor(requireContext().getColor(R.color.stat_green));
    }

    private void triggerAlert() {
        if (getContext() == null) return;
        SharedPreferences prefs = getContext().getSharedPreferences("study_plan_prefs", Context.MODE_PRIVATE);
        boolean vibrateEnabled = prefs.getBoolean("pref_vibrate_enabled", true);
        if (vibrateEnabled) {
            Vibrator v = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(800, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    v.vibrate(800);
                }
            }
        }
        Toast.makeText(requireContext(), "专注时间结束！", Toast.LENGTH_LONG).show();
    }

    public void startTimingFromHome(Task task, boolean isCountdown, boolean allowOverflow) {
        stopTimer();
        activeTask = task;
        isCountdownMode = isCountdown;
        allowOverflowMode = allowOverflow;
        triggeredOverflowAlarm = false;
        totalDurationSeconds = task.duration * 60;
        startRemainingSeconds = isCountdownMode ? totalDurationSeconds : 0;
        remainingSeconds = startRemainingSeconds;
        running = true;
        paused = false;
        lastElapsedSeconds = 0;
        startTime = System.currentTimeMillis();

        SharedPreferences prefs = requireContext().getSharedPreferences("study_plan_prefs", Context.MODE_PRIVATE);
        prefs.edit().putLong("pref_timing_task_id", task.id).apply();

        if (btnStart != null) {
            updateTaskView();
            updateDisplay();
            btnStart.setText("暂停");
            btnStart.setTextColor(requireContext().getColor(R.color.stat_orange));
            startTick();
        }
        // btnStart == null → running=true 会在 onCreateView 中自动唤起
    }

    public void resetTimerExternally() {
        stopTimer();
        activeTask = null;
        autoLoadNextTask();
    }

    public Task getActiveTask() {
        return activeTask;
    }

    public boolean isTimerRunning() {
        return running;
    }
}
