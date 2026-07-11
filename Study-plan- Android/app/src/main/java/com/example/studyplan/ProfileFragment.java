package com.example.studyplan;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

public class ProfileFragment extends Fragment {

    private TextView tvNickname, tvSignature;
    private ImageView ivAvatar;
    private View vAvatarBg;
    private TextView tvAvatarText;
    private SwitchCompat swManualComplete;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        tvNickname = view.findViewById(R.id.tv_nickname);
        tvSignature = view.findViewById(R.id.tv_signature);
        ivAvatar = view.findViewById(R.id.iv_avatar);
        vAvatarBg = view.findViewById(R.id.v_avatar_bg);
        tvAvatarText = view.findViewById(R.id.tv_avatar_text);

        swManualComplete = view.findViewById(R.id.sw_manual_complete);
        SharedPreferences prefs = getContext().getSharedPreferences("study_plan_prefs", 0);
        swManualComplete.setChecked(prefs.getBoolean("pref_manual_complete_enabled", true));
        swManualComplete.setOnCheckedChangeListener((buttonView, isChecked) ->
            prefs.edit().putBoolean("pref_manual_complete_enabled", isChecked).apply()
        );

        view.findViewById(R.id.item_manual_complete).setOnClickListener(v -> swManualComplete.toggle());

        refreshProfile();

        // 头像点击 → 直接弹出个人资料编辑弹窗
        view.findViewById(R.id.fl_avatar).setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), SettingsActivity.class);
            intent.putExtra("show_personal", true);
            startActivityForResult(intent, 1001);
        });

        view.findViewById(R.id.item_about).setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AboutActivity.class);
            startActivity(intent);
        });

        view.findViewById(R.id.item_settings).setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), SettingsActivity.class);
            startActivityForResult(intent, 1001);
        });

        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && resultCode == android.app.Activity.RESULT_OK) {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).refreshHomeTasks();
                ((MainActivity) getActivity()).refreshStats();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshProfile();
    }

    private void refreshProfile() {
        if (getContext() == null) return;
        SharedPreferences prefs = getContext().getSharedPreferences("study_plan_prefs", 0);
        tvNickname.setText(prefs.getString("pref_nickname", "我的学习"));
        tvSignature.setText(prefs.getString("pref_signature", "更高效的学习方式"));

        SettingsActivity.renderSavedAvatar(getContext(), ivAvatar, vAvatarBg, tvAvatarText);
    }

}
