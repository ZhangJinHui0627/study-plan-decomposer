import SwiftUI

struct SubjectRulesView: View {
    @EnvironmentObject private var store: StudyPlanStore
    @State private var newSubject = ""
    @State private var showAlert = false
    @State private var alertMessage = ""

    var body: some View {
        List {
            Section("已保存学科") {
                ForEach(store.subjects, id: \.self) { subject in
                    HStack {
                        Text(subject)
                            .foregroundStyle(StudyPlanTheme.textPrimary)
                        Spacer()
                        Button {
                            store.deleteSubject(subject)
                        } label: {
                            Image(systemName: "trash")
                                .foregroundStyle(.red)
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
            Section("添加学科") {
                HStack {
                    TextField("学科名称 (如: 计算机网络)", text: $newSubject)
                        .foregroundStyle(StudyPlanTheme.textPrimary)
                    
                    Button("添加") {
                        addSubject()
                    }
                    .font(.system(size: 14, weight: .bold))
                    .foregroundStyle(StudyPlanTheme.primary)
                }
            }
        }
        .scrollContentBackground(.hidden)
        .background(StudyPlanTheme.background)
        .navigationTitle("学科词库")
        .navigationBarTitleDisplayMode(.inline)
        .alert("提示", isPresented: $showAlert) {
            Button("确定", role: .cancel) {}
        } message: {
            Text(alertMessage)
        }
    }

    private func addSubject() {
        let name = newSubject.trimmingCharacters(in: .whitespacesAndNewlines)
        if name.isEmpty {
            alertMessage = "请输入学科名称"
            showAlert = true
            return
        }
        if name.count > 10 {
            alertMessage = "学科名称不能超过10个字符"
            showAlert = true
            return
        }
        if store.subjects.contains(name) {
            alertMessage = "该学科已存在于词库中"
            showAlert = true
            return
        }
        store.addSubject(name)
        newSubject = ""
    }
}
