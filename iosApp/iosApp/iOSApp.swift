import SwiftUI
import shared
import ui

@main
struct MafiaApp: App {
    init() { KoinHelperKt.doInitKoin() }
    var body: some Scene {
        WindowGroup {
            ComposeViewControllerWrapper().ignoresSafeArea()
        }
    }
}

struct ComposeViewControllerWrapper: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController { MainViewControllerKt.MainViewController() }
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
