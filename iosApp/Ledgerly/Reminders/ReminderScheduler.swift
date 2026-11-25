import Foundation
import UserNotifications

/// The daily "nothing logged today" nudge. The shared app state works out the next week of times
/// (skipping today once something is logged); this replaces Ledgerly's pending notifications with
/// one per time whenever they change.
enum ReminderScheduler {
    private static let idPrefix = "app.ledgerly.reminder."

    /// Asks once; afterwards returns what the user chose. False means reminders stay off.
    static func requestPermission() async -> Bool {
        let center = UNUserNotificationCenter.current()
        let settings = await center.notificationSettings()
        switch settings.authorizationStatus {
        case .authorized, .provisional, .ephemeral:
            return true
        case .denied:
            return false
        default:
            return (try? await center.requestAuthorization(options: [.alert, .sound])) ?? false
        }
    }

    /// Replaces every scheduled reminder with `times` (epoch milliseconds). An empty list cancels them all.
    static func schedule(times: [Int64], message: String) async {
        let center = UNUserNotificationCenter.current()
        let pending = await center.pendingNotificationRequests()
        center.removePendingNotificationRequests(withIdentifiers: pending.map(\.identifier).filter { $0.hasPrefix(idPrefix) })
        // The app is open, so a nudge still in Notification Centre has done its job.
        center.removeAllDeliveredNotifications()

        let now = Date()
        for (index, millis) in times.enumerated() {
            let date = Date(timeIntervalSince1970: TimeInterval(millis) / 1000)
            guard date > now else { continue }
            let content = UNMutableNotificationContent()
            content.title = "Ledgerly"
            content.body = message
            content.sound = .default
            let when = Calendar.current.dateComponents([.year, .month, .day, .hour, .minute], from: date)
            let request = UNNotificationRequest(
                identifier: "\(idPrefix)\(index)",
                content: content,
                trigger: UNCalendarNotificationTrigger(dateMatching: when, repeats: false)
            )
            try? await center.add(request)
        }
    }
}
