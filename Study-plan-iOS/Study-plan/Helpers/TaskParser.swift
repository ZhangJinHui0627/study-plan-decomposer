import Foundation

enum TaskParser {
    static func parse(_ text: String, today: Date = .now, subjects: [String]) -> [Task] {
        let cleaned = TextPreprocessor.clean(text)
        guard !cleaned.isEmpty else { return [] }
        let todayText = Self.dateFormatter.string(from: today)
        return cleaned.components(separatedBy: CharacterSet(charactersIn: "，,;；\n、和以及"))
            .compactMap { parseSegment($0, today: today, todayText: todayText, subjects: subjects) }
    }

    static func parse(_ text: String, subject: String = "") -> Task {
        parse(text, subjects: subject.isEmpty ? [] : [subject]).first ?? Task(content: text)
    }

    private static func parseSegment(_ raw: String, today: Date, todayText: String, subjects: [String]) -> Task? {
        var segment = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !segment.isEmpty else { return nil }
        var dateText: String?
        var timeRange: String?
        var specificTime: String?
        var subject = ""
        var duration = 0
        var pages = 0
        var priority = 0

        if let value = firstMatch(#"\d{4}-\d{2}-\d{2}"#, in: segment) { dateText = value; segment = segment.replacingOccurrences(of: value, with: "") }
        if dateText == nil, let value = firstMatch("今天|明天|后天", in: segment) {
            dateText = value == "今天" ? todayText : Self.dateFormatter.string(from: Calendar.current.date(byAdding: .day, value: value == "明天" ? 1 : 2, to: today) ?? today)
            segment = segment.replacingOccurrences(of: value, with: "")
        }
        if dateText == nil, let value = firstMatch("周[一二三四五六日]|星期[一二三四五六日]", in: segment) {
            dateText = Self.dateFormatter.string(from: resolveWeekday(today, value))
            segment = segment.replacingOccurrences(of: value, with: "")
        }
        if let value = firstMatch("早上|上午|中午|下午|傍晚|晚上|深夜", in: segment) { timeRange = value; segment = segment.replacingOccurrences(of: value, with: "") }
        if let value = parseTime(in: segment, timeRange: timeRange) { specificTime = value.text; segment = segment.replacingOccurrences(of: value.source, with: "") }
        if let value = firstMatch(#"\d+\s*(?:分钟|min|mins)"#, in: segment), let number = Int(value.filter(\.isNumber)) { duration = number; segment = segment.replacingOccurrences(of: value, with: "") }
        if let value = firstMatch(#"第\s*\d+\s*章"#, in: segment), let number = Int(value.filter(\.isNumber)) { pages = number; segment = segment.replacingOccurrences(of: value, with: "") }
        else if let value = firstMatch(#"\d+\s*(?:-|到|~)\s*\d+\s*(?:页|pages|p)"#, in: segment) {
            let numbers = value.split { !$0.isNumber }.compactMap { Int($0) }
            if numbers.count >= 2 { pages = max(0, numbers[1] - numbers[0] + 1) }
            segment = segment.replacingOccurrences(of: value, with: "")
        } else if let value = firstMatch(#"\d+\s*页"#, in: segment), let number = Int(value.filter(\.isNumber)) { pages = number; segment = segment.replacingOccurrences(of: value, with: "") }
        for value in subjects where segment.contains(value) { subject = value; segment = segment.replacingOccurrences(of: value, with: ""); break }
        if segment.contains("优先") || segment.contains("重要") || segment.contains("紧急") || segment.contains("必须") { priority = 2; segment = segment.replacingOccurrences(of: "优先", with: "").replacingOccurrences(of: "重要", with: "").replacingOccurrences(of: "紧急", with: "").replacingOccurrences(of: "必须", with: "") }
        else if segment.contains("复习") || segment.contains("完成") { priority = 1 }
        segment = segment.replacingOccurrences(of: #"^(学习|完成|做点|写点|看点|学点|背点|做|写|阅读|复习|背诵|看|打卡)"#, with: "", options: .regularExpression).trimmingCharacters(in: .whitespacesAndNewlines)
        if segment.isEmpty { segment = subject.isEmpty ? "学习任务" : subject }
        if dateText == nil { dateText = todayText }
        if specificTime == nil { specificTime = Self.defaultTime[timeRange ?? ""] ?? "08:00" }
        var task = Task(date: Self.dateFormatter.date(from: dateText ?? todayText) ?? today, timeRange: timeRange ?? "", subject: subject, content: segment, duration: duration, pages: pages, priority: priority)
        task.specificTime = specificTime ?? "08:00"
        return task
    }

    private static func firstMatch(_ pattern: String, in text: String) -> String? {
        guard let regex = try? NSRegularExpression(pattern: pattern, options: [.caseInsensitive]), let match = regex.firstMatch(in: text, range: NSRange(text.startIndex..., in: text)), let range = Range(match.range, in: text) else { return nil }
        return String(text[range])
    }

    private static func parseTime(in text: String, timeRange: String?) -> (source: String, text: String)? {
        guard let source = firstMatch(#"\d{1,2}:\d{2}|\d{1,2}点\d{2}分?|\d{1,2}点半|\d{1,2}点"#, in: text) else { return nil }
        let numbers = source.split { !$0.isNumber }.compactMap { Int($0) }
        guard let rawHour = numbers.first, rawHour < 24 else { return nil }
        let minute = source.contains("半") ? 30 : (numbers.count > 1 ? numbers[1] : 0)
        guard minute < 60 else { return nil }
        var hour = rawHour
        if hour < 12, ["下午", "傍晚", "晚上", "深夜"].contains(timeRange) { hour += 12 }
        return (source, String(format: "%02d:%02d", hour, minute))
    }

    private static func resolveWeekday(_ today: Date, _ value: String) -> Date {
        let digits = ["一": 2, "二": 3, "三": 4, "四": 5, "五": 6, "六": 7, "日": 1]
        let key = String(value.last ?? "一")
        let target = digits[key] ?? 1
        let current = Calendar.current.component(.weekday, from: today)
        return Calendar.current.date(byAdding: .day, value: (target - current + 7) % 7, to: today) ?? today
    }

    private static let dateFormatter: DateFormatter = { let value = DateFormatter(); value.dateFormat = "yyyy-MM-dd"; value.locale = Locale(identifier: "en_US_POSIX"); return value }()
    private static let defaultTime = ["早上": "08:00", "上午": "09:00", "中午": "12:00", "下午": "14:00", "傍晚": "17:00", "晚上": "19:00", "深夜": "22:00"]
}
