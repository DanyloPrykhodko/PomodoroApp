package com.weffle.pomodoro;

import android.content.Context;
import android.content.DialogInterface;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    private final long SESSION_TIME = 1_500_000L; // 25 minutes

    private ProgressBar progressBar; // Progress bar.
    private TextView timeView; // Time.
    private Button startStopButton; // Start/stop button.
    private Button breakButton; // Break/+5 min button.

    private Thread timerThread; // Timer thread.
    private long startTime; // Timer start time.
    private long stopTime; // Timer stop time.
    private boolean work; // Work session.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        timeView = (TextView) findViewById(R.id.textView);
        startStopButton = (Button) findViewById(R.id.startStopButton);
        breakButton = (Button) findViewById(R.id.breakButton);

        timeView.setText(getClock((int) (SESSION_TIME / 1000)));

        setColor(R.color.colorRed);

    }

    /**
     * Start/stop button on click.
     *
     * @param view View.
     */
    public void startStopOnClick(View view) {
        if (startStopButton.getText().equals(getString(R.string.start_title))) {
            work = true;
            start(SESSION_TIME);
        } else {
            work = false;
            timerThread.interrupt();
            reset();
        }
    }

    /**
     * Break/+5 min button on click.
     *
     * @param view View.
     */
    public void breakOnClick(View view) {
        long breakTime = 300_000L; // 5 minutes
        if (breakButton.getText().equals(getString(R.string.break_title))) {
            setColor(R.color.colorGreen);
            start(breakTime);
        } else stopTime += breakTime;
    }

    /**
     * Start timer session.
     *
     * @param session Session time.
     */
    private void start(long session) {
        startStopButton.setText(R.string.stop_title);
        breakButton.setText(R.string.add_title);
        startStopButton.setText(R.string.stop_title);
        startTime = System.currentTimeMillis();
        stopTime = startTime + session;
        timerThread = createTimerThread();
        timerThread.start();
    }

    /**
     * Create new timer.
     *
     * @return Timer thread
     */
    private Thread createTimerThread() {
        return new Thread() {
            @Override
            public void run() {
                long currentTime;
                while ((currentTime = System.currentTimeMillis()) < stopTime) {
                    if (isInterrupted()) return;
                    final long time = stopTime - currentTime;
                    final int progress = (int) ((float) time / (float) (stopTime - startTime) * 10000);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            timeView.setText(getClock((int) (time / 1000)));
                            progressBar.setProgress(progress);
                        }
                    });
                    try {
                        sleep(50);
                    } catch (InterruptedException e) {
                        interrupt();
                    }
                }
                String message = work ? getString(R.string.rest) : getString(R.string.work);
                alert(progressBar.getRootView().getContext(), message);
            }
        };
    }

    /**
     * Show alert and play ringtone.
     *
     * @param context Context.
     * @param message Message text.
     */
    private void alert(final Context context, final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
                    final Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);

                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setTitle(getString(R.string.over));
                    builder.setMessage(message);
                    builder.setCancelable(false);
                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            r.stop();
                            reset();
                        }
                    });
                    AlertDialog alert = builder.create();
                    alert.show();
                    r.play();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Reset timer.
     */
    private void reset() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                startStopButton.setText(R.string.start_title);
                breakButton.setText(R.string.break_title);
                timeView.setText(getClock((int) (SESSION_TIME / 1000)));
                progressBar.setProgress(progressBar.getMax());
                setColor(R.color.colorRed);
            }
        });
    }

    /**
     * Set color of GUI elements.
     *
     * @param id Color id.
     */
    private void setColor(int id) {
        progressBar.setProgressTintList(getColorStateList(id));
        timeView.setTextColor(getColorStateList(id));
        startStopButton.setTextColor(getColorStateList(id));
        breakButton.setTextColor(getColorStateList(id));
    }

    /**
     * Get digital clock time.
     *
     * @param time Time in seconds.
     * @return Digital clock.
     */
    private String getClock(int time) {
        StringBuilder builder = new StringBuilder();
        int min = time / 60;
        if (min < 10) builder.append('0');
        builder.append(min).append(':');
        int sec = time % 60;
        if (sec < 10) builder.append('0');
        builder.append(sec);
        return builder.toString();
    }
}