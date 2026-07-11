package com.example.studyplan;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

    private static final int REQUEST_AVATAR = 100;

    private SharedPreferences prefs;
    private TextView tvReminderTime, tvDbSize;
    private SwitchCompat swReminder, swVibrate;
    private android.widget.ImageView dialogAvatarView;
    private View dialogAvatarFrame;
    private Bitmap pendingAvatarBitmap;
    private int pendingImageTint;
    private int pendingFrameColor;
    private boolean profileDialogSaved;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        View settingsLayout = findViewById(R.id.settings_layout);
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(settingsLayout, (v, insets) -> {
            int statusBarHeight = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars()).top;
            com.google.android.material.appbar.MaterialToolbar t = findViewById(R.id.toolbar);
            if (t != null) {
                t.setPadding(0, statusBarHeight, 0, 0);
            }
            return insets;
        });

        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setTitle("设置");
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        prefs = getSharedPreferences("study_plan_prefs", MODE_PRIVATE);
        tvReminderTime = findViewById(R.id.tv_reminder_time);
        tvDbSize = findViewById(R.id.tv_db_size);
        swReminder = findViewById(R.id.sw_reminder);
        swVibrate = findViewById(R.id.sw_vibrate);

        // 账号 — 个人资料
        findViewById(R.id.item_person).setOnClickListener(v -> showPersonalDialog());

        // 通知 — 提醒开关
        boolean reminderOn = prefs.getBoolean("pref_reminder_enabled", true);
        swReminder.setChecked(reminderOn);
        swReminder.setOnCheckedChangeListener((btn, checked) -> {
            prefs.edit().putBoolean("pref_reminder_enabled", checked).apply();
            android.view.View reminderTimeItem = findViewById(R.id.item_reminder_time);
            if (reminderTimeItem != null) {
                reminderTimeItem.setEnabled(checked);
                reminderTimeItem.animate().cancel();
                reminderTimeItem.animate()
                        .alpha(checked ? 1.0f : 0.0f)
                        .scaleY(checked ? 1.0f : 0.0f)
                        .setDuration(250)
                        .start();
            }
        });

        String savedTime = prefs.getString("pref_reminder_time", "08:00");
        tvReminderTime.setText(savedTime);
        findViewById(R.id.item_reminder_time)
                .setOnClickListener(v -> {
                    String currentTime = prefs.getString("pref_reminder_time", "08:00");
                    int hour = 8;
                    int minute = 0;
                    try {
                        String[] parts = currentTime.split(":");
                        hour = Integer.parseInt(parts[0]);
                        minute = Integer.parseInt(parts[1]);
                        if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
                            hour = 8;
                            minute = 0;
                        }
                    } catch (RuntimeException ignored) {
                        hour = 8;
                        minute = 0;
                    }
                    showCustomTimePickerDialog(hour, minute);
                });
        findViewById(R.id.item_reminder_time).setEnabled(reminderOn);
        findViewById(R.id.item_reminder_time).setAlpha(reminderOn ? 1.0f : 0.4f);

        // 震动
        swVibrate.setChecked(prefs.getBoolean("pref_vibrate_enabled", true));
        swVibrate.setOnCheckedChangeListener((btn, checked) ->
                prefs.edit().putBoolean("pref_vibrate_enabled", checked).apply());

        // 清空
        findViewById(R.id.item_clear_all).setOnClickListener(v -> {
            AlertDialog dialog = new AlertDialog.Builder(this).create();
            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            float density = getResources().getDisplayMetrics().density;
            layout.setPadding((int) (28 * density), (int) (24 * density),
                    (int) (28 * density), (int) (24 * density));
            layout.setBackgroundResource(R.drawable.bg_dialog_card);

            TextView tvTitle = new TextView(this);
            tvTitle.setText("清空全部任务");
            tvTitle.setTextSize(18);
            tvTitle.setTextColor(Color.parseColor("#1F1F1F"));
            tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
            tvTitle.setPadding(0, 0, 0, 12);
            layout.addView(tvTitle);

            TextView tvMsg = new TextView(this);
            tvMsg.setText("确定要清空全部任务吗？此操作不可恢复。");
            tvMsg.setTextSize(14);
            tvMsg.setTextColor(Color.parseColor("#5F6368"));
            tvMsg.setPadding(0, 0, 0, 20);
            layout.addView(tvMsg);

            LinearLayout buttonLayout = new LinearLayout(this);
            buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
            buttonLayout.setGravity(android.view.Gravity.END);

            TextView tvCancel = new TextView(this);
            tvCancel.setText("取消");
            tvCancel.setTextColor(Color.parseColor("#5F6368"));
            tvCancel.setTextSize(15);
            tvCancel.setPadding(24, 12, 24, 12);
            tvCancel.setClickable(true);
            tvCancel.setFocusable(true);
            tvCancel.setOnClickListener(view -> dialog.dismiss());
            buttonLayout.addView(tvCancel);

            TextView tvClear = new TextView(this);
            tvClear.setText("清空");
            tvClear.setTextColor(Color.parseColor("#E53935"));
            tvClear.setTextSize(15);
            tvClear.setTypeface(null, android.graphics.Typeface.BOLD);
            tvClear.setPadding(24, 12, 24, 12);
            tvClear.setClickable(true);
            tvClear.setFocusable(true);
            tvClear.setOnClickListener(view -> {
                TaskDatabaseHelper db = new TaskDatabaseHelper(this);
                java.util.List<Task> allTasks = db.getAllTasks();
                for (Task t : allTasks) {
                    AlarmScheduler.cancelAlarm(this, t.id);
                    CalendarHelper.deleteFromCalendar(this, t);
                }
                db.deleteAllTasks();
                android.app.NotificationManager nm = (android.app.NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                if (nm != null) nm.cancelAll();
                updateDbSize();
                setResult(RESULT_OK);
                Toast.makeText(this, "已清空全部任务", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
            buttonLayout.addView(tvClear);
            layout.addView(buttonLayout);

            dialog.setView(layout);
            dialog.show();
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
                dialog.getWindow().setWindowAnimations(0);
                dialog.getWindow().setLayout((int) (340 * density),
                        android.view.WindowManager.LayoutParams.WRAP_CONTENT);
            }
        });

        updateDbSize();

        // 规则库管理 — 自定义学科词库
        findViewById(R.id.item_subject_rules).setOnClickListener(v -> showSubjectRulesDialog());

        // 从个人头像点击进入时，自动弹出个人资料弹窗
        if (getIntent().getBooleanExtra("show_personal", false)) {
            showPersonalDialog();
        }

        findViewById(R.id.item_reminder).setOnClickListener(v -> swReminder.toggle());
        findViewById(R.id.item_vibrate).setOnClickListener(v -> swVibrate.toggle());
    }

    private void showPersonalDialog() {
        String nick = prefs.getString("pref_nickname", "我的学习");
        String sig = prefs.getString("pref_signature", "更高效的学习方式");
        pendingAvatarBitmap = null;
        pendingImageTint = prefs.getInt("pref_avatar_image_tint", Color.TRANSPARENT);
        pendingFrameColor = prefs.getInt("pref_avatar_frame_color", Color.TRANSPARENT);
        if (pendingFrameColor == Color.WHITE) pendingFrameColor = Color.TRANSPARENT;
        profileDialogSaved = false;

        AlertDialog dialog = new AlertDialog.Builder(this).create();

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        float density = getResources().getDisplayMetrics().density;
        layout.setPadding((int) (28 * density), (int) (24 * density),
                (int) (28 * density), (int) (24 * density));
        layout.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
        layout.setBackgroundResource(R.drawable.bg_dialog_card);

        // 标题
        TextView tvTitle = new TextView(this);
        tvTitle.setText("个人资料");
        tvTitle.setTextSize(18);
        tvTitle.setTextColor(Color.parseColor("#1F1F1F"));
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTitle.setPadding(0, 0, 0, 16);
        layout.addView(tvTitle);

        FrameLayout avatarFrame = new FrameLayout(this);
        int containerSize = (int) (112 * density);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(containerSize, containerSize);
        avatarFrame.setLayoutParams(lp);

        ImageView ivAvatar = new ImageView(this);
        ivAvatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
        int avatarSize = (int) (96 * density);
        FrameLayout.LayoutParams avatarLp = new FrameLayout.LayoutParams(avatarSize, avatarSize);
        avatarLp.gravity = android.view.Gravity.CENTER;
        avatarFrame.addView(ivAvatar, avatarLp);

        View glassFrame = new View(this);
        int frameSize = (int) (104 * density);
        FrameLayout.LayoutParams frameLp = new FrameLayout.LayoutParams(frameSize, frameSize);
        frameLp.gravity = android.view.Gravity.CENTER;
        avatarFrame.addView(glassFrame, frameLp);

        layout.addView(avatarFrame);
        dialogAvatarView = ivAvatar;
        dialogAvatarFrame = glassFrame;
        applyAvatarPreview(ivAvatar, glassFrame, loadAvatarBitmap(), pendingImageTint, pendingFrameColor);

        android.widget.Button btnAvatar = new android.widget.Button(this);
        btnAvatar.setText("更换头像");
        btnAvatar.setTextColor(Color.parseColor("#1F1F1F"));
        btnAvatar.setBackgroundResource(R.drawable.bg_dialog_btn);
        LinearLayout.LayoutParams btnAvatarLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, (int) (36 * getResources().getDisplayMetrics().density));
        btnAvatarLp.topMargin = 12;
        btnAvatarLp.bottomMargin = 20;
        btnAvatar.setLayoutParams(btnAvatarLp);
        btnAvatar.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, REQUEST_AVATAR);
        });
        layout.addView(btnAvatar);


        TextView tvNickLabel = new TextView(this);
        tvNickLabel.setText("名字");
        tvNickLabel.setTextSize(12);
        tvNickLabel.setTextColor(getColor(R.color.text_secondary));
        tvNickLabel.setPadding(4, 0, 0, 4);
        LinearLayout.LayoutParams nickLabelLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tvNickLabel.setLayoutParams(nickLabelLp);
        layout.addView(tvNickLabel);

        EditText etNick = new EditText(this);
        etNick.setText(nick);
        etNick.setHint("请输入名字");
        etNick.setHintTextColor(Color.parseColor("#805F6368"));
        etNick.setPadding(24, 16, 24, 16);
        etNick.setTextColor(Color.parseColor("#1F1F1F"));
        etNick.setBackgroundResource(R.drawable.bg_dialog_input);
        LinearLayout.LayoutParams nickLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        nickLp.topMargin = 4;
        etNick.setLayoutParams(nickLp);
        layout.addView(etNick);

        TextView tvSigLabel = new TextView(this);
        tvSigLabel.setText("签名");
        tvSigLabel.setTextSize(12);
        tvSigLabel.setTextColor(getColor(R.color.text_secondary));
        tvSigLabel.setPadding(4, 0, 0, 4);
        LinearLayout.LayoutParams sigLabelLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        sigLabelLp.topMargin = 16;
        tvSigLabel.setLayoutParams(sigLabelLp);
        layout.addView(tvSigLabel);

        EditText etSig = new EditText(this);
        etSig.setText(sig);
        etSig.setHint("请输入签名");
        etSig.setHintTextColor(Color.parseColor("#805F6368"));
        etSig.setPadding(24, 16, 24, 16);
        etSig.setTextColor(Color.parseColor("#1F1F1F"));
        etSig.setBackgroundResource(R.drawable.bg_dialog_input);
        LinearLayout.LayoutParams sigLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        sigLp.topMargin = 4;
        etSig.setLayoutParams(sigLp);
        layout.addView(etSig);

        // 底部控制按钮 LinearLayout
        LinearLayout buttonLayout = new LinearLayout(this);
        buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
        buttonLayout.setGravity(android.view.Gravity.END);
        LinearLayout.LayoutParams buttonLayoutLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        buttonLayoutLp.topMargin = 24;
        buttonLayout.setLayoutParams(buttonLayoutLp);

        TextView tvCancel = new TextView(this);
        tvCancel.setText("取消");
        tvCancel.setTextColor(Color.parseColor("#5F6368"));
        tvCancel.setTextSize(15);
        tvCancel.setPadding(24, 12, 24, 12);
        tvCancel.setClickable(true);
        tvCancel.setFocusable(true);
        tvCancel.setOnClickListener(v -> dialog.dismiss());
        buttonLayout.addView(tvCancel);

        TextView tvSave = new TextView(this);
        tvSave.setText("保存");
        tvSave.setTextColor(Color.parseColor("#1A73E8"));
        tvSave.setTextSize(15);
        tvSave.setTypeface(null, android.graphics.Typeface.BOLD);
        tvSave.setPadding(24, 12, 24, 12);
        tvSave.setClickable(true);
        tvSave.setFocusable(true);
        tvSave.setOnClickListener(v -> {
            String nn = etNick.getText().toString().trim();
            String ss = etSig.getText().toString().trim();
            if (nn.length() > 16) {
                Toast.makeText(this, "名字不能超过16个字符", Toast.LENGTH_SHORT).show();
                return;
            }
            if (ss.length() > 30) {
                Toast.makeText(this, "签名不能超过30个字符", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!nn.isEmpty()) prefs.edit().putString("pref_nickname", nn).apply();
            if (!ss.isEmpty()) prefs.edit().putString("pref_signature", ss).apply();
            if (pendingAvatarBitmap != null) {
                saveAvatarBitmap(pendingAvatarBitmap);
            }
            prefs.edit()
                    .putInt("pref_avatar_image_tint", pendingImageTint)
                    .putInt("pref_avatar_frame_color", pendingFrameColor)
                    .apply();
            profileDialogSaved = true;
            setResult(RESULT_OK);
            Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
        buttonLayout.addView(tvSave);

        layout.addView(buttonLayout);

        dialog.setView(layout);
        dialog.setOnDismissListener(d -> {
            if (!profileDialogSaved && pendingAvatarBitmap != null && !pendingAvatarBitmap.isRecycled()) {
                pendingAvatarBitmap.recycle();
            }
            pendingAvatarBitmap = null;
            dialogAvatarView = null;
            dialogAvatarFrame = null;
        });
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setWindowAnimations(0);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_AVATAR && resultCode == RESULT_OK && data != null) {
            try {
                Uri uri = data.getData();
                if (uri == null) return;

                // 1. 获取原始尺寸
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                InputStream is = getContentResolver().openInputStream(uri);
                BitmapFactory.decodeStream(is, null, options);
                if (is != null) is.close();

                // 2. 计算采样率
                options.inSampleSize = calculateInSampleSize(options, 512, 512);
                options.inJustDecodeBounds = false;

                // 3. 真正解码
                is = getContentResolver().openInputStream(uri);
                Bitmap bmp = BitmapFactory.decodeStream(is, null, options);
                if (is != null) is.close();

                if (bmp == null) return;

                // 4. 1:1 正方形裁剪，取短边居中
                int w = bmp.getWidth();
                int h = bmp.getHeight();
                int size = Math.min(w, h);
                int x = (w - size) / 2;
                int y = (h - size) / 2;
                Bitmap cropped = Bitmap.createBitmap(bmp, x, y, size, size);

                // 5. 缩放到待保存头像；资料弹窗点击“保存”时才写入文件
                Bitmap scaled = Bitmap.createScaledBitmap(cropped, 256, 256, true);

                // 释放内存
                if (cropped != bmp) bmp.recycle();
                if (scaled != cropped) cropped.recycle();

                if (pendingAvatarBitmap != null && !pendingAvatarBitmap.isRecycled()) {
                    pendingAvatarBitmap.recycle();
                }
                pendingAvatarBitmap = scaled;
                if (dialogAvatarView != null) {
                    applyAvatarPreview(dialogAvatarView, dialogAvatarFrame, pendingAvatarBitmap,
                            pendingImageTint, pendingFrameColor);
                }
                Toast.makeText(this, "头像已加入预览，点击保存后生效", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "更换失败", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    private void saveAvatarBitmap(Bitmap bitmap) {
        try (FileOutputStream fos = new FileOutputStream(new File(getFilesDir(), "avatar.png"))) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, fos);
        } catch (Exception e) {
            Toast.makeText(this, "头像保存失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void applyAvatarPreview(ImageView imageView, View frameView, Bitmap bitmap,
                                    int imageTint, int frameColor) {
        android.graphics.drawable.GradientDrawable clip = new android.graphics.drawable.GradientDrawable();
        clip.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        clip.setColor(Color.TRANSPARENT);
        imageView.setBackground(clip);
        imageView.setClipToOutline(true);
        if (bitmap == null) {
            imageView.setImageResource(R.drawable.bg_circle_avatar);
            imageView.clearColorFilter();
        } else {
            imageView.setImageBitmap(bitmap);
            if (imageTint == Color.TRANSPARENT) {
                imageView.clearColorFilter();
            } else {
                imageView.setColorFilter(new PorterDuffColorFilter(imageTint, PorterDuff.Mode.SRC_ATOP));
            }
        }
        android.graphics.drawable.GradientDrawable frame = new android.graphics.drawable.GradientDrawable();
        frame.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        frame.setColor(Color.argb(42, Color.red(frameColor), Color.green(frameColor), Color.blue(frameColor)));
        frame.setStroke((int) (2 * getResources().getDisplayMetrics().density),
                Color.argb(220, Color.red(frameColor), Color.green(frameColor), Color.blue(frameColor)));
        frameView.setBackground(frame);
        frameView.setAlpha(0.9f);
    }

    public static void renderSavedAvatar(Context context, ImageView imageView, View frameView,
                                         TextView fallbackText) {
        SharedPreferences avatarPrefs = context.getSharedPreferences("study_plan_prefs", MODE_PRIVATE);
        int imageTint = avatarPrefs.getInt("pref_avatar_image_tint", Color.TRANSPARENT);
        int frameColor = avatarPrefs.getInt("pref_avatar_frame_color", Color.TRANSPARENT);
        Bitmap bitmap = loadAvatarBitmap(context);
        GradientDrawable clip = new GradientDrawable();
        clip.setShape(GradientDrawable.OVAL);
        clip.setColor(Color.TRANSPARENT);
        imageView.setBackground(clip);
        imageView.setClipToOutline(true);
        if (bitmap != null) {
            imageView.setVisibility(View.VISIBLE);
            imageView.setImageBitmap(bitmap);
            if (imageTint == Color.TRANSPARENT) {
                imageView.clearColorFilter();
            } else {
                imageView.setColorFilter(new PorterDuffColorFilter(imageTint, PorterDuff.Mode.SRC_ATOP));
            }
            frameView.setVisibility(View.VISIBLE);
            fallbackText.setVisibility(View.GONE);
        } else {
            imageView.setVisibility(View.GONE);
            imageView.clearColorFilter();
            frameView.setVisibility(View.VISIBLE);
            fallbackText.setVisibility(View.VISIBLE);
        }
        if (frameView instanceof AvatarFrameView) {
            AvatarFrameView animatedFrame = (AvatarFrameView) frameView;
            animatedFrame.setAccentColor(frameColor);
            animatedFrame.setFallback(bitmap == null);
        } else {
            GradientDrawable frame = new GradientDrawable();
            frame.setShape(GradientDrawable.OVAL);
            frame.setColor(Color.argb(42, Color.red(frameColor), Color.green(frameColor), Color.blue(frameColor)));
            frame.setStroke((int) (2 * context.getResources().getDisplayMetrics().density),
                    Color.argb(220, Color.red(frameColor), Color.green(frameColor), Color.blue(frameColor)));
            frameView.setBackground(frame);
            frameView.setAlpha(0.9f);
            frameView.animate().cancel();
            frameView.setScaleX(1f);
            frameView.setScaleY(1f);
            frameView.animate().alpha(0.72f).setDuration(900).withEndAction(() ->
                    frameView.animate().alpha(0.9f).setDuration(900).start()).start();
        }
    }


    public static Bitmap loadAvatarBitmap(android.content.Context ctx) {
        File file = new File(ctx.getFilesDir(), "avatar.png");
        if (file.exists()) {
            return BitmapFactory.decodeFile(file.getAbsolutePath());
        }
        return null;
    }

    private Bitmap loadAvatarBitmap() {
        return loadAvatarBitmap(this);
    }

    private void updateDbSize() {
        File dbFile = getDatabasePath("study_plan.db");
        long bytes = dbFile != null && dbFile.exists() ? dbFile.length() : 0;
        String s;
        if (bytes < 1024) s = bytes + " B";
        else if (bytes < 1024 * 1024) s = String.format("%.1f KB", bytes / 1024.0);
        else s = String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        tvDbSize.setText(s);
    }

    private void showCustomTimePickerDialog(int currentHour, int currentMinute) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_time_picker, null);

        NumberPicker npHour = dialogView.findViewById(R.id.np_hour);
        NumberPicker npMinute = dialogView.findViewById(R.id.np_minute);

        npHour.setMinValue(0);
        npHour.setMaxValue(23);
        npHour.setValue(currentHour);
        npHour.setFormatter(i -> String.format(Locale.US, "%02d", i));

        npMinute.setMinValue(0);
        npMinute.setMaxValue(59);
        npMinute.setValue(currentMinute);
        npMinute.setFormatter(i -> String.format(Locale.US, "%02d", i));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        dialogView.findViewById(R.id.btn_dialog_cancel)
                .setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.btn_dialog_save).setOnClickListener(v -> {
            String selectedTime = String.format(Locale.US, "%02d:%02d",
                    npHour.getValue(), npMinute.getValue());
            prefs.edit().putString("pref_reminder_time", selectedTime).apply();
            tvReminderTime.setText(selectedTime);
            dialog.dismiss();
        });

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setWindowAnimations(0);
        }
    }

    @Override
    public void finish() {
        super.finish();
    }

    private String pad(int n) {
        return n < 10 ? "0" + n : String.valueOf(n);
    }

    private void showSubjectRulesDialog() {
        TaskDatabaseHelper dbHelper = new TaskDatabaseHelper(this);

        AlertDialog dialog = new AlertDialog.Builder(this).create();

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 36, 48, 36);
        layout.setBackgroundResource(R.drawable.bg_dialog_card);

        // 标题
        TextView tvTitle = new TextView(this);
        tvTitle.setText("学科词库管理");
        tvTitle.setTextSize(18);
        tvTitle.setTextColor(Color.parseColor("#1F1F1F"));
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTitle.setPadding(0, 0, 0, 16);
        layout.addView(tvTitle);

        // 输入区域
        LinearLayout inputLayout = new LinearLayout(this);
        inputLayout.setOrientation(LinearLayout.HORIZONTAL);
        inputLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);

        EditText etNewSubject = new EditText(this);
        etNewSubject.setHint("输入新学科(如: 计算机网络)");
        etNewSubject.setHintTextColor(Color.parseColor("#805F6368"));
        etNewSubject.setTextColor(Color.parseColor("#1F1F1F"));
        etNewSubject.setBackgroundResource(R.drawable.bg_dialog_input);
        etNewSubject.setPadding(20, 12, 20, 12);
        LinearLayout.LayoutParams lpEdit = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        etNewSubject.setLayoutParams(lpEdit);
        inputLayout.addView(etNewSubject);

        android.widget.Button btnAdd = new android.widget.Button(this);
        btnAdd.setText("添加");
        btnAdd.setTextColor(Color.parseColor("#1F1F1F"));
        btnAdd.setBackgroundResource(R.drawable.bg_dialog_btn);
        LinearLayout.LayoutParams lpBtnAdd = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, (int) (38 * getResources().getDisplayMetrics().density));
        lpBtnAdd.leftMargin = 12;
        btnAdd.setLayoutParams(lpBtnAdd);
        inputLayout.addView(btnAdd);

        layout.addView(inputLayout);

        // 列表区域
        ScrollView scrollView = new ScrollView(this);
        int scrollHeight = (int) (280 * getResources().getDisplayMetrics().density);
        LinearLayout.LayoutParams lpScroll = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, scrollHeight);
        lpScroll.topMargin = 16;
        scrollView.setLayoutParams(lpScroll);

        LinearLayout listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(listContainer);
        layout.addView(scrollView);

        // 底部控制按钮
        LinearLayout buttonLayout = new LinearLayout(this);
        buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
        buttonLayout.setGravity(android.view.Gravity.END);
        LinearLayout.LayoutParams buttonLayoutLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        buttonLayoutLp.topMargin = 16;
        buttonLayout.setLayoutParams(buttonLayoutLp);

        android.util.TypedValue outValue = new android.util.TypedValue();
        getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
        int rippleRes = outValue.resourceId;

        // 刷新列表的方法
        Runnable refreshList = new Runnable() {
            @Override
            public void run() {
                listContainer.removeAllViews();
                List<String> subjects = dbHelper.getAllSubjects();
                for (String subject : subjects) {
                    LinearLayout row = new LinearLayout(SettingsActivity.this);
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    float rowDensity = getResources().getDisplayMetrics().density;
                    row.setPadding((int) (12 * rowDensity), (int) (14 * rowDensity),
                            (int) (12 * rowDensity), (int) (14 * rowDensity));
                    row.setGravity(android.view.Gravity.CENTER_VERTICAL);

                    TextView tvName = new TextView(SettingsActivity.this);
                    tvName.setText(subject);
                    tvName.setTextSize(15);
                    tvName.setTextColor(Color.parseColor("#1F1F1F"));
                    row.addView(tvName, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));

                    TextView tvDelete = new TextView(SettingsActivity.this);
                    tvDelete.setText("删除");
                    tvDelete.setTextColor(Color.parseColor("#E53935"));
                    tvDelete.setPadding(16, 8, 16, 8);
                    tvDelete.setClickable(true);
                    tvDelete.setFocusable(true);
                    tvDelete.setBackgroundResource(rippleRes);
                    tvDelete.setOnClickListener(v -> {
                        dbHelper.deleteSubject(subject);
                        run(); // 刷新
                        Toast.makeText(SettingsActivity.this, "已删除: " + subject, Toast.LENGTH_SHORT).show();
                    });
                    row.addView(tvDelete);

                    listContainer.addView(row);
                }
            }
        };

        refreshList.run();

        btnAdd.setOnClickListener(v -> {
            String name = etNewSubject.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(this, "请输入学科名称", Toast.LENGTH_SHORT).show();
                return;
            }
            if (name.length() > 10) {
                Toast.makeText(this, "学科名称不能超过10个字符", Toast.LENGTH_SHORT).show();
                return;
            }
            List<String> currentSubjects = dbHelper.getAllSubjects();
            if (currentSubjects.contains(name)) {
                Toast.makeText(this, "该学科已存在于词库中", Toast.LENGTH_SHORT).show();
                return;
            }
            dbHelper.insertSubject(name);
            etNewSubject.setText("");
            refreshList.run();
            Toast.makeText(this, "已添加: " + name, Toast.LENGTH_SHORT).show();
        });

        TextView tvClose = new TextView(this);
        tvClose.setText("关闭");
        tvClose.setTextColor(Color.parseColor("#1A73E8"));
        tvClose.setTextSize(15);
        tvClose.setTypeface(null, android.graphics.Typeface.BOLD);
        tvClose.setPadding(24, 12, 24, 12);
        tvClose.setClickable(true);
        tvClose.setFocusable(true);
        tvClose.setBackgroundResource(rippleRes);
        tvClose.setOnClickListener(v -> dialog.dismiss());
        buttonLayout.addView(tvClose);

        layout.addView(buttonLayout);

        dialog.setView(layout);
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setWindowAnimations(0);
        }
    }
}
