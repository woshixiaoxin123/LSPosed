package io.github.lsposed.manager.util;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.core.app.NotificationChannelCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.topjohnwu.superuser.Shell;

import io.github.lsposed.manager.App;
import io.github.lsposed.manager.R;
import io.github.lsposed.manager.ui.activity.MainActivity;
import io.github.lsposed.manager.ui.activity.ModulesActivity;

@SuppressLint("UnspecifiedImmutableFlag")
public final class NotificationUtil {

    public static final int NOTIFICATION_MODULE_NOT_ACTIVATED_YET = 0;
    private static final int NOTIFICATION_MODULES_UPDATED = 1;
    private static final int PENDING_INTENT_OPEN_MODULES = 0;
    private static final int PENDING_INTENT_OPEN_INSTALL = 1;
    private static final int PENDING_INTENT_SOFT_REBOOT = 2;
    private static final int PENDING_INTENT_REBOOT = 3;

    private static final String NOTIFICATION_MODULES_CHANNEL = "modules_channel_2";

    @SuppressLint("StaticFieldLeak")
    private static Context context = null;
    @SuppressLint("StaticFieldLeak")
    private static NotificationManagerCompat notificationManager;

    public static void init() {
        if (context != null) {
            return;
        }

        context = App.getInstance();
        notificationManager = NotificationManagerCompat.from(context);

        NotificationChannelCompat.Builder channel = new NotificationChannelCompat.Builder(NOTIFICATION_MODULES_CHANNEL,
                NotificationManager.IMPORTANCE_HIGH)
                .setName(context.getString(R.string.nav_item_modules))
                .setSound(null, null)
                .setVibrationPattern(null);
        notificationManager.createNotificationChannel(channel.build());
    }

    public static void cancel(String tag, int id) {
        notificationManager.cancel(tag, id);
    }

    public static void cancelAll() {
        notificationManager.cancelAll();
    }

    public static void showNotActivatedNotification(String packageName, String appName) {
        Intent intent = new Intent(context, ModulesActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent pModulesTab = PendingIntent.getActivity(context, PENDING_INTENT_OPEN_MODULES, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        String title = context.getString(R.string.module_is_not_activated_yet);
        NotificationCompat.Builder builder = getNotificationBuilder(title, appName)
                .setContentIntent(pModulesTab);

        NotificationCompat.BigTextStyle style = new NotificationCompat.BigTextStyle();
        style.setBigContentTitle(title);
        style.bigText(context.getString(R.string.module_is_not_activated_yet_detailed, appName));
        builder.setStyle(style);

        notificationManager.notify(packageName, NOTIFICATION_MODULE_NOT_ACTIVATED_YET, builder.build());
    }

    public static void showModulesUpdatedNotification(String appName) {
        Intent intent = new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent pInstallTab = PendingIntent.getActivity(context, PENDING_INTENT_OPEN_INSTALL, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        String title = context
                .getString(R.string.xposed_module_updated_notification_title);
        NotificationCompat.Builder builder = getNotificationBuilder(title, appName)
                .setContentIntent(pInstallTab);

        Intent iSoftReboot = new Intent(context, RebootReceiver.class);
        iSoftReboot.putExtra(RebootReceiver.EXTRA_SOFT_REBOOT, true);
        PendingIntent pSoftReboot = PendingIntent.getBroadcast(context, PENDING_INTENT_SOFT_REBOOT,
                iSoftReboot, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent iReboot = new Intent(context, RebootReceiver.class);
        PendingIntent pReboot = PendingIntent.getBroadcast(context, PENDING_INTENT_REBOOT,
                iReboot, PendingIntent.FLAG_UPDATE_CURRENT);

        builder.addAction(new NotificationCompat.Action.Builder(0, context.getString(R.string.reboot), pReboot).build());
        builder.addAction(new NotificationCompat.Action.Builder(0, context.getString(R.string.soft_reboot), pSoftReboot).build());

        notificationManager.notify(null, NOTIFICATION_MODULES_UPDATED, builder.build());
    }

    private static NotificationCompat.Builder getNotificationBuilder(String title, String message) {
        return new NotificationCompat.Builder(context, NOTIFICATION_MODULES_CHANNEL)
                .setContentTitle(title)
                .setContentText(message)
                .setSound(null)
                .setVibrate(new long[]{0})
                .setAutoCancel(true)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(ContextCompat.getColor(context, R.color.colorPrimary));
    }

    public static class RebootReceiver extends BroadcastReceiver {
        public static String EXTRA_SOFT_REBOOT = "soft";

        @Override
        public void onReceive(Context context, Intent intent) {
            /*
             * Close the notification bar in order to see the toast that module
             * was enabled successfully. Furthermore, if SU permissions haven't
             * been granted yet, the SU dialog will be prompted behind the
             * expanded notification panel and is therefore not visible to the
             * user.
             */
            context.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
            cancelAll();

            if (!Shell.rootAccess()) {
                Log.e(App.TAG, "NotificationUtil -> Could not start root shell");
                return;
            }

            boolean softReboot = intent.getBooleanExtra(EXTRA_SOFT_REBOOT, false);
            RebootUtil.reboot(softReboot ? RebootUtil.RebootType.USERSPACE : RebootUtil.RebootType.NORMAL);
        }
    }
}