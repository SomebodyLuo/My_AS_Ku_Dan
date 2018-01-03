package io.orbi.ar.utils;

import android.os.Handler;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Ian Thew on 8/23/17.
 */

/*
USAGES:
// set timeout
TaskHandle handle = BasicTimer.setTimeout(new Runnable() {
    public void run() {
        Log.d(TAG, "Executed after 3000 ms!");
    }
}, 3000);


// set interval
TaskHandle handle = BasicTimer.setInterval(new Runnable() {
    public void run() {
        Log.d(TAG, "Executed every 3000 ms!");
    }
}, 3000);


handle.invalidate();   // to cancel
 */

public class BasicTimer
{
    // setTimeout, setInterval
    public interface TaskHandle {
        void invalidate();
    }

    public static TaskHandle setTimeout(final Runnable r, long delay) {
        final Handler h = new Handler();
        h.postDelayed(r, delay);
        return new TaskHandle() {
            @Override
            public void invalidate() {
                h.removeCallbacks(r);
            }
        };
    }

    public static TaskHandle setInterval(final Runnable r, long interval) {
        final Timer t = new Timer();
        final Handler h = new Handler();
        t.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                h.post(r);
            }
        }, interval, interval);  // Unlike JavaScript, in Java the initial call is immediate, so we put interval instead.
        return new TaskHandle() {
            @Override
            public void invalidate() {
                t.cancel();
                t.purge();
            }
        };
    }
}
