package com.example.musicplayer.playlist.activity;

import android.animation.ValueAnimator;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.musicplayer.R;
import com.example.musicplayer.Song;

import java.util.concurrent.TimeUnit;

public class PlayerUIHelper {

    private static final String TAG = "PlayerUIHelper";

    // Views
    public ImageView imgCover, imgBackground, imgVinyl;
    public ImageButton btnPlayPause, btnBack, btnNext, btnPrevious, btnShuffle, btnRepeat, btnLike, btnDownload;
    public TextView txtTitle, txtArtist, txtLyrics, txtCurrentTime, txtTotalTime;
    public SeekBar seekBar;
    public View gradientOverlay;

    private AppCompatActivity activity;
    private RotateAnimation rotateAnimation;
    private int dominantColor = Color.parseColor("#1DB954");

    public PlayerUIHelper(AppCompatActivity activity) {
        this.activity = activity;
        initViews();
    }

    private void initViews() {
        imgCover = activity.findViewById(R.id.imgCover);
        imgBackground = activity.findViewById(R.id.imgBackground);
        imgVinyl = activity.findViewById(R.id.imgVinyl);
        txtTitle = activity.findViewById(R.id.txtTitle);
        txtArtist = activity.findViewById(R.id.txtArtist);
        txtLyrics = activity.findViewById(R.id.txtLyrics);
        txtCurrentTime = activity.findViewById(R.id.txtCurrentTime);
        txtTotalTime = activity.findViewById(R.id.txtTotalTime);
        seekBar = activity.findViewById(R.id.seekBar);
        btnPlayPause = activity.findViewById(R.id.btnPlayPause);
        btnBack = activity.findViewById(R.id.btnBack);
        btnNext = activity.findViewById(R.id.btnNext);
        btnPrevious = activity.findViewById(R.id.btnPrevious);
        btnShuffle = activity.findViewById(R.id.btnShuffle);
        btnRepeat = activity.findViewById(R.id.btnRepeat);
        btnLike = activity.findViewById(R.id.btnLike);
        btnDownload = activity.findViewById(R.id.btnDownload);
        gradientOverlay = activity.findViewById(R.id.gradientOverlay);
    }

    public void loadSongUI(Song song, boolean isFavorite, Runnable onCoverLoaded) {
        txtTitle.setText(song.title != null ? song.title : "Unknown");
        txtArtist.setText(song.artist != null ? song.artist : "Unknown");

        updateLikeButton(isFavorite);

        txtTitle.setAlpha(0f);
        txtArtist.setAlpha(0f);
        txtTitle.animate().alpha(1f).setDuration(500).start();
        txtArtist.animate().alpha(1f).setDuration(500).setStartDelay(100).start();

        if (song.cover != null && !song.cover.isEmpty()) {
            Glide.with(activity)
                    .asBitmap()
                    .load(song.cover)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_gallery)
                    .circleCrop()
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap bitmap, Transition<? super Bitmap> transition) {
                            imgCover.setImageBitmap(bitmap);
                            int extractedColor = extractDominantColor(bitmap);
                            animateColorChange(extractedColor);
                            setBlurredBackground(bitmap);
                            onCoverLoaded.run(); // Callback
                        }

                        @Override
                        public void onLoadCleared(Drawable placeholder) {
                            imgCover.setImageDrawable(placeholder);
                        }
                    });
        } else {
            imgCover.setImageResource(android.R.drawable.ic_menu_gallery);
            imgBackground.setImageResource(android.R.color.black);
            onCoverLoaded.run(); // Callback
        }

        setLyrics("Đang tải lời bài hát...");
    }

    public void setLyrics(String text) {
        txtLyrics.setText(text);
        txtLyrics.setAlpha(0f);
        txtLyrics.animate().alpha(1f).setDuration(500).start();
    }

    public void updateLikeButton(boolean isFavorite) {
        if (isFavorite) {
            btnLike.setImageResource(android.R.drawable.btn_star_big_on);
            btnLike.setColorFilter(Color.RED);
        } else {
            btnLike.setImageResource(android.R.drawable.btn_star_big_off);
            btnLike.setColorFilter(Color.WHITE);
        }
    }

    public void updatePlayPauseButton(boolean isPlaying) {
        if (isPlaying) {
            btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
            btnPlayPause.setScaleX(0.8f);
            btnPlayPause.setScaleY(0.8f);
            btnPlayPause.animate().scaleX(1f).scaleY(1f).setDuration(300).start();
        } else {
            btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
        }
    }

    public void updateTimers(int currentMillis, int totalMillis) {
        if (currentMillis >= 0) {
            txtCurrentTime.setText(formatTime(currentMillis));
        }
        if (totalMillis >= 0) {
            txtTotalTime.setText(formatTime(totalMillis));
        }
    }

    public void updateSeekBar(int progress, int max) {
        if (max > 0) {
            seekBar.setMax(max);
        }
        if (progress >= 0) {
            seekBar.setProgress(progress);
        }
    }

    public void updateRepeatButton(int repeatMode) {
        switch (repeatMode) {
            case 0:
                btnRepeat.setColorFilter(Color.WHITE);
                break;
            case 1:
                btnRepeat.setColorFilter(dominantColor);
                break;
            case 2:
                btnRepeat.setColorFilter(Color.parseColor("#FF6B35"));
                break;
        }
    }

    public void updateShuffleButton(boolean isShuffle) {
        if (isShuffle) {
            btnShuffle.setColorFilter(dominantColor);
        } else {
            btnShuffle.setColorFilter(Color.WHITE);
        }
    }

    public void animateButton(View view) {
        view.animate()
                .scaleX(0.8f).scaleY(0.8f)
                .setDuration(100)
                .withEndAction(() ->
                        view.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                ).start();
    }

    public void setBlurredBackground(Bitmap originalBitmap) {
        try {
            int width = originalBitmap.getWidth() / 8;
            int height = originalBitmap.getHeight() / 8;
            Bitmap smallBitmap = Bitmap.createScaledBitmap(originalBitmap, width, height, false);
            Bitmap blurredBitmap = Bitmap.createBitmap(smallBitmap);

            RenderScript rs = RenderScript.create(activity);
            ScriptIntrinsicBlur blur = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
            Allocation input = Allocation.createFromBitmap(rs, smallBitmap);
            Allocation output = Allocation.createFromBitmap(rs, blurredBitmap);

            blur.setRadius(25f);
            blur.setInput(input);
            blur.forEach(output);
            output.copyTo(blurredBitmap);
            rs.destroy();

            imgBackground.setImageBitmap(blurredBitmap);
            imgBackground.setAlpha(0f);
            imgBackground.animate().alpha(0.4f).setDuration(500).start();

        } catch (Exception e) {
            Log.e(TAG, "Blur error: " + e.getMessage());
        }
    }

    public int extractDominantColor(Bitmap bitmap) {
        // ... (Logic trích xuất màu giữ nguyên) ...
        try {
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            int centerX = width / 2;
            int centerY = height / 2;
            int sampleSize = Math.min(width, height) / 4;

            long redSum = 0, greenSum = 0, blueSum = 0;
            int pixelCount = 0;

            for (int x = centerX - sampleSize/2; x < centerX + sampleSize/2; x += 5) {
                for (int y = centerY - sampleSize/2; y < centerY + sampleSize/2; y += 5) {
                    if (x >= 0 && x < width && y >= 0 && y < height) {
                        int pixel = bitmap.getPixel(x, y);
                        int brightness = (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3;

                        if (brightness > 30 && brightness < 225) {
                            redSum += Color.red(pixel);
                            greenSum += Color.green(pixel);
                            blueSum += Color.blue(pixel);
                            pixelCount++;
                        }
                    }
                }
            }

            if (pixelCount > 0) {
                int avgRed = (int)(redSum / pixelCount);
                int avgGreen = (int)(greenSum / pixelCount);
                int avgBlue = (int)(blueSum / pixelCount);

                float[] hsv = new float[3];
                Color.RGBToHSV(avgRed, avgGreen, avgBlue, hsv);
                hsv[1] = Math.min(1.0f, hsv[1] * 1.3f);
                hsv[2] = Math.min(1.0f, hsv[2] * 1.1f);

                return Color.HSVToColor(hsv);
            }
        } catch (Exception e) {
            Log.e(TAG, "Color extraction error: " + e.getMessage());
        }
        return Color.parseColor("#1DB954");
    }

    public void animateColorChange(int newColor) {
        ValueAnimator colorAnim = ValueAnimator.ofArgb(dominantColor, newColor);
        colorAnim.setDuration(800);
        colorAnim.addUpdateListener(animator -> {
            int color = (int) animator.getAnimatedValue();
            if (gradientOverlay != null) {
                gradientOverlay.setBackgroundColor(adjustAlpha(color, 0.3f));
            }
            seekBar.getProgressDrawable().setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_IN);
            seekBar.getThumb().setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_IN);
        });
        colorAnim.start();
        dominantColor = newColor;
    }

    private int adjustAlpha(int color, float factor) {
        int alpha = Math.round(Color.alpha(color) * factor);
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    public String formatTime(int millis) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    public void startDiscAnimation() {
        if (rotateAnimation == null) {
            rotateAnimation = new RotateAnimation(
                    0f, 360f,
                    Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF, 0.5f
            );
            rotateAnimation.setDuration(20000);
            rotateAnimation.setRepeatCount(Animation.INFINITE);
            rotateAnimation.setInterpolator(new LinearInterpolator());
        }
        imgCover.startAnimation(rotateAnimation);
    }

    public void stopDiscAnimation() {
        if (imgCover != null) {
            imgCover.clearAnimation();
        }
    }

}