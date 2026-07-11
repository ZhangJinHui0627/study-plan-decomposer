package com.example.studyplan;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

public class TaskParserTest {

    private final String today = "2026-07-10"; // Monday
    private final List<String> subjects = new ArrayList<String>() {{
        add("高等数学");
        add("计算机网络");
        add("线性代数");
        add("英语");
        add("编译原理");
        add("大学物理");
    }};

    @Test
    public void testCase1_AbsoluteDate() {
        List<Task> result = TaskParser.parse("2026-07-12 高等数学 45分钟", today, subjects);
        assertEquals(1, result.size());
        Task t = result.get(0);
        assertEquals("2026-07-12", t.date);
        assertEquals("高等数学", t.subject);
        assertEquals(45, t.duration);
        assertEquals("08:00", t.specificTime);
    }

    @Test
    public void testCase2_RelativeToday() {
        List<Task> result = TaskParser.parse("今天 下午 2点半 计算机网络 50分钟", today, subjects);
        assertEquals(1, result.size());
        Task t = result.get(0);
        assertEquals("2026-07-10", t.date);
        assertEquals("下午", t.timeRange);
        assertEquals("14:30", t.specificTime);
        assertEquals("计算机网络", t.subject);
        assertEquals(50, t.duration);
    }

    @Test
    public void testCase3_RelativeTomorrow() {
        List<Task> result = TaskParser.parse("明天 早上 9:00 英语 30分钟", today, subjects);
        assertEquals(1, result.size());
        Task t = result.get(0);
        assertEquals("2026-07-11", t.date);
        assertEquals("早上", t.timeRange);
        assertEquals("09:00", t.specificTime);
        assertEquals("英语", t.subject);
    }

    @Test
    public void testCase4_RelativeAfterTomorrow() {
        List<Task> result = TaskParser.parse("后天 14:00 大学物理", today, subjects);
        assertEquals(1, result.size());
        Task t = result.get(0);
        assertEquals("2026-07-12", t.date);
        assertEquals("14:00", t.specificTime);
        assertEquals("大学物理", t.subject);
    }

    @Test
    public void testCase5_Weekday() {
        // 2026-07-10 is Friday. Next Wednesday should be 2026-07-15.
        List<Task> result = TaskParser.parse("周三 晚上 8点 英语", today, subjects);
        assertEquals(1, result.size());
        Task t = result.get(0);
        assertEquals("2026-07-15", t.date);
        assertEquals("晚上", t.timeRange);
        assertEquals("20:00", t.specificTime);
    }

    @Test
    public void testCase6_SpecificTimeColon() {
        List<Task> result = TaskParser.parse("今天 18:30 编译原理", today, subjects);
        assertEquals(1, result.size());
        Task t = result.get(0);
        assertEquals("18:30", t.specificTime);
        assertEquals("编译原理", t.subject);
    }

    @Test
    public void testCase7_SpecificTimeChineseMin() {
        List<Task> result = TaskParser.parse("今天 10点15分 线性代数", today, subjects);
        assertEquals(1, result.size());
        Task t = result.get(0);
        assertEquals("10:15", t.specificTime);
        assertEquals("线性代数", t.subject);
    }

    @Test
    public void testCase8_SpecificTimeHourOnly() {
        List<Task> result = TaskParser.parse("今天 8点 英语", today, subjects);
        assertEquals(1, result.size());
        Task t = result.get(0);
        assertEquals("08:00", t.specificTime);
    }

    @Test
    public void testCase9_TimeRangeFuzzy() {
        List<Task> result = TaskParser.parse("今天 上午 学习 40分钟", today, subjects);
        assertEquals(1, result.size());
        Task t = result.get(0);
        assertEquals("上午", t.timeRange);
        assertEquals("09:00", t.specificTime); // default mapping for 上午
        assertEquals("09:00", t.specificTime);
    }

    @Test
    public void testCase10_MultipleTasks() {
        List<Task> result = TaskParser.parse("今天 高等数学 60分钟，明天 线性代数 50分钟", today, subjects);
        assertEquals(2, result.size());
        assertEquals("2026-07-10", result.get(0).date);
        assertEquals("高等数学", result.get(0).subject);
        assertEquals("2026-07-11", result.get(1).date);
        assertEquals("线性代数", result.get(1).subject);
    }

    @Test
    public void testCase11_Pages() {
        List<Task> result = TaskParser.parse("今天 学习大学物理 10页", today, subjects);
        assertEquals(1, result.size());
        Task t = result.get(0);
        assertEquals(10, t.pages);
        assertEquals("大学物理", t.subject);
    }

    @Test
    public void testCase12_Chapters() {
        List<Task> result = TaskParser.parse("明天 高等数学 第5章", today, subjects);
        assertEquals(1, result.size());
        Task t = result.get(0);
        assertEquals(5, t.pages); // Chapter maps to pages in TaskParser
        assertEquals("高等数学", t.subject);
    }

    @Test
    public void testCase13_MinSuffix() {
        List<Task> result = TaskParser.parse("2026-08-01 英语 45min", today, subjects);
        assertEquals(1, result.size());
        Task t = result.get(0);
        assertEquals(45, t.duration);
    }

    @Test
    public void testCase14_HighPriorityImportant() {
        List<Task> result = TaskParser.parse("今天 15点 计算机网络 重要 60分钟", today, subjects);
        assertEquals(1, result.size());
        Task t = result.get(0);
        assertEquals(2, t.priority);
    }

    @Test
    public void testCase15_MidPriorityReview() {
        List<Task> result = TaskParser.parse("明天 晚上 英语 复习", today, subjects);
        assertEquals(1, result.size());
        Task t = result.get(0);
        assertEquals(1, t.priority);
    }

    @Test
    public void testCase16_FallbackContent() {
        List<Task> result = TaskParser.parse("做点有意思的", today, subjects);
        assertEquals(1, result.size());
        Task t = result.get(0);
        assertEquals("有意思的", t.content);
        assertEquals("", t.subject);
    }

    @Test
    public void testCase17_SubjectInList() {
        List<Task> result = TaskParser.parse("今天 8点 编译原理 课后题", today, subjects);
        assertEquals(1, result.size());
        Task t = result.get(0);
        assertEquals("编译原理", t.subject);
        assertEquals("课后题", t.content);
    }

    @Test
    public void testCase18_VagueTimeRangeDefaultMapping() {
        List<Task> result = TaskParser.parse("今天 傍晚 散步", today, subjects);
        assertEquals(1, result.size());
        Task t = result.get(0);
        assertEquals("傍晚", t.timeRange);
        assertEquals("17:00", t.specificTime); // 傍晚 maps to 17:00
    }

    @Test
    public void testCase19_MultipleDelimiters() {
        List<Task> result = TaskParser.parse("今天 高等数学 20分钟、明天 英语 30min和后天 大学物理", today, subjects);
        assertEquals(3, result.size());
        assertEquals("2026-07-10", result.get(0).date);
        assertEquals("2026-07-11", result.get(1).date);
        assertEquals("2026-07-12", result.get(2).date);
    }

    @Test
    public void testCase20_EmptyInput() {
        List<Task> result = TaskParser.parse("   ", today, subjects);
        assertEquals(0, result.size());
    }

    @Test
    public void vagueTimeTaskGetsDefaultFocusDuration() {
        Task task = TaskParser.parse("早上去图书馆", today, subjects).get(0);

        assertEquals("早上", task.timeRange);
        assertEquals("08:00", task.specificTime);
        assertEquals(0, task.duration);
    }
}
