//
//  Study_planApp.swift
//  Study-plan
//
//  Created by 吴思彤 on 2026/7/11.
//

import SwiftUI

@main
struct Study_planApp: App {
    @StateObject private var store = StudyPlanStore()
    @Environment(\.scenePhase) private var scenePhase

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(store)
        }
        .onChange(of: scenePhase) { _, phase in
            if phase == .active { store.refreshTimer() }
        }
    }
}
