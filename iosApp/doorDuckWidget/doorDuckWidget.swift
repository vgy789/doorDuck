import SwiftUI
import UIKit
import WidgetKit

private let appGroupId = "group.io.github.vgy789.doorDuck"
private let qrImageKey = "door_duck.qr_image_base64"
private let lastSuccessAtKey = "door_duck.last_success_at"
private let expiresAtKey = "door_duck.expires_at"
private let lastErrorKey = "door_duck.last_sync_error"
private let tokenKey = "door_duck.token"
private let userIdKey = "door_duck.user_id"

struct DoorDuckEntry: TimelineEntry {
    let date: Date
    let qrImage: UIImage?
}

struct DoorDuckProvider: TimelineProvider {
    func placeholder(in context: Context) -> DoorDuckEntry {
        DoorDuckEntry(
            date: Date(),
            qrImage: nil
        )
    }

    func getSnapshot(in context: Context, completion: @escaping (DoorDuckEntry) -> Void) {
        completion(loadEntry())
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<DoorDuckEntry>) -> Void) {
        let entry = loadEntry()
        let now = Date()
        let nextRefresh = Calendar.current.date(byAdding: .minute, value: 5, to: now) ?? now.addingTimeInterval(300)
        completion(Timeline(entries: [entry], policy: .after(nextRefresh)))
    }

    private func loadEntry() -> DoorDuckEntry {
        let defaults = UserDefaults(suiteName: appGroupId) ?? .standard
        let token = defaults.string(forKey: tokenKey) ?? ""
        let userId = defaults.string(forKey: userIdKey) ?? ""
        let qrImage = decodeImage(defaults.string(forKey: qrImageKey))
        let lastSuccessAt = defaults.string(forKey: lastSuccessAtKey).flatMap(TimeInterval.init).map(Date.init(timeIntervalSince1970:))
        let expiresAt = defaults.string(forKey: expiresAtKey).flatMap(TimeInterval.init).map(Date.init(timeIntervalSince1970:))
        let lastError = defaults.string(forKey: lastErrorKey)

        let visibleQrImage: UIImage?
        if token.isEmpty || userId.isEmpty || lastError != nil || (expiresAt == nil && lastSuccessAt == nil) {
            visibleQrImage = nil
        } else {
            visibleQrImage = qrImage
        }

        return DoorDuckEntry(
            date: Date(),
            qrImage: visibleQrImage
        )
    }

    private func decodeImage(_ base64: String?) -> UIImage? {
        guard let base64, let data = Data(base64Encoded: base64) else { return nil }
        return UIImage(data: data)
    }
}

struct DoorDuckWidgetEntryView: View {
    var entry: DoorDuckProvider.Entry

    var body: some View {
        ZStack {
            LinearGradient(
                colors: [Color(red: 0.91, green: 0.95, blue: 1.0), .white],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            if let qrImage = entry.qrImage {
                Image(uiImage: qrImage)
                    .resizable()
                    .interpolation(.none)
                    .scaledToFit()
                    .padding(12)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        }
    }
}

struct DoorDuckWidget: Widget {
    let kind = "DoorDuckWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: DoorDuckProvider()) { entry in
            DoorDuckWidgetEntryView(entry: entry)
        }
        .configurationDisplayName("doorDuck QR")
        .description("Shows the latest QR code from doorDuck.")
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}

@main
struct DoorDuckWidgetBundle: WidgetBundle {
    var body: some Widget {
        DoorDuckWidget()
    }
}
