package com.horncharger;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends Activity {

    private static final int REQ_PICK = 1001;

    private TextView fileNameText;
    private TextView volumeLabel;
    private TextView statusText;
    private SeekBar volumeSeek;
    private CheckBox chargerChk;
    private CheckBox headphoneChk;

    private SoundPlayer previewPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fileNameText = findViewById(R.id.fileNameText);
        volumeLabel = findViewById(R.id.volumeLabel);
        statusText = findViewById(R.id.statusText);
        volumeSeek = findViewById(R.id.volumeSeek);
        chargerChk = findViewById(R.id.chargerChk);
        headphoneChk = findViewById(R.id.headphoneChk);

        previewPlayer = new SoundPlayer(this);

        int volume = Prefs.getVolume(this);
        volumeSeek.setProgress(volume);
        volumeLabel.setText("Volume: " + volume + "%");

        chargerChk.setChecked(Prefs.isChargerEnabled(this));
        headphoneChk.setChecked(Prefs.isHeadphoneEnabled(this));

        findViewById(R.id.importBtn).setOnClickListener(v -> pickAudio());
        findViewById(R.id.previewBtn).setOnClickListener(v -> preview());

        volumeSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                volumeLabel.setText("Volume: " + progress + "%");
                if (fromUser) {
                    Prefs.setVolume(MainActivity.this, progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Prefs.setVolume(MainActivity.this, seekBar.getProgress());
            }
        });

        chargerChk.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Prefs.setChargerEnabled(MainActivity.this, isChecked);
            updateServiceState();
        });

        headphoneChk.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Prefs.setHeadphoneEnabled(MainActivity.this, isChecked);
            updateServiceState();
        });

        requestNotificationPermission();

        if (Prefs.isAnyEnabled(this)) {
            PlayService.start(this);
        }

        refreshUi();
    }

    private void updateServiceState() {
        if (Prefs.isAnyEnabled(this)) {
            PlayService.start(this);
        } else {
            PlayService.stop(this);
        }
        refreshUi();
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 2001);
        }
    }

    private void refreshUi() {
        File sound = Prefs.getSoundFile(this);
        if (sound.exists()) {
            long kb = Math.max(1, sound.length() / 1024);
            fileNameText.setText("Sound: imported (" + kb + " KB)");
        } else {
            fileNameText.setText("Sound: default (bundled)");
        }
        boolean charger = Prefs.isChargerEnabled(this);
        boolean headphone = Prefs.isHeadphoneEnabled(this);
        if (!charger && !headphone) {
            statusText.setText("Disabled. Enable charger and/or headphone above.");
        } else if (charger && headphone) {
            statusText.setText("Enabled. Plays on charger plug-in and headphone connect.");
        } else if (charger) {
            statusText.setText("Enabled. Plays on charger plug-in.");
        } else {
            statusText.setText("Enabled. Plays on headphone connect.");
        }
    }

    private void pickAudio() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("audio/*");
        try {
            startActivityForResult(intent, REQ_PICK);
        } catch (Exception e) {
            Toast.makeText(this, "No file picker available", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_PICK || resultCode != RESULT_OK || data == null) {
            return;
        }
        Uri uri = data.getData();
        if (uri == null) {
            return;
        }
        if (copyToInternal(uri)) {
            Toast.makeText(this, "Sound imported", Toast.LENGTH_SHORT).show();
            refreshUi();
        } else {
            Toast.makeText(this, "Import failed", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean copyToInternal(Uri uri) {
        File dst = Prefs.getSoundFile(this);
        InputStream in = null;
        OutputStream out = null;
        try {
            in = getContentResolver().openInputStream(uri);
            if (in == null) {
                return false;
            }
            out = new FileOutputStream(dst);
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            out.flush();
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            closeQuietly(in);
            closeQuietly(out);
        }
    }

    private void preview() {
        File sound = Prefs.getSoundFile(this);
        if (sound.exists()) {
            previewPlayer.play(sound, Prefs.getVolume(this));
        } else {
            previewPlayer.playRaw(R.raw.charge_sound, Prefs.getVolume(this));
        }
    }

    private static void closeQuietly(java.io.Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        previewPlayer.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        previewPlayer.stop();
    }
}
