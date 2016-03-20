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
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.view.SurfaceHolder;

import java.lang.ref.WeakReference;
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

    /**
     * mager testing
     */
//    IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
//    Intent batteryStatus = context.registerReceiver(null, ifilter);


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
        Paint mBackgroundPaint;

        //mager testing
        Paint mTickPaint;
        Paint mHourPaint;
        Paint mMinutePaint;
        Paint mSecondPaint;
        // end testing


        Paint mHandPaint;
        boolean mAmbient;
        Time mTime;
        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mTime.clear(intent.getStringExtra("time-zone"));
                mTime.setToNow();
            }
        };
        int mTapCount;

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

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(resources.getColor(R.color.background));

            mTickPaint = new Paint();
            mTickPaint.setColor(resources.getColor(R.color.test1));

            mHourPaint= new Paint();
            mHourPaint.setColor(resources.getColor(R.color.test2));

            mMinutePaint= new Paint();
            mMinutePaint.setColor(resources.getColor(R.color.test3));

            mSecondPaint= new Paint();
            mSecondPaint.setColor(resources.getColor(R.color.test4));


            mHandPaint = new Paint();
            mHandPaint.setColor(resources.getColor(R.color.analog_hands));
            mHandPaint.setStrokeWidth(resources.getDimension(R.dimen.analog_hand_stroke));
            mHandPaint.setAntiAlias(true);
            mHandPaint.setStrokeCap(Paint.Cap.ROUND);

            mTime = new Time();
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
            if (mAmbient != inAmbientMode) {
                mAmbient = inAmbientMode;
                if (mLowBitAmbient) {
                    mHandPaint.setAntiAlias(!inAmbientMode);
                }
                invalidate();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        /**
         * Captures tap event (and tap type) and toggles the background color if the user finishes
         * a tap.
         */
        @Override
        public void onTapCommand(int tapType, int x, int y, long eventTime) {
            Resources resources = WatchFace.this.getResources();
            switch (tapType) {
                case TAP_TYPE_TOUCH:
                    // The user has started touching the screen.
                    break;
                case TAP_TYPE_TOUCH_CANCEL:
                    // The user has started a different gesture or otherwise cancelled the tap.
                    break;
                case TAP_TYPE_TAP:
                    // The user has completed the tap gesture.
                    mTapCount++;
                    mBackgroundPaint.setColor(resources.getColor(mTapCount % 2 == 0 ?
                            R.color.background : R.color.background2));
                    break;
            }
            invalidate();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            mTime.setToNow();

            int faceWidth = bounds.width();
            int faceHeight = bounds.height();
            float xCenter = faceWidth / 2.0f;
            float yCenter = faceHeight / 2.0f;

            //draw background
            canvas.drawRect(0, 0, faceWidth, faceHeight, mBackgroundPaint);

            //draw tick marks
            float hourTickWidth = 6.0f;
            float minuteTickWidth = 3.0f;
            float hourTickHeight = 35.0f;
            float minuteTickHeight = 20.0f;
            float tickOffsef = 10f;
            float tickWidth, tickHeight, angle;
            RectF r;

            for (int i = 0; i < 60; i++) {
                if (i % 5 == 0) {
                    tickWidth = hourTickWidth;
                    tickHeight = hourTickHeight;
                } else {
                    tickWidth = minuteTickWidth;
                    tickHeight = minuteTickHeight;
                }
                angle = i * 360f / 60f;
                canvas.save();
                canvas.rotate(angle, xCenter, yCenter);
                r = new RectF(xCenter - tickWidth / 2f,tickHeight,xCenter + tickWidth / 2,tickOffsef);
                canvas.drawRect(r, mTickPaint);
                canvas.restore();
            }

            // draw hours / minute / second hands
            float baseMountWitdh = 7f;
            float baseMountHole = 3f;
            float hourHandWidth = 3.0f;
            float hourHandLength = xCenter / 2f;
            float minuteHandWidth = 3.0f;
            float minuteHandLength = yCenter * 0.75f;
            float handOffsetLength = 15f;
            float secondHandWidth = 1f;
            float secondHandLength = yCenter * 0.75f;

            //calculate hours
            angle = mTime.hour / 12f * 360f + mTime.minute / 60f * 1f / 12f * 360f;

            //display hours
            canvas.save();
            canvas.rotate(angle, xCenter, yCenter);
            canvas.drawRect(xCenter - hourHandWidth, yCenter - hourHandLength, xCenter + hourHandWidth, yCenter + handOffsetLength, mMinutePaint);
            Path triangle = new Path();
            triangle.moveTo(xCenter - hourHandWidth, yCenter - hourHandLength);
            triangle.rLineTo(hourHandWidth, -2 * hourHandWidth);
            triangle.rLineTo(hourHandWidth, 2 * hourHandWidth);
            triangle.rLineTo(-2 * hourHandWidth, 0f);
            canvas.drawPath(triangle, mSecondPaint);
            canvas.drawLine(xCenter, yCenter + handOffsetLength, xCenter, yCenter - hourHandLength, mSecondPaint);
            canvas.restore();

            //calculate minutes
            angle = mTime.minute / 60f * 360f + mTime.second / 60f * 1f / 60f * 360f;

            //display minutes
            canvas.save();
            canvas.rotate(angle, xCenter, yCenter);
            canvas.drawRect(xCenter - minuteHandWidth, yCenter - minuteHandLength, xCenter + minuteHandWidth, yCenter + handOffsetLength, mMinutePaint);
            triangle = new Path();
            triangle.moveTo(xCenter - minuteHandWidth, yCenter - minuteHandLength);
            triangle.rLineTo(minuteHandWidth, -2 * minuteHandWidth);
            triangle.rLineTo(minuteHandWidth, 2 * minuteHandWidth);
            triangle.rLineTo(-2 * minuteHandWidth, 0f);
            canvas.drawPath(triangle, mSecondPaint);
            canvas.drawLine(xCenter, yCenter + handOffsetLength, xCenter, yCenter - minuteHandLength, mSecondPaint);
            canvas.restore();

            //calculate seconds
            if (!isInAmbientMode()) {
                angle = mTime.second / 60f * 360f;

                //display seconds
                canvas.save();
                canvas.rotate(angle, xCenter, yCenter);
                canvas.drawRect(xCenter - secondHandWidth, yCenter - secondHandLength, xCenter + secondHandWidth, yCenter + handOffsetLength, mMinutePaint);
                triangle = new Path();
                triangle.moveTo(xCenter - secondHandWidth, yCenter - secondHandLength);
                triangle.rLineTo(secondHandWidth, -2 * secondHandWidth);
                triangle.rLineTo(secondHandWidth, 2 * secondHandWidth);
                triangle.rLineTo(-2 * secondHandWidth, 0f);
                canvas.drawPath(triangle, mSecondPaint);
                canvas.restore();
            }

            //cork it off with a hole punched through the middle
            canvas.drawCircle(xCenter, yCenter, baseMountWitdh, mHourPaint);
            canvas.drawCircle(xCenter, yCenter, baseMountHole, mBackgroundPaint);

            //display other information
            if (!isInAmbientMode()) {

            }
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
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            WatchFace.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            WatchFace.this.unregisterReceiver(mTimeZoneReceiver);
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
