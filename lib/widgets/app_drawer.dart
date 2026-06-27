import 'package:flutter/material.dart';
import 'package:package_info_plus/package_info_plus.dart';

import '../core/l10n/app_strings.dart';
import '../features/backup/backup_screen.dart';
import '../features/calendar/calendar_screen.dart';
import '../features/help/help_guide_screen.dart';
import '../features/meds/medication_screen.dart';
import '../features/reminders/notification_center_screen.dart';
import '../features/reminders/reliability_test_screen.dart';
import '../features/reminders/reminder_defaults_screen.dart';
import '../features/security/security_settings_screen.dart';
import '../features/settings/settings_screen.dart';
import '../features/sounds/sound_library_screen.dart';
import '../features/sync/cloud_sync_screen.dart';

/// القائمة الجانبية الرئيسية لتطبيق التنبيهات (تنبيهات/أدوية/تقويم + الخدمات).
class AppDrawer extends StatelessWidget {
  const AppDrawer({super.key});

  @override
  Widget build(BuildContext context) {
    final s = S.of(context);
    final scheme = Theme.of(context).colorScheme;

    void go(Widget page) {
      Navigator.pop(context); // أغلق القائمة
      Navigator.push(context, MaterialPageRoute(builder: (_) => page));
    }

    return Drawer(
      child: SafeArea(
        child: ListView(
          padding: EdgeInsets.zero,
          children: [
            DrawerHeader(
              decoration: BoxDecoration(color: scheme.primary),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisAlignment: MainAxisAlignment.end,
                children: [
                  // الشعار + زرّ إغلاق واضح للقائمة الجانبية (نفس الصفّ).
                  Row(
                    children: [
                      Icon(Icons.notifications_active,
                          color: scheme.onPrimary, size: 40),
                      const Spacer(),
                      IconButton(
                        tooltip: 'رجوع',
                        visualDensity: VisualDensity.compact,
                        icon: Icon(Icons.arrow_back, color: scheme.onPrimary),
                        onPressed: () => Navigator.pop(context),
                      ),
                    ],
                  ),
                  const SizedBox(height: 8),
                  Text(
                    s.t('app_name'),
                    style: TextStyle(
                      color: scheme.onPrimary,
                      fontSize: 22,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  Text(
                    s.t('about_desc'),
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                    style: TextStyle(
                        color: scheme.onPrimary.withOpacity(0.8), fontSize: 11),
                  ),
                  FutureBuilder<PackageInfo>(
                    future: PackageInfo.fromPlatform(),
                    builder: (context, snap) {
                      final info = snap.data;
                      if (info == null) return const SizedBox.shrink();
                      return Padding(
                        padding: const EdgeInsets.only(top: 2),
                        child: Text(
                          'v${info.version} • #${info.buildNumber}',
                          style: TextStyle(
                              color: scheme.onPrimary.withOpacity(0.7),
                              fontSize: 11),
                        ),
                      );
                    },
                  ),
                ],
              ),
            ),
            // دليل الاستخدام بارز أعلى القائمة.
            ListTile(
              leading: Icon(Icons.auto_stories, color: scheme.primary),
              title: Text(s.t('user_guide'),
                  style: TextStyle(
                      fontWeight: FontWeight.bold, color: scheme.primary)),
              onTap: () => go(const HelpGuideScreen()),
            ),
            const Divider(height: 1),
            // 1) التنبيهات والمنبّهات.
            _group(context, Icons.alarm, s.t('reminders'),
                initiallyExpanded: true, children: [
              _tile(context, Icons.medication_outlined, s.t('med_mode'),
                  () => go(const MedicationScreen())),
              _tile(context, Icons.calendar_month_outlined, s.t('calendar'),
                  () => go(const CalendarScreen())),
              _tile(context, Icons.notifications_active_outlined,
                  s.t('notif_center'),
                  () => go(const NotificationCenterScreen())),
              _tile(context, Icons.library_music_outlined, s.t('sound_library'),
                  () => go(const SoundLibraryScreen())),
              _tile(context, Icons.tune, s.t('reminder_defaults'),
                  () => go(const ReminderDefaultsScreen())),
              _tile(context, Icons.health_and_safety_outlined,
                  s.t('reliability_test'),
                  () => go(const ReliabilityTestScreen())),
            ]),
            // 2) الأمان.
            _group(context, Icons.shield_outlined, s.t('security'),
                children: [
              _tile(context, Icons.security, s.t('security_lock'),
                  () => go(const SecuritySettingsScreen())),
            ]),
            // 3) النسخ والمزامنة.
            _group(context, Icons.backup_outlined, s.t('group_backup'),
                children: [
              _tile(context, Icons.backup_outlined, s.t('backup'),
                  () => go(const BackupScreen())),
              _tile(context, Icons.cloud_sync_outlined, s.t('cloud_sync'),
                  () => go(const CloudSyncScreen())),
            ]),
            const Divider(),
            _tile(context, Icons.settings_outlined, s.t('settings'),
                () => go(const SettingsScreen())),
          ],
        ),
      ),
    );
  }

  Widget _tile(BuildContext context, IconData icon, String label,
      VoidCallback onTap) {
    return ListTile(
      leading: Icon(icon),
      title: Text(label, style: const TextStyle(fontWeight: FontWeight.bold)),
      onTap: onTap,
    );
  }

  /// مجموعة قابلة للطيّ (تمدّد/انكماش) تضمّ عناصر متشابهة.
  Widget _group(BuildContext context, IconData icon, String title,
      {required List<Widget> children, bool initiallyExpanded = false}) {
    return ExpansionTile(
      leading: Icon(icon),
      title: Text(title,
          style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 15)),
      initiallyExpanded: initiallyExpanded,
      shape: const Border(),
      collapsedShape: const Border(),
      childrenPadding: const EdgeInsetsDirectional.only(start: 12),
      children: children,
    );
  }
}
