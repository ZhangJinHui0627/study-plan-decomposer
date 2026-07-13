package com.example.studyplan;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import androidx.core.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HomeFragment extends Fragment {

    private RecyclerView rvTasks;
    private TextView tvEmpty;
    private TaskDatabaseHelper dbHelper;
    private TaskAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        rvTasks = view.findViewById(R.id.rv_tasks);
        tvEmpty = view.findViewById(R.id.tv_empty);

        dbHelper = new TaskDatabaseHelper(requireContext());
        List<Task> tasks = dbHelper.getAllTasks();

        adapter = new TaskAdapter(tasks, taskId -> {
                    int pos = findTaskPosition(taskId);
                    if (pos < 0) return;
                    Task task = adapter.getTasks().get(pos);
                    TaskInteractionPolicy.TapAction action =
                            TaskInteractionPolicy.tapAction(true, task.status);
                    if (action == TaskInteractionPolicy.TapAction.TOGGLE_STATUS) {
                        int newStatus = dbHelper.toggleTaskStatus(taskId);
                        task.status = newStatus;
                        CalendarHelper.updateCalendarStatus(requireContext(), task);
                        if (task.status == 1 && getActivity() instanceof MainActivity) {
                            ((MainActivity) getActivity()).onTaskCompletedExternally(task.id);
                        }
                        refreshList();
                        refreshStats();
                    } else if (action == TaskInteractionPolicy.TapAction.OPEN_TIMER) {
                        showTimerOptionsDialog(task);
                    }
                });

        adapter.setOnStartFocusClickListener(task -> showTimerOptionsDialog(task));

        adapter.setOnSelectionChangeListener(count ->
                ((TextView) view.findViewById(R.id.tv_selected_count)).setText("已选择 " + count + " 项"));

        rvTasks.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvTasks.setAdapter(adapter);
        SharedPreferences initialPrefs = requireContext()
                .getSharedPreferences("study_plan_prefs", Context.MODE_PRIVATE);
        adapter.sortThreeLayers(initialPrefs.getLong("pref_timing_task_id", -1L));

        // 底部多选栏按钮
        view.findViewById(R.id.btn_cancel_select).setOnClickListener(v -> exitMultiSelectMode());
        view.findViewById(R.id.btn_batch_delete).setOnClickListener(v -> {
            Set<Long> selected = new HashSet<>(adapter.getSelectedIds());
            if (selected.isEmpty()) {
                Toast.makeText(requireContext(), "请先选择任务", Toast.LENGTH_SHORT).show();
                return;
            }
            AlertDialog batchDeleteDialog = new AlertDialog.Builder(requireContext())
                    .setTitle("批量删除")
                    .setMessage("确定要删除选中的 " + selected.size() + " 个任务吗？")
                    .setPositiveButton("删除", (dialog, which) -> {
                        for (long taskId : selected) {
                            Task t = dbHelper.getTaskById(taskId);
                            if (t != null) {
                                CalendarHelper.deleteFromCalendar(requireContext(), t);
                            }
                            dbHelper.deleteTask(taskId);
                            AlarmScheduler.cancelAlarm(requireContext(), taskId);
                        }
                        exitMultiSelectMode();
                        refreshList();
                        refreshStats();
                        Toast.makeText(requireContext(), "已批量删除 " + selected.size() + " 个任务", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("取消", null)
                    .create();
            batchDeleteDialog.show();
            if (batchDeleteDialog.getWindow() != null) {
                batchDeleteDialog.getWindow().setWindowAnimations(0);
            }
        });

        // 右滑：手动打卡开启时切换完成状态，关闭时进入多选；左滑：删除
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public int getDragDirs(@NonNull RecyclerView recyclerView,
                                   @NonNull RecyclerView.ViewHolder viewHolder) {
                return adapter.isMultiSelectMode() ? 0
                        : super.getDragDirs(recyclerView, viewHolder);
            }

            @Override
            public int getSwipeDirs(@NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder) {
                return adapter.isMultiSelectMode() ? 0
                        : ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;
            }

            @Override
            public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh,
                                  @NonNull RecyclerView.ViewHolder target) {
                int from = vh.getAdapterPosition();
                int to = target.getAdapterPosition();
                if (from == RecyclerView.NO_POSITION || to == RecyclerView.NO_POSITION) {
                    return false;
                }
                adapter.onItemMove(from, to);
                saveCustomOrder();
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int direction) {
                int pos = vh.getAdapterPosition();
                if (pos < 0 || pos >= adapter.getItemCount()) return;
                if (direction == ItemTouchHelper.LEFT) {
                    Task task = adapter.getTasks().get(pos);
                    AlertDialog swipeDeleteDialog = new AlertDialog.Builder(requireContext())
                            .setTitle("删除任务")
                            .setMessage("确定要删除该任务吗？")
                            .setPositiveButton("删除", (dialog, which) -> {
                                CalendarHelper.deleteFromCalendar(requireContext(), task);
                                dbHelper.deleteTask(task.id);
                                AlarmScheduler.cancelAlarm(requireContext(), task.id);
                                refreshList();
                                refreshStats();
                            })
                            .setNegativeButton("取消", (dialog, which) -> {
                                adapter.notifyItemChanged(pos);
                            })
                            .setOnCancelListener(dialog -> adapter.notifyItemChanged(pos))
                            .create();
                    swipeDeleteDialog.show();
                    if (swipeDeleteDialog.getWindow() != null) {
                        swipeDeleteDialog.getWindow().setWindowAnimations(0);
                    }
                } else if (direction == ItemTouchHelper.RIGHT) {
                    enterMultiSelectMode(pos);
                }
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY,
                                    int actionState, boolean isCurrentlyActive) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    View itemView = viewHolder.itemView;
                    Paint p = new Paint();
                    if (dX > 0) {
                        p.setColor(Color.parseColor("#1A73E8"));
                        RectF background = new RectF((float) itemView.getLeft(), (float) itemView.getTop(), dX, (float) itemView.getBottom());
                        c.drawRect(background, p);

                        Drawable icon = ContextCompat.getDrawable(requireContext(), android.R.drawable.checkbox_on_background);
                        if (icon != null) {
                            int iconMargin = (itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
                            int iconTop = itemView.getTop() + (itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
                            int iconBottom = iconTop + icon.getIntrinsicHeight();
                            int iconLeft = itemView.getLeft() + iconMargin;
                            int iconRight = itemView.getLeft() + iconMargin + icon.getIntrinsicWidth();
                            icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                            icon.draw(c);
                        }
                    } else if (dX < 0) {
                        p.setColor(Color.parseColor("#EF5350"));
                        RectF background = new RectF((float) itemView.getRight() + dX, (float) itemView.getTop(), (float) itemView.getRight(), (float) itemView.getBottom());
                        c.drawRect(background, p);

                        Drawable icon = ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_menu_delete);
                        if (icon != null) {
                            int iconMargin = (itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
                            int iconTop = itemView.getTop() + (itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
                            int iconBottom = iconTop + icon.getIntrinsicHeight();
                            int iconRight = itemView.getRight() - iconMargin;
                            int iconLeft = iconRight - icon.getIntrinsicWidth();
                            icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                            icon.draw(c);
                        }
                    }
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        });
        itemTouchHelper.attachToRecyclerView(rvTasks);

        updateEmptyView();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshList();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && adapter != null && isAdded()) {
            refreshList();
        }
    }

    void refreshList() {
        SharedPreferences prefs = requireContext().getSharedPreferences("study_plan_prefs", Context.MODE_PRIVATE);
        long timingTaskId = prefs.getLong("pref_timing_task_id", -1L);
        List<Task> newTasks = dbHelper.getAllTasks();

        boolean hasChanged = false;
        List<Task> currentTasks = adapter.getTasks();
        if (currentTasks == null || currentTasks.size() != newTasks.size()) {
            hasChanged = true;
        } else {
            java.util.Map<Long, Integer> currentMap = new java.util.HashMap<>();
            for (Task t : currentTasks) {
                currentMap.put(t.id, t.status);
            }
            for (Task t : newTasks) {
                Integer oldStatus = currentMap.get(t.id);
                if (oldStatus == null || !oldStatus.equals(t.status)) {
                    hasChanged = true;
                    break;
                }
            }
        }

        String savedOrder = prefs.getString("pref_tasks_order", "");
        if (!savedOrder.isEmpty() && adapter.isManualOrder()) {
            String[] orderIds = savedOrder.split(",");
            java.util.List<Long> idList = new java.util.ArrayList<>();
            for (String s : orderIds) {
                if (!s.isEmpty()) idList.add(Long.parseLong(s));
            }
            java.util.Collections.sort(newTasks, (a, b) -> {
                int idxA = idList.indexOf(a.id);
                int idxB = idList.indexOf(b.id);
                if (idxA == -1) idxA = Integer.MAX_VALUE;
                if (idxB == -1) idxB = Integer.MAX_VALUE;
                return idxA - idxB;
            });
            adapter.setTasks(newTasks);
            adapter.notifyDataSetChanged();
        } else {
            boolean timingChanged = adapter.getTimingTaskId() != timingTaskId;
            if (hasChanged || timingChanged) {
                adapter.setManualOrder(false);
                adapter.setTasks(newTasks);
                adapter.sortThreeLayers(timingTaskId);
            } else {
                adapter.setTimingTaskId(timingTaskId);
                adapter.notifyDataSetChanged();
            }
        }
        updateEmptyView();
    }

    private void updateEmptyView() {
        if (adapter == null) return;
        if (adapter.getItemCount() == 0) {
            tvEmpty.setVisibility(View.VISIBLE);
            rvTasks.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            rvTasks.setVisibility(View.VISIBLE);
        }
    }

    private int findTaskPosition(long taskId) {
        if (adapter == null) return -1;
        List<Task> tasks = adapter.getTasks();
        if (tasks == null) return -1;
        for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i).id == taskId) return i;
        }
        return -1;
    }

    private void enterMultiSelectMode(int initialPos) {
        View root = getView();
        if (root == null || initialPos < 0 || initialPos >= adapter.getItemCount()) {
            return;
        }
        adapter.setMultiSelectMode(true);
        adapter.getSelectedIds().clear();
        Task task = adapter.getTasks().get(initialPos);
        adapter.getSelectedIds().add(task.id);
        adapter.notifyDataSetChanged();

        View sheet = root.findViewById(R.id.ll_bottom_delete_sheet);
        if (sheet != null) {
            sheet.setVisibility(View.VISIBLE);
            sheet.setTranslationY(sheet.getHeight() + 100);
            sheet.animate()
                    .translationY(0)
                    .setDuration(300)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }

        ((TextView) root.findViewById(R.id.tv_selected_count)).setText("已选择 1 项");
    }

    private void exitMultiSelectMode() {
        adapter.setMultiSelectMode(false);
        adapter.getSelectedIds().clear();
        adapter.notifyDataSetChanged();

        View root = getView();
        if (root == null) return;
        View sheet = root.findViewById(R.id.ll_bottom_delete_sheet);
        if (sheet != null) {
            sheet.animate()
                    .translationY(sheet.getHeight() + 100)
                    .setDuration(250)
                    .setInterpolator(new AccelerateInterpolator())
                    .withEndAction(() -> sheet.setVisibility(View.GONE))
                    .start();
        }
    }

    private void saveCustomOrder() {
        if (adapter == null) return;
        StringBuilder sb = new StringBuilder();
        for (Task t : adapter.getTasks()) {
            sb.append(t.id).append(",");
        }
        requireContext().getSharedPreferences("study_plan_prefs", Context.MODE_PRIVATE)
                .edit().putString("pref_tasks_order", sb.toString()).apply();
    }

    public void showAddDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_plan, null);
        builder.setView(dialogView);

        EditText etInput = dialogView.findViewById(R.id.et_add_input);
        TextView btnParse = dialogView.findViewById(R.id.btn_add_parse);

        AlertDialog dialog = builder.create();

        btnParse.setOnClickListener(v -> {
            String rawText = etInput.getText().toString();
            if (rawText.trim().isEmpty()) {
                Toast.makeText(requireContext(), "请先输入学习计划", Toast.LENGTH_SHORT).show();
                return;
            }

            String cleanText = TextPreprocessor.clean(rawText);
            List<String> subjects = dbHelper.getAllSubjects();
            List<Task> parsed = TaskParser.parse(cleanText, LocalDate.now().toString(), subjects);

            if (!parsed.isEmpty()) {
                android.database.sqlite.SQLiteDatabase db = dbHelper.getWritableDatabase();
                db.beginTransaction();
                try {
                    for (Task task : parsed) {
                        long id = dbHelper.insertTask(db, task);
                        task.id = id;
                        AlarmScheduler.setAlarm(requireContext(), task);
                        CalendarHelper.addToCalendar(requireContext(), task);
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                dialog.dismiss();
                refreshList();
                refreshStats();
                Toast.makeText(requireContext(), "成功拆解并导入了 " + parsed.size() + " 项任务", Toast.LENGTH_SHORT).show();
                etInput.setText("");
            }
        });

        dialog.setOnShowListener(d -> {
            etInput.requestFocus();
            InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(etInput, InputMethodManager.SHOW_IMPLICIT);
        });

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.getWindow().setWindowAnimations(0);
        }
    }

    public void showSearchDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_search, null);
        builder.setView(dialogView);

        EditText etSearch = dialogView.findViewById(R.id.et_search);
        TextView tvResult = dialogView.findViewById(R.id.tv_search_result);

        AlertDialog dialog = builder.create();

        etSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(android.text.Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String keyword = s.toString().trim().toLowerCase();
                List<Task> all = dbHelper.getAllTasks();
                if (keyword.isEmpty()) {
                    tvResult.setText("");
                    return;
                }
                List<Task> filtered = new java.util.ArrayList<>();
                for (Task t : all) {
                    if ((t.subject != null && t.subject.toLowerCase().contains(keyword)) ||
                        (t.content != null && t.content.toLowerCase().contains(keyword)) ||
                        (t.date != null && t.date.contains(keyword))) {
                        filtered.add(t);
                    }
                }
                if (filtered.isEmpty()) {
                    tvResult.setText("未找到匹配的任务");
                } else {
                    StringBuilder sb = new StringBuilder();
                    for (Task t : filtered) {
                        String datePart = t.date != null && t.date.length() >= 5 ? t.date.substring(5) : t.date;
                        sb.append(datePart).append("  ").append(t.subject).append("  ").append(t.content).append("\n");
                    }
                    tvResult.setText(sb.toString());
                }
            }
        });

        dialogView.findViewById(R.id.btn_search_close).setOnClickListener(v -> dialog.dismiss());

        dialog.setOnShowListener(d -> {
            etSearch.requestFocus();
            InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(etSearch, InputMethodManager.SHOW_IMPLICIT);
        });

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.getWindow().setWindowAnimations(0);
        }
    }

    private void refreshStats() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).refreshStats();
        }
    }

    private void showTimerOptionsDialog(Task task) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_timer_options, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext()).setView(dialogView).create();

        TextView btnCountdown = dialogView.findViewById(R.id.btn_mode_countdown);
        TextView btnCountup = dialogView.findViewById(R.id.btn_mode_countup);
        TextView btnOverflow = dialogView.findViewById(R.id.btn_mode_overflow);

        final boolean[] isCountdown = {true};
        final boolean[] allowOverflow = {false};

        // Apply default selection: countdown selected, countup unselected, overflow gone + unselected
        btnCountdown.setBackgroundResource(R.drawable.bg_capsule_selected);
        btnCountdown.setTextColor(Color.parseColor("#1A73E8"));
        btnCountup.setBackgroundResource(R.drawable.bg_capsule_unselected);
        btnCountup.setTextColor(Color.parseColor("#5F6368"));
        btnOverflow.setBackgroundResource(R.drawable.bg_capsule_unselected);
        btnOverflow.setTextColor(Color.parseColor("#5F6368"));

        btnCountdown.setOnClickListener(v -> {
            isCountdown[0] = true;
            allowOverflow[0] = false;
            // Select countdown
            btnCountdown.setBackgroundResource(R.drawable.bg_capsule_selected);
            btnCountdown.setTextColor(Color.parseColor("#1A73E8"));
            // Unselect countup
            btnCountup.setBackgroundResource(R.drawable.bg_capsule_unselected);
            btnCountup.setTextColor(Color.parseColor("#5F6368"));
            // Hide and unselect overflow
            btnOverflow.setVisibility(View.GONE);
            btnOverflow.setBackgroundResource(R.drawable.bg_capsule_unselected);
            btnOverflow.setTextColor(Color.parseColor("#5F6368"));
        });

        btnCountup.setOnClickListener(v -> {
            isCountdown[0] = false;
            // Select countup
            btnCountup.setBackgroundResource(R.drawable.bg_capsule_selected);
            btnCountup.setTextColor(Color.parseColor("#1A73E8"));
            // Unselect countdown
            btnCountdown.setBackgroundResource(R.drawable.bg_capsule_unselected);
            btnCountdown.setTextColor(Color.parseColor("#5F6368"));
            // Show overflow (keep current state)
            btnOverflow.setVisibility(View.VISIBLE);
        });

        btnOverflow.setOnClickListener(v -> {
            allowOverflow[0] = !allowOverflow[0];
            if (allowOverflow[0]) {
                btnOverflow.setBackgroundResource(R.drawable.bg_capsule_selected);
                btnOverflow.setTextColor(Color.parseColor("#1A73E8"));
            } else {
                btnOverflow.setBackgroundResource(R.drawable.bg_capsule_unselected);
                btnOverflow.setTextColor(Color.parseColor("#5F6368"));
            }
        });

        dialogView.findViewById(R.id.btn_dialog_cancel).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.btn_dialog_start).setOnClickListener(v -> {
            if (task.duration <= 0) {
                Toast.makeText(requireContext(), "请先输入计划时长，例如“25分钟”", Toast.LENGTH_LONG).show();
                return;
            }
            dialog.dismiss();
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).startTimingForTask(task, isCountdown[0], allowOverflow[0]);
            }
        });

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.getWindow().setWindowAnimations(0);
        }
    }
}
