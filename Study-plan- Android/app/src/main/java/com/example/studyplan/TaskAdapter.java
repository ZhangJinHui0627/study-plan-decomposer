package com.example.studyplan;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(long taskId);
    }

    public interface OnSelectionChangeListener {
        void onSelectionChanged(int count);
    }

    private List<Task> tasks;
    private final OnItemClickListener clickListener;
    private OnSelectionChangeListener selectChangeListener;

    private boolean isMultiSelectMode = false;
    private final Set<Long> selectedIds = new HashSet<>();

    private long timingTaskId = -1;
    private boolean isManualOrder = false;
    private boolean manualCompletionEnabled = false;
    private final java.util.Set<Long> expandedTaskIds = new java.util.HashSet<>();

    public TaskAdapter(List<Task> tasks, OnItemClickListener clickListener) {
        this.tasks = tasks != null ? tasks : new ArrayList<>();
        this.clickListener = clickListener;
    }

    public void setTasks(List<Task> tasks) {
        this.tasks = tasks != null ? tasks : new ArrayList<>();
        Set<Long> availableIds = new HashSet<>();
        for (Task task : this.tasks) {
            availableIds.add(task.id);
        }
        selectedIds.retainAll(availableIds);
        notifyDataSetChanged();
    }

    public List<Task> getTasks() {
        return tasks;
    }

    public boolean isMultiSelectMode() {
        return isMultiSelectMode;
    }

    public void setMultiSelectMode(boolean multiSelectMode) {
        isMultiSelectMode = multiSelectMode;
    }

    public void setManualCompletionEnabled(boolean enabled) {
        if (manualCompletionEnabled != enabled) {
            manualCompletionEnabled = enabled;
            notifyDataSetChanged();
        }
    }

    public Set<Long> getSelectedIds() {
        return selectedIds;
    }

    public void setOnSelectionChangeListener(OnSelectionChangeListener listener) {
        this.selectChangeListener = listener;
    }

    public long getTimingTaskId() {
        return timingTaskId;
    }

    public void setTimingTaskId(long timingTaskId) {
        this.timingTaskId = timingTaskId;
    }

    public boolean isManualOrder() {
        return isManualOrder;
    }

    public void setManualOrder(boolean manualOrder) {
        isManualOrder = manualOrder;
    }

    /**
     * 三层默认排序：计时中 → 未完成 → 已完成
     */
    public void sortThreeLayers(long timingTaskId) {
        if (tasks == null || tasks.isEmpty()) return;
        this.timingTaskId = timingTaskId;
        Collections.sort(tasks, (a, b) -> {
            // 第一层：计时中排最前
            if (a.id == timingTaskId && b.id != timingTaskId) return -1;
            if (a.id != timingTaskId && b.id == timingTaskId) return 1;
            // 第二层：未完成(0) → 已完成(1)
            if (a.status != b.status) return a.status - b.status;
            // 第三层：相同状态下 priority 降序 → date 升序 → specificTime 升序
            if (a.priority != b.priority) return b.priority - a.priority;
            if (a.date == null && b.date == null) return 0;
            if (a.date == null) return 1;
            if (b.date == null) return -1;
            int dateCmp = a.date.compareTo(b.date);
            if (dateCmp != 0) return dateCmp;
            // specificTime 升序
            if (a.specificTime == null && b.specificTime == null) return 0;
            if (a.specificTime == null) return 1;
            if (b.specificTime == null) return -1;
            return a.specificTime.compareTo(b.specificTime);
        });
        notifyDataSetChanged();
    }

    /**
     * 判断任务是否逾期（仅未完成任务）
     */
    public boolean checkIsOverdue(Task task) {
        if (task.status != 0) return false; // 已完成不算逾期
        if (task.date == null) return false;
        LocalDate today = LocalDate.now();
        LocalDate taskDate;
        try {
            taskDate = LocalDate.parse(task.date);
        } catch (Exception e) {
            return false;
        }
        if (taskDate.isBefore(today)) return true;
        if (taskDate.isAfter(today)) return false;
        // 日期是今天，检查是否已过具体时刻
        if (task.specificTime == null || task.specificTime.isEmpty()) return false;
        // 如果正在计时中，不算逾期
        if (task.id == timingTaskId) return false;
        try {
            LocalTime taskTime = LocalTime.parse(task.specificTime, DateTimeFormatter.ofPattern("HH:mm", Locale.US));
            return LocalTime.now().isAfter(taskTime);
        } catch (Exception e) {
            return false;
        }
    }

    public void onItemMove(int fromPos, int toPos) {
        Collections.swap(tasks, fromPos, toPos);
        notifyItemMoved(fromPos, toPos);
        isManualOrder = true;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Task task = tasks.get(position);
        Context ctx = holder.itemView.getContext();

        // 多选模式：显示 CheckBox
        holder.cbSelect.setVisibility(isMultiSelectMode ? View.VISIBLE : View.GONE);
        holder.cbSelect.setChecked(selectedIds.contains(task.id));

        // 左侧头像：科目首字
        String avatarText = (task.subject != null && !task.subject.isEmpty())
                ? String.valueOf(task.subject.charAt(0))
                : "学";
        holder.tvAvatarText.setText(avatarText);
        holder.vAvatarBg.setBackgroundColor(getAvatarColor(avatarText, ctx));

        SpannableStringBuilder ssb = new SpannableStringBuilder();
        if (task.priority >= 2) {
            int start = ssb.length();
            ssb.append("[加急] ");
            int end = ssb.length();
            ssb.setSpan(new ForegroundColorSpan(ContextCompat.getColor(ctx, R.color.priority_high)),
                    start, end - 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            ssb.setSpan(new StyleSpan(android.graphics.Typeface.BOLD),
                    start, end - 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else if (task.priority == 1) {
            int start = ssb.length();
            ssb.append("[优先] ");
            int end = ssb.length();
            ssb.setSpan(new ForegroundColorSpan(ContextCompat.getColor(ctx, R.color.priority_mid)),
                    start, end - 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            ssb.setSpan(new StyleSpan(android.graphics.Typeface.BOLD),
                    start, end - 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        String titleStr = "";
        if (task.subject != null && !task.subject.isEmpty()) {
            titleStr = task.subject;
        }
        if (task.timeRange != null && !task.timeRange.isEmpty()) {
            if (!titleStr.isEmpty()) {
                titleStr += " · " + task.timeRange;
            } else {
                titleStr = task.timeRange;
            }
        }
        if (titleStr.isEmpty()) {
            titleStr = "学习计划";
        }
        ssb.append(titleStr);
        holder.tvTitle.setText(ssb);

        // 预览：任务内容
        holder.tvPreview.setText(task.content != null ? task.content : "");

        // 右侧日期：取 MM-DD
        if (task.date != null && task.date.length() == 10 && task.date.charAt(4) == '-') {
            holder.tvDate.setText(task.date.substring(5));
        } else {
            holder.tvDate.setText(task.date);
        }

        // ---- 四种卡片状态 ----
        boolean isTiming = task.id == timingTaskId;
        boolean isCompleted = task.status == 1;
        boolean isOverdue = checkIsOverdue(task);
        holder.tvClickComplete.setVisibility(
                manualCompletionEnabled && !isCompleted && !isTiming && !isMultiSelectMode
                        ? View.VISIBLE : View.GONE);

        if (isTiming) {
            // 计时中：蓝色玻璃卡片，不带中划线，文字默认
            holder.cardContainer.setBackgroundResource(R.drawable.bg_glass_card_timing);
            holder.cardContainer.setAlpha(1.0f);
            holder.tvTitle.setPaintFlags(holder.tvTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.tvPreview.setPaintFlags(holder.tvPreview.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.tvTitle.setTextColor(ContextCompat.getColor(ctx, R.color.text_primary));
            holder.tvPreview.setTextColor(ContextCompat.getColor(ctx, R.color.text_secondary));
            holder.tvDate.setTextColor(getDateColor(task, ctx));

        } else if (isCompleted) {
            // 已完成：半透明玻璃卡片 + 中划线 + 灰色文字
            holder.cardContainer.setBackgroundResource(R.drawable.bg_glass_card);
            holder.cardContainer.setAlpha(0.4f);
            holder.tvTitle.setPaintFlags(holder.tvTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvPreview.setPaintFlags(holder.tvPreview.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvTitle.setTextColor(Color.parseColor("#9E9E9E"));
            holder.tvPreview.setTextColor(Color.parseColor("#9E9E9E"));
            holder.tvDate.setTextColor(Color.parseColor("#9E9E9E"));

        } else if (isOverdue) {
            // 逾期只用日期颜色提示，避免整张卡片变红并与优先级颜色混淆。
            holder.cardContainer.setBackgroundResource(R.drawable.bg_glass_card);
            holder.cardContainer.setAlpha(1.0f);
            holder.tvTitle.setPaintFlags(holder.tvTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.tvPreview.setPaintFlags(holder.tvPreview.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.tvTitle.setTextColor(ContextCompat.getColor(ctx, R.color.text_primary));
            holder.tvPreview.setTextColor(ContextCompat.getColor(ctx, R.color.text_secondary));
            holder.tvDate.setTextColor(Color.parseColor("#C62828"));

        } else {
            // 普通未完成：默认玻璃卡片
            holder.cardContainer.setBackgroundResource(R.drawable.bg_glass_card);
            holder.cardContainer.setAlpha(1.0f);
            holder.tvTitle.setPaintFlags(holder.tvTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.tvPreview.setPaintFlags(holder.tvPreview.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.tvTitle.setTextColor(ContextCompat.getColor(ctx, R.color.text_primary));
            holder.tvPreview.setTextColor(ContextCompat.getColor(ctx, R.color.text_secondary));
            holder.tvDate.setTextColor(getDateColor(task, ctx));
        }

        // 折叠/展开直接显隐以保证滑动流畅度
        boolean isExpanded = expandedTaskIds.contains(task.id);
        if (isExpanded && !isMultiSelectMode) {
            holder.llExpandedDetails.setVisibility(View.VISIBLE);
        } else {
            holder.llExpandedDetails.setVisibility(View.GONE);
        }

        holder.tvDetailInfo.setText("计划时长: " + task.duration + " 分钟"
                + (task.pages > 0 ? " | 计划阅读: " + task.pages + " 页" : ""));

        // 按照打卡首选项，动态调整工具栏操作按钮文字
        boolean manualEnabledCheck = ctx.getSharedPreferences("study_plan_prefs", Context.MODE_PRIVATE)
                .getBoolean("pref_manual_complete_enabled", true);
        if (manualEnabledCheck) {
            holder.tvDetailAction.setText(task.status == 1 ? "重做任务" : "标记完成");
        } else {
            holder.tvDetailAction.setText(task.status == 1 ? "重做任务" : "开始专注");
        }

        // 快捷操作动作绑定
        holder.tvDetailAction.setOnClickListener(v -> {
            clickListener.onItemClick(task.id);
            // 同步日历日程状态
            CalendarHelper.updateCalendarStatus(ctx, task);
            if (task.status == 1 && ctx instanceof MainActivity) {
                ((MainActivity) ctx).onTaskCompletedExternally(task.id);
            }
        });

        // 点击整行切换展开折叠
        holder.itemRow.setOnClickListener(v -> {
            int currentPosition = holder.getBindingAdapterPosition();
            if (currentPosition == RecyclerView.NO_POSITION) return;
            Task currentTask = tasks.get(currentPosition);
            if (isMultiSelectMode) {
                if (selectedIds.contains(currentTask.id)) {
                    selectedIds.remove(currentTask.id);
                } else {
                    selectedIds.add(currentTask.id);
                }
                notifyItemChanged(currentPosition);
                if (selectChangeListener != null) {
                    selectChangeListener.onSelectionChanged(selectedIds.size());
                }
            } else {
                if (expandedTaskIds.contains(currentTask.id)) {
                    expandedTaskIds.remove(currentTask.id);
                } else {
                    expandedTaskIds.add(currentTask.id);
                }
                notifyItemChanged(currentPosition);
            }
        });
        holder.itemView.setOnLongClickListener(null);
        holder.itemView.setLongClickable(false);
    }

    private int getDateColor(Task task, Context ctx) {
        if (task.priority >= 2) {
            return ContextCompat.getColor(ctx, R.color.priority_high);
        } else if (task.priority == 1) {
            return ContextCompat.getColor(ctx, R.color.priority_mid);
        } else {
            return ContextCompat.getColor(ctx, R.color.text_secondary);
        }
    }

    @Override
    public int getItemCount() {
        return tasks != null ? tasks.size() : 0;
    }

    private int getAvatarColor(String text, Context ctx) {
        char c = text.charAt(0);
        int[] colors = {
                R.color.avatar_blue, R.color.avatar_green, R.color.avatar_orange,
                R.color.avatar_red, R.color.avatar_purple, R.color.avatar_teal
        };
        return ContextCompat.getColor(ctx, colors[Math.abs(c) % colors.length]);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout cardContainer, itemRow, llExpandedDetails;
        View vAvatarBg;
        TextView tvAvatarText, tvTitle, tvPreview, tvDate, tvClickComplete, tvDetailInfo, tvDetailAction;
        CheckBox cbSelect;

        ViewHolder(View itemView) {
            super(itemView);
            cardContainer = itemView.findViewById(R.id.card_container);
            itemRow = itemView.findViewById(R.id.item_row);
            llExpandedDetails = itemView.findViewById(R.id.ll_expanded_details);
            vAvatarBg = itemView.findViewById(R.id.v_avatar_bg);
            tvAvatarText = itemView.findViewById(R.id.tv_avatar_text);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvPreview = itemView.findViewById(R.id.tv_preview);
            tvDate = itemView.findViewById(R.id.tv_date);
            tvClickComplete = itemView.findViewById(R.id.tv_click_complete);
            tvDetailInfo = itemView.findViewById(R.id.tv_detail_info);
            tvDetailAction = itemView.findViewById(R.id.tv_detail_action);
            cbSelect = itemView.findViewById(R.id.cb_select);
        }
    }
}
