import SwiftUI
import Shared

@main
struct iOSApp: App {
    
    // 1. Add this init block
        init() {
            // 2. Call the top-level Kotlin function you created in MainViewController.kt
            MainViewControllerKt.doInitKoin()
        }
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
