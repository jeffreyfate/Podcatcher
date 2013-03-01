package com.jeffthefate.pod_dev;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.os.Build;
import android.text.method.ScrollingMovementMethod;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.LinearInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

public abstract class VersionedAnimateDetails {
    
    public abstract VersionedAnimateDetails create(
            TextView detailEpisodeDescription, RelativeLayout speedButton,
            ImageView skipButton, SeekBar seekBar, ProgressBar progressBar,
            FullScreenImageView feedImage, LinearLayout progressLayout,
            RelativeLayout timeLayout, ImageView otherViewButtonBottom,
            RelativeLayout episodeDetailsLayout);
    public abstract boolean animate();
    public abstract boolean unanimate();
    
    public static VersionedAnimateDetails newInstance() {
        final int sdkVersion = Build.VERSION.SDK_INT;
        VersionedAnimateDetails view = null;
        if (sdkVersion < Build.VERSION_CODES.HONEYCOMB)
            view = new GingerbreadAnimateDetails();
        else
            view = new HoneycombAnimateDetails();

        return view;
    }
    
    private static class GingerbreadAnimateDetails extends
            VersionedAnimateDetails {
        TextView detailEpisodeDescription;
        RelativeLayout speedButton;
        ImageView skipButton;
        SeekBar seekBar;
        ProgressBar progressBar;
        FullScreenImageView feedImage;
        LinearLayout progressLayout;
        RelativeLayout timeLayout;
        ImageView otherViewButtonBottom;
        RelativeLayout episodeDetailsLayout;
        @Override
        public VersionedAnimateDetails create(TextView detailEpisodeDescription,
                RelativeLayout speedButton, ImageView skipButton,
                SeekBar seekBar, ProgressBar progressBar,
                FullScreenImageView feedImage, LinearLayout progressLayout,
                RelativeLayout timeLayout, ImageView otherViewButtonBottom,
                RelativeLayout episodeDetailsLayout) {
            this.detailEpisodeDescription = detailEpisodeDescription;
            this.speedButton = speedButton;
            this.skipButton = skipButton;
            this.seekBar = seekBar;
            this.progressBar = progressBar;
            this.feedImage = feedImage;
            this.progressLayout = progressLayout;
            this.timeLayout = timeLayout;
            this.otherViewButtonBottom = otherViewButtonBottom;
            this.episodeDetailsLayout = episodeDetailsLayout;
            return this;
        }
        @Override
        public boolean animate() {
            final AlphaAnimation episodeDetailsAnim =
                new AlphaAnimation(0.0f, 1.0f);
            episodeDetailsAnim.setDuration(50);
            episodeDetailsAnim.setInterpolator(new LinearInterpolator());
            episodeDetailsAnim.setAnimationListener(new AnimationListener() {
                @Override
                public void onAnimationEnd(Animation animation) {
                    detailEpisodeDescription.setMovementMethod(
                            new ScrollingMovementMethod());
                }
                @Override
                public void onAnimationRepeat(Animation animation) {}
                @Override
                public void onAnimationStart(Animation animation) {} 
            });
            ScaleAnimation feedImageAnim = new ScaleAnimation(1.0f, 0.5f, 1.0f,
                    0.5f, Animation.RELATIVE_TO_SELF, 0.0f,
                    Animation.RELATIVE_TO_SELF, 0.0f);
            feedImageAnim.setDuration(100);
            feedImageAnim.setInterpolator(new LinearInterpolator());
            feedImageAnim.setAnimationListener(new AnimationListener() {
                @Override
                public void onAnimationEnd(Animation animation) {
                    detailEpisodeDescription.startAnimation(episodeDetailsAnim);
                }
                @Override
                public void onAnimationRepeat(Animation animation) {}
                @Override
                public void onAnimationStart(Animation animation) {} 
            });
            AlphaAnimation progressAnim = new AlphaAnimation(1.0f, 0.0f);
            progressAnim.setDuration(100);
            progressAnim.setInterpolator(new LinearInterpolator());
            AlphaAnimation timeAnim = new AlphaAnimation(0.0f, 1.0f);
            timeAnim.setDuration(100);
            timeAnim.setInterpolator(new LinearInterpolator());
            AlphaAnimation speedAnim = new AlphaAnimation(0.0f, 1.0f);
            speedAnim.setDuration(100);
            speedAnim.setInterpolator(new LinearInterpolator());
            speedAnim.setAnimationListener(new AnimationListener() {
                @Override
                public void onAnimationEnd(Animation animation) {
                    speedButton.setEnabled(true);
                }
                @Override
                public void onAnimationRepeat(Animation animation) {}
                @Override
                public void onAnimationStart(Animation animation) {} 
            });
            AlphaAnimation skipAnim = new AlphaAnimation(0.0f, 1.0f);
            skipAnim.setDuration(100);
            skipAnim.setInterpolator(new LinearInterpolator());
            skipAnim.setAnimationListener(new AnimationListener() {
                @Override
                public void onAnimationEnd(Animation animation) {
                    skipButton.setEnabled(true);
                }
                @Override
                public void onAnimationRepeat(Animation animation) {}
                @Override
                public void onAnimationStart(Animation animation) {} 
            });
            AlphaAnimation seekAnim = new AlphaAnimation(0.0f, 1.0f);
            seekAnim.setDuration(100);
            seekAnim.setInterpolator(new LinearInterpolator());
            seekAnim.setAnimationListener(new AnimationListener() {
                @Override
                public void onAnimationEnd(Animation animation) {
                    seekBar.setEnabled(true);
                }
                @Override
                public void onAnimationRepeat(Animation animation) {}
                @Override
                public void onAnimationStart(Animation animation) {} 
            });
            AlphaAnimation progAnim = new AlphaAnimation(1.0f, 0.0f);
            progAnim.setDuration(100);
            progAnim.setInterpolator(new LinearInterpolator());
            progAnim.setAnimationListener(new AnimationListener() {
                @Override
                public void onAnimationEnd(Animation animation) {
                    progressBar.setEnabled(false);
                }
                @Override
                public void onAnimationRepeat(Animation animation) {}
                @Override
                public void onAnimationStart(Animation animation) {} 
            });
            feedImage.startAnimation(feedImageAnim);
            progressLayout.startAnimation(progressAnim);
            timeLayout.startAnimation(timeAnim);
            speedButton.startAnimation(speedAnim);
            skipButton.startAnimation(skipAnim);
            seekBar.startAnimation(seekAnim);
            progressBar.startAnimation(progAnim);
            otherViewButtonBottom.setEnabled(false);
            return true;
        }
        @Override
        public boolean unanimate() {
            final ScaleAnimation feedImageAnim = new ScaleAnimation(0.5f, 1.0f, 0.5f,
                    1.0f, Animation.RELATIVE_TO_SELF, 0.0f,
                    Animation.RELATIVE_TO_SELF, 0.0f);
            feedImageAnim.setDuration(100);
            feedImageAnim.setInterpolator(new LinearInterpolator());
            AlphaAnimation progressAnim = new AlphaAnimation(0.0f, 1.0f);
            progressAnim.setDuration(100);
            progressAnim.setInterpolator(new LinearInterpolator());
            AlphaAnimation timeAnim = new AlphaAnimation(1.0f, 0.0f);
            timeAnim.setDuration(100);
            timeAnim.setInterpolator(new LinearInterpolator());
            AlphaAnimation speedAnim = new AlphaAnimation(0.0f, 1.0f);
            speedAnim.setDuration(100);
            speedAnim.setInterpolator(new LinearInterpolator());
            speedAnim.setAnimationListener(new AnimationListener() {
                @Override
                public void onAnimationEnd(Animation animation) {
                    speedButton.setEnabled(false);
                }
                @Override
                public void onAnimationRepeat(Animation animation) {}
                @Override
                public void onAnimationStart(Animation animation) {} 
            });
            AlphaAnimation skipAnim = new AlphaAnimation(1.0f, 0.0f);
            skipAnim.setDuration(100);
            skipAnim.setInterpolator(new LinearInterpolator());
            skipAnim.setAnimationListener(new AnimationListener() {
                @Override
                public void onAnimationEnd(Animation animation) {
                    skipButton.setEnabled(false);
                }
                @Override
                public void onAnimationRepeat(Animation animation) {}
                @Override
                public void onAnimationStart(Animation animation) {} 
            });
            AlphaAnimation seekAnim = new AlphaAnimation(1.0f, 0.0f);
            seekAnim.setDuration(100);
            seekAnim.setInterpolator(new LinearInterpolator());
            seekAnim.setAnimationListener(new AnimationListener() {
                @Override
                public void onAnimationEnd(Animation animation) {
                    seekBar.setEnabled(false);
                }
                @Override
                public void onAnimationRepeat(Animation animation) {}
                @Override
                public void onAnimationStart(Animation animation) {} 
            });
            AlphaAnimation progAnim = new AlphaAnimation(0.0f, 1.0f);
            progAnim.setDuration(100);
            progAnim.setInterpolator(new LinearInterpolator());
            progAnim.setAnimationListener(new AnimationListener() {
                @Override
                public void onAnimationEnd(Animation animation) {
                    progressBar.setEnabled(true);
                }
                @Override
                public void onAnimationRepeat(Animation animation) {}
                @Override
                public void onAnimationStart(Animation animation) {} 
            });
            final AlphaAnimation episodeDetailsAnim =
                new AlphaAnimation(1.0f, 0.0f);
            episodeDetailsAnim.setDuration(50);
            episodeDetailsAnim.setInterpolator(new LinearInterpolator());
            episodeDetailsAnim.setAnimationListener(new AnimationListener() {
                @Override
                public void onAnimationEnd(Animation animation) {
                    detailEpisodeDescription.setMovementMethod(null);
                    feedImage.startAnimation(feedImageAnim);
                }
                @Override
                public void onAnimationRepeat(Animation animation) {}
                @Override
                public void onAnimationStart(Animation animation) {} 
            });
            episodeDetailsLayout.startAnimation(episodeDetailsAnim);
            progressLayout.startAnimation(progressAnim);
            timeLayout.startAnimation(timeAnim);
            speedButton.startAnimation(speedAnim);
            skipButton.startAnimation(skipAnim);
            seekBar.startAnimation(seekAnim);
            progressBar.startAnimation(progAnim);
            otherViewButtonBottom.setEnabled(true);
            return false;
        }
    }
    
    private static class HoneycombAnimateDetails extends
            GingerbreadAnimateDetails {
        @Override
        public boolean animate() {
            final ValueAnimator epDetailsAnim = ObjectAnimator.ofFloat(
                    episodeDetailsLayout, "alpha", 0.0f, 1.0f);
            epDetailsAnim.setDuration(50);
            epDetailsAnim.setInterpolator(new LinearInterpolator());
            epDetailsAnim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    detailEpisodeDescription.setMovementMethod(
                            new ScrollingMovementMethod());
                }
            });
            ValueAnimator imageAnimX = ObjectAnimator.ofFloat(feedImage, "scaleX",
                    0.5f);
            imageAnimX.setDuration(100);
            imageAnimX.setInterpolator(new LinearInterpolator());
            imageAnimX.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    epDetailsAnim.start();
                }
            });
            ValueAnimator imageAnimY = ObjectAnimator.ofFloat(feedImage, "scaleY", 
                    0.5f);
            imageAnimY.setDuration(100);
            imageAnimY.setInterpolator(new LinearInterpolator());
            ValueAnimator progressAnimFade = ObjectAnimator.ofFloat(progressLayout,
                    "alpha", 1.0f, 0.0f);
            progressAnimFade.setDuration(100);
            progressAnimFade.setInterpolator(new LinearInterpolator());
            ValueAnimator timeAnimFade = ObjectAnimator.ofFloat(timeLayout, "alpha",
                    0.0f, 1.0f);
            timeAnimFade.setDuration(100);
            timeAnimFade.setInterpolator(new LinearInterpolator());
            ValueAnimator speedAnimFade = ObjectAnimator.ofFloat(speedButton, 
                    "alpha", 0.0f, 1.0f);
            speedAnimFade.setDuration(100);
            speedAnimFade.setInterpolator(new LinearInterpolator());
            speedAnimFade.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    speedButton.setEnabled(true);
                }
            });
            ValueAnimator skipAnimFade = ObjectAnimator.ofFloat(skipButton, "alpha",
                    0.0f, 1.0f);
            skipAnimFade.setDuration(100);
            skipAnimFade.setInterpolator(new LinearInterpolator());
            skipAnimFade.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    skipButton.setEnabled(true);
                }
            });
            ValueAnimator seekAnimFade = ObjectAnimator.ofFloat(seekBar, "alpha",
                    0.0f, 1.0f);
            seekAnimFade.setDuration(100);
            seekAnimFade.setInterpolator(new LinearInterpolator());
            seekAnimFade.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    seekBar.setEnabled(true);
                }
            });
            ValueAnimator progAnimFade = ObjectAnimator.ofFloat(progressBar,
                    "alpha", 1.0f, 0.0f);
            progAnimFade.setDuration(100);
            progAnimFade.setInterpolator(new LinearInterpolator());
            progAnimFade.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    progressBar.setEnabled(false);
                }
            });
            AnimatorSet imageAnimSet = new AnimatorSet();
            imageAnimSet.play(imageAnimX).with(imageAnimY);
            imageAnimSet.play(imageAnimX).with(progressAnimFade);
            imageAnimSet.play(imageAnimX).with(timeAnimFade);
            imageAnimSet.play(imageAnimX).with(speedAnimFade);
            imageAnimSet.play(imageAnimX).with(skipAnimFade);
            imageAnimSet.play(imageAnimX).with(seekAnimFade);
            imageAnimSet.play(imageAnimX).with(progAnimFade);
            imageAnimSet.start();
            otherViewButtonBottom.setEnabled(false);
            return true;
        }
        @Override
        public boolean unanimate() {
            ValueAnimator imageAnimX = ObjectAnimator.ofFloat(feedImage, "scaleX", 
                    1.0f);
            imageAnimX.setDuration(100);
            imageAnimX.setInterpolator(new LinearInterpolator());
            ValueAnimator imageAnimY = ObjectAnimator.ofFloat(feedImage, "scaleY", 
                    1.0f);
            imageAnimY.setDuration(100);
            imageAnimY.setInterpolator(new LinearInterpolator());
            ValueAnimator progressAnimFade = ObjectAnimator.ofFloat(progressLayout,
                    "alpha", 0.0f, 1.0f);
            progressAnimFade.setDuration(100);
            progressAnimFade.setInterpolator(new LinearInterpolator());
            ValueAnimator timeAnimFade = ObjectAnimator.ofFloat(timeLayout, "alpha",
                    1.0f, 0.0f);
            timeAnimFade.setDuration(100);
            timeAnimFade.setInterpolator(new LinearInterpolator());
            ValueAnimator speedAnimFade = ObjectAnimator.ofFloat(speedButton, 
                    "alpha", 1.0f, 0.0f);
            speedAnimFade.setDuration(100);
            speedAnimFade.setInterpolator(new LinearInterpolator());
            speedAnimFade.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    speedButton.setEnabled(false);
                }
            });
            ValueAnimator skipAnimFade = ObjectAnimator.ofFloat(skipButton, "alpha",
                    1.0f, 0.0f);
            skipAnimFade.setDuration(100);
            skipAnimFade.setInterpolator(new LinearInterpolator());
            skipAnimFade.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    skipButton.setEnabled(false);
                }
            });
            ValueAnimator seekAnimFade = ObjectAnimator.ofFloat(seekBar, "alpha",
                    1.0f, 0.0f);
            seekAnimFade.setDuration(100);
            seekAnimFade.setInterpolator(new LinearInterpolator());
            seekAnimFade.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    seekBar.setEnabled(false);
                }
            });
            ValueAnimator progAnimFade = ObjectAnimator.ofFloat(progressBar,
                    "alpha", 0.0f, 1.0f);
            progAnimFade.setDuration(100);
            progAnimFade.setInterpolator(new LinearInterpolator());
            progAnimFade.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    progressBar.setEnabled(true);
                }
            });
            final AnimatorSet imageAnimSet = new AnimatorSet();
            imageAnimSet.play(imageAnimX).with(imageAnimY).with(progressAnimFade)
                    .with(timeAnimFade).with(speedAnimFade).with(skipAnimFade)
                    .with(seekAnimFade).with(progAnimFade);
            ValueAnimator epDetailsAnim = ObjectAnimator.ofFloat(
                    episodeDetailsLayout, "alpha", 1.0f, 0.0f);
            epDetailsAnim.setDuration(50);
            epDetailsAnim.setInterpolator(new LinearInterpolator());
            epDetailsAnim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    detailEpisodeDescription.setMovementMethod(null);
                    imageAnimSet.start();
                }
            });
            epDetailsAnim.start();
            otherViewButtonBottom.setEnabled(true);
            return false;
        }
    }
    
}
