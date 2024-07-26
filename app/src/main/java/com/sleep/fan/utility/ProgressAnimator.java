package com.sleep.fan.utility;

import android.animation.ObjectAnimator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ProgressBar;

public class ProgressAnimator {
    private ProgressBar progressBar;
    private int progressTo;

    public ProgressAnimator(ProgressBar progressBar) {
        this.progressBar = progressBar;
    }

    public void animateProgress(int progressTo) {
        this.progressTo = progressTo;
        ObjectAnimator animation = ObjectAnimator.ofInt(progressBar, "progress", progressBar.getProgress(), progressTo);
        animation.setDuration(1000); // Adjust the duration as needed
        animation.setInterpolator(new DecelerateInterpolator());
        animation.start();
    }

    public int getProgressTo() {
        return progressTo;
    }
}
