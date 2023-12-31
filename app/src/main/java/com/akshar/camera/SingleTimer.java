package com.akshar.camera;

import java.util.Timer;
import java.util.TimerTask;

public class SingleTimer {
    private static final SingleTimer ourInstance = new SingleTimer();
    private Timer timer;
    private boolean isRunning = false;
    private TimerTask timerTask;
    private SingleTimer() {
    }

    public synchronized static SingleTimer getInstance() {
        return ourInstance;
    }

    public void start(TimerTask timerTask, int delay) {
        if (isRunning)
            stop();

        this.timerTask = timerTask;
        timer = new Timer();
        isRunning = true;
        timer.schedule(timerTask, 0, delay);
    }

    public void stop() {
        timer.cancel();
        timer.purge();
        timerTask.cancel();
        isRunning = false;
    }

    public void changeDelay(int newDelay) {
        start(timerTask, newDelay);
    }
}
