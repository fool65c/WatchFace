/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.heather_and_kevin.watchface.watchface;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.WindowInsets;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.view.SurfaceHolder;

import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Analog watch face with a ticking second hand. In ambient mode, the second hand isn't shown. On
 * devices with low-bit ambient mode, the hands are drawn without anti-aliasing in ambient mode.
 */
public class WatchFace extends CanvasWatchFaceService {
    /**
     * Update rate in milliseconds for interactive mode. We update once a second to advance the
     * second hand.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private static class EngineHandler extends Handler {
        private final WeakReference<WatchFace.Engine> mWeakReference;

        public EngineHandler(WatchFace.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            WatchFace.Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }

    private class Engine extends CanvasWatchFaceService.Engine {
        final Handler mUpdateTimeHandler = new EngineHandler(this);
        boolean mRegisteredTimeZoneReceiver = false;
        boolean mRegisteredBatteryLevelReceiver = false;

        private float angle;

        /**
         * Bitmap testing
         */
        private Bitmap mBackgroundBitmap;
        private Bitmap mBackgroundAmbientBitmap;
        private Bitmap mBackgroundScaledBitmap;


        /**
         * Tick Mark Configuration
         */
        private float hourTickHeight = 30.0f;

        /**
         * Chin size
         */
        float mChinSize;

        /**
         * Watch Hand Configuration
         */
        private ClockHand hourHand;
        private ClockHand minuteHand;
        private ClockHand secondHand;
        private float baseMountWidth = 8f;
        private float baseMountSecondWidth = 4f;
        private float baseMountHole = 2f;
        private float hourHandWidth = 10.0f;
        private float minuteHandWidth = 10.0f;
        private float handOffsetLength = 10f;
        private float secondHandWidth = 2f;
        private float hourHandLengthPercent = 1f / 2.5f;
        private float secondHandLength;
        private float handOpeningPercent = 0.35f;

        //Setting up paint colors
        Paint mBackgroundPaint;

        Paint mHandPaint;
        Paint mHandBasePaint;
        Paint mHandTipPaint;
        Paint mSecondHandPaint;
        Paint mTickPaint;
        Paint mAccessoryPaint;
        Paint mAccessoryBackgroundPaint;

        RadialGradient mBaseGradient;

        boolean mAmbient;
        Time mTime;
        Calendar calander;
        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mTime.clear(intent.getStringExtra("time-zone"));
                mTime.setToNow();
            }
        };

        float batteryPercent;
        final BroadcastReceiver mBatteryLevelReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                batteryPercent = level / (float)scale;
            }
        };

        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean mLowBitAmbient;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(WatchFace.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .setAcceptsTapEvents(true)
                    .build());

            Resources resources = WatchFace.this.getResources();

            Drawable backgroundDrawable = resources.getDrawable(R.drawable.watchface, null);
            Drawable backgroundAmbientDrawable = resources.getDrawable(R.drawable.watchfaceambient, null);
            mBackgroundBitmap = ((BitmapDrawable) backgroundDrawable).getBitmap();
            mBackgroundAmbientBitmap = ((BitmapDrawable) backgroundAmbientDrawable).getBitmap();


            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(resources.getColor(R.color.background));

            mTickPaint = new Paint();
            mTickPaint.setColor(resources.getColor(R.color.tickColor));
            mTickPaint.setAntiAlias(true);

            mHandPaint = new Paint();
            mHandPaint.setColor(resources.getColor(R.color.handColor));
            mHandPaint.setAntiAlias(true);

            mHandBasePaint= new Paint(mHandPaint);

            mHandTipPaint= new Paint();
            mHandTipPaint.setColor(resources.getColor(R.color.handTipColor));

            mSecondHandPaint = new Paint();
            mSecondHandPaint.setColor(resources.getColor(R.color.secondHandColor));
            mSecondHandPaint.setAntiAlias(true);

            mAccessoryPaint = new Paint();
            mAccessoryPaint.setColor(resources.getColor(R.color.accessoryColor));
            mAccessoryPaint.setStrokeWidth(2);
            mAccessoryPaint.setStyle(Paint.Style.STROKE);
            mAccessoryPaint.setAntiAlias(true);

            mAccessoryBackgroundPaint = new Paint();
            mAccessoryBackgroundPaint.setColor(resources.getColor(R.color.accessoryBackGroundColor));
            mAccessoryBackgroundPaint.setAntiAlias(true);

            hourHand = new ClockHand(mHandPaint, mHandTipPaint, hourHandWidth, R.color.handAccentColor);
            minuteHand = new ClockHand(mHandPaint, mHandTipPaint, minuteHandWidth, R.color.handAccentColor);
            secondHand = new ClockHand(mSecondHandPaint, mHandTipPaint, secondHandWidth, handOffsetLength * 2f);

            mTime = new Time();
            calander = Calendar.getInstance();
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);
            mChinSize = insets.getSystemWindowInsetBottom();
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            Resources resources = WatchFace.this.getResources();
            if (mAmbient != inAmbientMode) {
                mAmbient = inAmbientMode;
                invalidate();
            }

            if (mAmbient && mBackgroundScaledBitmap != null) {
                mBackgroundScaledBitmap = Bitmap.createScaledBitmap(mBackgroundAmbientBitmap,
                        mBackgroundScaledBitmap.getWidth(), mBackgroundScaledBitmap.getHeight(), true /* filter */);
            } else {
                mBackgroundScaledBitmap = Bitmap.createScaledBitmap(mBackgroundBitmap,
                        mBackgroundScaledBitmap.getWidth(), mBackgroundScaledBitmap.getHeight(), true /* filter */);
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        @Override
        public void onSurfaceChanged(
                SurfaceHolder holder, int format, int width, int height) {
            if (mBackgroundScaledBitmap == null
                    || mBackgroundScaledBitmap.getWidth() != width
                    || mBackgroundScaledBitmap.getHeight() != height) {
                mBackgroundScaledBitmap = Bitmap.createScaledBitmap(mBackgroundBitmap,
                        width, height, true /* filter */);
            }
            super.onSurfaceChanged(holder, format, width, height);
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            mTime.setToNow();

            int faceWidth = bounds.width();
            int faceHeight = bounds.height();
            float xCenter = faceWidth / 2.0f;
            float yCenter = faceHeight / 2.0f;
            double handLength;

            //draw background
//            canvas.drawRect(0, 0, faceWidth, faceHeight, mBackgroundPaint);
            canvas.drawBitmap(mBackgroundScaledBitmap, 0, 0, null);

            //display other information
            if (!isInAmbientMode()) {
                canvas.drawText(Float.toString(faceHeight),xCenter+20,yCenter,mSecondHandPaint);
                float xBatteryCircleCenter = xCenter;
                float batteryOffset = (yCenter - hourTickHeight + baseMountWidth) /2f;
                float yBatteryCircleCenter = yCenter;
                float accessoryCircleSize = 42f;
//                canvas.drawCircle(xBatteryCircleCenter,yBatteryCircleCenter - batteryOffset,accessoryCircleSize,mAccessoryPaint);
                canvas.drawCircle(xBatteryCircleCenter,yBatteryCircleCenter -1f - batteryOffset,accessoryCircleSize-2,mAccessoryBackgroundPaint);

//                canvas.drawCircle(xBatteryCircleCenter + batteryOffset,yBatteryCircleCenter,accessoryCircleSize,mAccessoryPaint);
                canvas.drawCircle(xBatteryCircleCenter +1f + batteryOffset,yBatteryCircleCenter,accessoryCircleSize-2,mAccessoryBackgroundPaint);
//
//                canvas.drawCircle(xBatteryCircleCenter - batteryOffset, yBatteryCircleCenter, accessoryCircleSize, mAccessoryPaint);
                canvas.drawCircle(xBatteryCircleCenter - 1f - batteryOffset, yBatteryCircleCenter, accessoryCircleSize - 2, mAccessoryBackgroundPaint);
                Path mArc = new Path();

//                mArc.addCircle(xBatteryCircleCenter - batteryOffset,
//                        yBatteryCircleCenter,
//                        accessoryCircleSize,
//                        Path.Direction.CW);

//                canvas.drawPath(mArc, mHandTipPaint);

//                canvas.drawRect(oval,mHandTipPaint);

//                canvas.drawPath(mArc,mHandTipPaint);

//                canvas.drawTextOnPath(Integer.toString(calander.DAY_OF_MONTH),mArc,0,10f,mHandTipPaint);


            }

            // draw hours / minute / second hands
            //calculate hours
            angle = mTime.hour / 12f * 360f + mTime.minute / 60f * 1f / 12f * 360f;
            //display hours
            hourHand.setHandLength(yCenter * hourHandLengthPercent);
//            hourHand.drawHand(canvas, xCenter, yCenter, angle);

            //calculate minutes
            angle = mTime.minute / 60f * 360f + mTime.second / 60f * 1f / 60f * 360f;
            //display minutes
            if (mTime.minute < 23 || mTime.minute > 35) {
                minuteHand.setHandLength(yCenter - handOffsetLength - hourTickHeight);
            } else {
                handLength = Math.toRadians(angle);
                minuteHand.setHandLength((float)((yCenter - mChinSize ) / -Math.cos(handLength)) - handOffsetLength - hourTickHeight);
            }
//            minuteHand.drawHand(canvas,xCenter,yCenter,angle);

            //draw hour hand base
//            canvas.drawCircle(xCenter, yCenter, baseMountWidth, mHandBasePaint);

            //calculate seconds
            if (!isInAmbientMode()) {
                angle = mTime.second / 60f * 360f;

                if (mTime.second < 24 || mTime.second > 36) {
                    secondHandLength = yCenter - handOffsetLength;
                } else {
                    handLength = Math.toRadians(angle);
                    secondHandLength = (float)((yCenter - mChinSize ) / -Math.cos(handLength)) - handOffsetLength;
                }

                //display seconds
                secondHand.setHandLength(secondHandLength);
                secondHand.drawHand(canvas, xCenter, yCenter, angle);

                //draw second hand base
//                canvas.drawCircle(xCenter, yCenter, baseMountSecondWidth, mSecondHandPaint);
            }

            //cork it off with a hole punched through the middle
//            canvas.drawCircle(xCenter, yCenter, baseMountHole, mBackgroundPaint);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();

                // Update time zone in case it changed while we weren't visible.
                mTime.clear(TimeZone.getDefault().getID());
                mTime.setToNow();
            } else {
                unregisterReceiver();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        private void registerReceiver() {

            if (!mRegisteredTimeZoneReceiver) {
                mRegisteredTimeZoneReceiver = true;
                IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
                WatchFace.this.registerReceiver(mTimeZoneReceiver, filter);
            }

            if (!mRegisteredBatteryLevelReceiver) {
                mRegisteredBatteryLevelReceiver = true;
                IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
                WatchFace.this.registerReceiver(mBatteryLevelReceiver, filter);
            }
        }

        private void unregisterReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                mRegisteredTimeZoneReceiver = false;
                WatchFace.this.unregisterReceiver(mTimeZoneReceiver);
            }

            if (mRegisteredBatteryLevelReceiver) {
                mRegisteredBatteryLevelReceiver = false;
                WatchFace.this.unregisterReceiver(mBatteryLevelReceiver);
            }

        }



        /**
         * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS
                        - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }
    }
}
