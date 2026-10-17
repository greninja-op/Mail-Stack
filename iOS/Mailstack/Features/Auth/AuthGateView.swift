import SwiftUI

struct AuthGateView: View {
    @EnvironmentObject private var container: AppContainer
    @State private var signedIn = false
    @State private var error: String?

    var body: some View {
        Group {
            if signedIn {
                AppShellView()
            } else {
                VStack(spacing: 20) {
                    Text("Mailstack")
                        .font(.system(size: MSTypography.displayMedium, weight: .bold))
                    Text("Organize Gmail on your iPhone.")
                        .foregroundStyle(.secondary)
                    if let error {
                        Text(error).foregroundStyle(.red)
                    }
                    Button("Continue with demo account") {
                        Task {
                            let result = await container.auth.beginSignIn()
                            switch result {
                            case .success: signedIn = true
                            case .failure(let e): error = e.localizedDescription
                            }
                        }
                    }
                    .buttonStyle(.borderedProminent)
                    Text("Live Google OAuth requires Mac build + client IDs.")
                        .font(.system(size: MSTypography.labelSmall))
                        .foregroundStyle(.secondary)
                        .multilineTextAlignment(.center)
                        .padding()
                }
                .padding()
            }
        }
        .task {
            for await v in container.auth.isSignedInPublisher.values {
                signedIn = v
            }
        }
    }
}
