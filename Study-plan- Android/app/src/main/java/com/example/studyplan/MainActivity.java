package com.example.studyplan;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CALENDAR = 101;

    private HomeFragment homeFragment;
    private StatsFragment statsFragment;
    private TimerFragment timerFragment;
    private ProfileFragment profileFragment;
    private ScreenGlowFrameView screenGlowFrame;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences initialPrefs = getSharedPreferences("study_plan_prefs", MODE_PRIVATE);
        if (!initialPrefs.contains("pref_manual_complete_enabled")) {
            initialPrefs.edit().putBoolean("pref_manual_complete_enabled", true).apply();
        }
        VersionConfig.init(this);
        setContentView(R.layout.activity_main);
        screenGlowFrame = findViewById(R.id.screen_glow_frame);

        // 动态申请日历读写权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_CALENDAR, Manifest.permission.READ_CALENDAR}, REQUEST_CALENDAR);
        }

        View mainLayout = findViewById(R.id.main_layout);
        ViewCompat.setOnApplyWindowInsetsListener(mainLayout, (v, insets) -> {
            int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            int navBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;

            MaterialToolbar toolbar = findViewById(R.id.toolbar);
            toolbar.setPadding(0, statusBarHeight, 0, 0);

            BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
            bottomNav.setPadding(0, 0, 0, navBarHeight);

            return insets;
        });

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        ImageButton profileLogout = findViewById(R.id.btn_profile_logout);
        if (profileLogout != null) {
            profileLogout.setOnClickListener(v ->
                    android.widget.Toast.makeText(this, "当前为本地模式，无需退出", android.widget.Toast.LENGTH_SHORT).show());
        }
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_add) {
                Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                if (current instanceof HomeFragment) {
                    ((HomeFragment) current).showAddDialog();
                } else if (homeFragment != null) {
                    homeFragment.showAddDialog();
                }
                return true;
            }
            if (item.getItemId() == R.id.action_search) {
                Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                if (current instanceof HomeFragment) {
                    ((HomeFragment) current).showSearchDialog();
                } else if (homeFragment != null) {
                    homeFragment.showSearchDialog();
                }
                return true;
            }
            return false;
        });

        if (savedInstanceState == null) {
            homeFragment = new HomeFragment();
            statsFragment = new StatsFragment();
            timerFragment = new TimerFragment();
            profileFragment = new ProfileFragment();

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, homeFragment, "home")
                    .add(R.id.fragment_container, statsFragment, "stats")
                    .add(R.id.fragment_container, timerFragment, "timer")
                    .add(R.id.fragment_container, profileFragment, "profile")
                    .hide(statsFragment)
                    .hide(timerFragment)
                    .hide(profileFragment)
                    .commit();
        } else {
            homeFragment = (HomeFragment) getSupportFragmentManager().findFragmentByTag("home");
            statsFragment = (StatsFragment) getSupportFragmentManager().findFragmentByTag("stats");
            timerFragment = (TimerFragment) getSupportFragmentManager().findFragmentByTag("timer");
            profileFragment = (ProfileFragment) getSupportFragmentManager().findFragmentByTag("profile");
        }

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment active;
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                active = homeFragment;
            } else if (id == R.id.nav_stats) {
                active = statsFragment;
                statsFragment.loadStats();
            } else if (id == R.id.nav_timer) {
                active = timerFragment;
            } else if (id == R.id.nav_profile) {
                active = profileFragment;
            } else {
                return false;
            }

            Fragment[] all = {homeFragment, statsFragment, timerFragment, profileFragment};
            androidx.fragment.app.FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
            for (Fragment f : all) {
                if (f == active) tx.show(f); else tx.hide(f);
            }
            updateMenuVisibility(active);
            tx.commit();
            return true;
        });

        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.nav_home);
        }
        updateMenuVisibility(homeFragment);
        updateScreenGlow();

        // Android 13+ 动态申请通知权限
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 102);
            }
        }
    }

    public void refreshStats() {
        if (statsFragment != null) {
            statsFragment.loadStats();
        }
    }

    public void refreshHomeTasks() {
        if (homeFragment != null) {
            homeFragment.refreshList();
        }
        updateScreenGlow();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateScreenGlow();
    }

    private void updateScreenGlow() {
        if (screenGlowFrame == null) return;
        boolean activeTaskRunning = timerFragment != null && timerFragment.isTimerRunning();
        screenGlowFrame.setVisibility(activeTaskRunning ? View.VISIBLE : View.GONE);
    }

    public void refreshScreenGlow() {
        updateScreenGlow();
    }

    private void updateMenuVisibility(Fragment active) {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            if (active == homeFragment) {
                toolbar.setTitle("学习计划");
            } else if (active == statsFragment) {
                toolbar.setTitle("学习统计");
            } else if (active == timerFragment) {
                toolbar.setTitle("专注计时");
            } else if (active == profileFragment) {
                toolbar.setTitle("个人中心");
            }
            android.view.Menu menu = toolbar.getMenu();
            if (menu != null) {
                android.view.MenuItem itemAdd = menu.findItem(R.id.action_add);
                android.view.MenuItem itemSearch = menu.findItem(R.id.action_search);
                boolean show = (active == homeFragment);
                if (itemAdd != null) itemAdd.setVisible(show);
                if (itemSearch != null) itemSearch.setVisible(show);
            }
            ImageButton profileLogout = findViewById(R.id.btn_profile_logout);
            if (profileLogout != null) {
                profileLogout.setVisibility(active == profileFragment ? View.VISIBLE : View.GONE);
            }
        }
    }

    public void startTimingForTask(Task task, boolean isCountdown, boolean allowOverflow) {
        TimerFragment timerFragment = (TimerFragment) getSupportFragmentManager().findFragmentByTag("timer");
        if (timerFragment != null) {
            timerFragment.startTimingFromHome(task, isCountdown, allowOverflow);
        }
        refreshHomeTasks(); // 刷新列表计时指示器
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_timer);
        }
        updateScreenGlow();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CALENDAR) {
            if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                new Thread(() -> {
                    TaskDatabaseHelper db = new TaskDatabaseHelper(MainActivity.this);
                    java.util.List<Task> tasks = db.getAllTasks();
                    for (Task t : tasks) {
                        if (t.status == 0) {
                            CalendarHelper.addToCalendar(MainActivity.this, t);
                        }
                    }
                }).start();
            }
        }
    }

    public void onTaskCompletedExternally(long taskId) {
        TimerFragment timer = (TimerFragment) getSupportFragmentManager().findFragmentByTag("timer");
        if (timer != null && timer.getActiveTask() != null && timer.getActiveTask().id == taskId) {
            timer.stopTimer();
            timer.resetTimerExternally(); // 重置计时器
            updateScreenGlow();
        }
    }
}
