import SwiftUI
import UIKit
import WidgetKit

private let appGroupId = "group.io.github.vgy789.doorDuck"
private let qrImageKey = "door_duck.qr_image_base64"
private let lastSuccessAtKey = "door_duck.last_success_at"
private let expiresAtKey = "door_duck.expires_at"
private let userIdKey = "door_duck.user_id"
private let qrSnapshotV2Key = "door_duck.qr_snapshot_v2"

private struct PersistedQrSnapshot: Decodable {
    let qrImageBase64: String
    let lastSuccessAtMs: Int64?
    let expiresAtMs: Int64?
    let imageValidationStatus: String
}

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
        let userId = defaults.string(forKey: userIdKey) ?? ""
        let snapshot = loadSnapshot(defaults: defaults)
        let qrImage = decodeImage(snapshot?.qrImageBase64 ?? defaults.string(forKey: qrImageKey))
        let lastSuccessAt = snapshot?.lastSuccessAtMs.map { Date(timeIntervalSince1970: TimeInterval($0) / 1000.0) }
            ?? defaults.string(forKey: lastSuccessAtKey).flatMap(TimeInterval.init).map(Date.init(timeIntervalSince1970:))
        let expiresAt = snapshot?.expiresAtMs.map { Date(timeIntervalSince1970: TimeInterval($0) / 1000.0) }
            ?? defaults.string(forKey: expiresAtKey).flatMap(TimeInterval.init).map(Date.init(timeIntervalSince1970:))
        let validationStatus = snapshot?.imageValidationStatus ?? (qrImage == nil ? "INVALID" : "VALID")

        let visibleQrImage: UIImage?
        if userId.isEmpty || validationStatus != "VALID" || isExpired(expiresAt) || (expiresAt == nil && lastSuccessAt == nil) {
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

    private func loadSnapshot(defaults: UserDefaults) -> PersistedQrSnapshot? {
        guard let raw = defaults.string(forKey: qrSnapshotV2Key),
              let data = raw.data(using: .utf8) else {
            return nil
        }
        return try? JSONDecoder().decode(PersistedQrSnapshot.self, from: data)
    }

    private func isExpired(_ date: Date?) -> Bool {
        guard let date else { return false }
        return date <= Date()
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
