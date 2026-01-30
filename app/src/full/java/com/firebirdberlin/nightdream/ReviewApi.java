package com.firebirdberlin.nightdream;

import android.content.Context;
import android.util.Log;

import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.google.android.gms.tasks.Task;

public class ReviewApi {

    private static final String TAG = "ReviewApi";

    public static void askForReview(Context context) {
        Log.d(TAG, "askForReview called (full flavor)");
        ReviewManager manager = ReviewManagerFactory.create(context);
        Task<ReviewInfo> request = manager.requestReviewFlow();
        request.addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // We got the ReviewInfo object
                ReviewInfo reviewInfo = task.getResult();
                Log.d(TAG, "ReviewInfo obtained. Launching review flow.");
                Task<Void> flow = manager.launchReviewFlow((PreferencesActivity) context, reviewInfo);
                flow.addOnCompleteListener(flowTask -> {
                    // The review flow has finished. Acknowledge the user that the review process has finished.
                    // Note: The review flow might not have been shown to the user if conditions aren't met.
                    Log.d(TAG, "Review flow finished. Success: " + flowTask.isSuccessful());
                });
            } else {
                // There was some problem, log or handle the error code.
                Log.e(TAG, "Failed to get ReviewInfo: " + task.getException());
            }
        });
    }
}
