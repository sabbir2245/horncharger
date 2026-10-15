package com.horncharger;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

import java.io.File;

public class PlayService extends Service {

    public static final String ACTION_START = "com.horncharger.START";
    public static final String ACTION_STOP = "com.horncharger.STOP";

    private static final String CHANNEL_ID = "horncharger";
    private static final int NOTIF_ID = 1;

    private static final String ACTION_HEADSET_PLUG = "android.intent.action.HEADSET_PLUG";
    private static final String ACTION_USB_HEADSET_PLUG = "android.intent.action.USB_HEADSET_PLUG";

    private BroadcastReceiver chargerReceiver;

    public static void start(Context context) {
        Intent intent = new Intent(context, PlayService.class).setAction(ACTION_START);
        startServiceCompat(context, intent);
    }

    public static void stop(Context context) {
        Intent intent = new Intent(context, PlayService.class).setAction(ACTION_STOP);
        try {
            context.startService(intent);
        } catch (Exception ignored) {
        }
    }

    private static void startServiceCompat(Context context, Intent intent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent);
            } else {
                context.startService(intent);
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }

        createChannel();
        startForegroundCompat();
        registerChargerReceiver();
        return START_STICKY;
    }

    private void startForegroundCompat() {
        Notification notification = buildNotification();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIF_ID, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(NOTIF_ID, notification);
        }
    }

    private Notification buildNotification() {
        Intent open = new Intent(this, MainActivity.class);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, open, flags);

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }
        return builder
                .setContentTitle("HornCharger")
                .setContentText("Ready to play on charger plug-in")
                .setSmallIcon(R.drawable.ic_notif)
                .setContentIntent(contentIntent)
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_LOW)
                .build();
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "Charger sound", NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Keeps HornCharger ready to play on charger plug-in");
        nm.createNotificationChannel(channel);
    }

    private void registerChargerReceiver() {
        if (chargerReceiver != null) {
            return;
        }
        chargerReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent == null || intent.getAction() == null) {
                    return;
                }
                String action = intent.getAction();
                if (Intent.ACTION_POWER_CONNECTED.equals(action)) {
                    if (Prefs.isChargerEnabled(context)) {
                        play(context);
                    }
                } else if (ACTION_HEADSET_PLUG.equals(action)
                        || ACTION_USB_HEADSET_PLUG.equals(action)) {
                    int state = intent.getIntExtra("state", 0);
                    if (state == 1 && Prefs.isHeadphoneEnabled(context)) {
                        play(context);
                    }
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_POWER_CONNECTED);
        filter.addAction(ACTION_HEADSET_PLUG);
        filter.addAction(ACTION_USB_HEADSET_PLUG);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(chargerReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(chargerReceiver, filter);
        }
    }

    private static void play(Context context) {
        SoundPlayer player = new SoundPlayer(context);
        File sound = Prefs.getSoundFile(context);
        if (sound.exists()) {
            player.play(sound, Prefs.getVolume(context));
        } else {
            player.playRaw(R.raw.charge_sound, Prefs.getVolume(context));
        }
    }

    @Override
    public void onDestroy() {
        if (chargerReceiver != null) {
            try {
                unregisterReceiver(chargerReceiver);
            } catch (Exception ignored) {
            }
            chargerReceiver = null;
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
