import Foundation

enum TextPreprocessor {
    static func clean(_ rawInput: String) -> String {
        var result = rawInput.trimmingCharacters(in: .whitespacesAndNewlines)
        [#"(?i)当然可以[，,].*?[：:]"#, #"(?i)以下是.*?学习计划[：:]"#, #"(?i)好的[，,].*?[：:]"#, #"(?i)收到[，,].*?[：:]"#, #"(?i)没问题[，,].*?[：:]"#].forEach { result = result.replacingOccurrences(of: $0, with: "", options: .regularExpression) }
        return result.replacingOccurrences(of: "，", with: ",").replacingOccurrences(of: "；", with: ";")
    }
}
