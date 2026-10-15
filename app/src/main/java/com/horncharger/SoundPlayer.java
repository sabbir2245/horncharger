package com.horncharger;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;

import java.io.File;

public final class SoundPlayer {

    private static final long MAX_PLAY_MS = 5000L;

    private static SoundPlayer sActive;

    private final Context ctx;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable stopRunnable = this::stop;

    private MediaPlayer player;
    private PowerManager.WakeLock wakeLock;

    public SoundPlayer(Context context) {
        this.ctx = context.getApplicationContext();
    }

    private static AudioAttributes audioAttributes() {
        return new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();
    }

    public boolean play(File file, int volumePercent) {
        if (file == null || !file.exists()) {
            return false;
        }
        stop();
        try {
            MediaPlayer mp = new MediaPlayer();
            mp.setAudioAttributes(audioAttributes());
            mp.setDataSource(file.getAbsolutePath());
            mp.prepare();
            return start(mp, volumePercent);
        } catch (Exception e) {
            stop();
            return false;
        }
    }

    public boolean playRaw(int rawId, int volumePercent) {
        stop();
        try {
            MediaPlayer mp = MediaPlayer.create(ctx, rawId, audioAttributes(),
                    AudioManager.AUDIO_SESSION_ID_GENERATE);
            if (mp == null) {
                return false;
            }
            return start(mp, volumePercent);
        } catch (Exception e) {
            stop();
            return false;
        }
    }

    private boolean start(MediaPlayer mp, int volumePercent) {
        float v = Math.max(0f, Math.min(1f, volumePercent / 100f));
        v = v * v; // perceptual (squared) curve
        mp.setVolume(v, v);
        mp.setOnCompletionListener(m -> stop());
        mp.setOnErrorListener((m, what, extra) -> {
            stop();
            return true;
        });

        player = mp;
        acquireWakeLock();
        try {
            mp.start();
        } catch (Exception e) {
            stop();
            return false;
        }

        handler.removeCallbacks(stopRunnable);
        handler.postDelayed(stopRunnable, MAX_PLAY_MS);
        sActive = this;
        return true;
    }

    private void acquireWakeLock() {
        try {
            PowerManager pm = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
            if (pm == null) {
                return;
            }
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "HornCharger:play");
            wakeLock.setReferenceCounted(false);
            wakeLock.acquire(60000L);
        } catch (Exception ignored) {
        }
    }

    public void stop() {
        handler.removeCallbacks(stopRunnable);
        if (player != null) {
            try {
                if (player.isPlaying()) {
                    player.stop();
                }
            } catch (Exception ignored) {
            }
            try {
                player.release();
            } catch (Exception ignored) {
            }
            player = null;
        }
        if (wakeLock != null) {
            try {
                if (wakeLock.isHeld()) {
                    wakeLock.release();
                }
            } catch (Exception ignored) {
            }
            wakeLock = null;
        }
        if (sActive == this) {
            sActive = null;
        }
    }
}
