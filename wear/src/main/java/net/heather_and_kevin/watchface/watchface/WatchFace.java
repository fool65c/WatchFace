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
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
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
         * Tick Mark Configuration
         */
        private float hourTickWidth = 5.0f;
        private float minuteTickWidth = 2.5f;
        private float hourTickHeight = 30.0f;
        private float minuteTickHeight = 20.0f;
        private float tickOffset = 0f;//10f;
        private float tickWidth, tickHeight;
        private RectF tickRectangle;

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
        private ClockTick clockTicks;
        private float baseMountWidth = 12f;
        private float baseMountSecondWidth = 6f;
        private float baseMountHole = 3f;
        private float hourHandWidth = 10.0f;
        private float minuteHandWidth = 10.0f;
        private float handOffsetLength = 10f;
        private float secondHandWidth = 2f;
        private float hourHandLengthPercent = 0.5f;
        private float minuteHandLengthPercent = 0.75f;
        private float secondHandLengthPercent = 1f;//0.75f;
        private float handOpeningPercent = 0.35f;

        //Setting up paint colors
        Paint mBackgroundPaint;

        Paint mHandPaint;
        Paint mHandBasePaint;
        Paint mHandTipPaint;
        Paint mSecondHandPaint;
        Paint mTickPaint;
        Paint mTestPaint;

        RadialGradient mBaseGradient;

        boolean mAmbient;
        Time mTime;
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
            mTickPaint.setColor(resources.getColor(R.color.tickColor));

            mTestPaint = new Paint();
            mTestPaint.setColor(resources.getColor(R.color.test));

            mHandPaint= new Paint();
            mHandPaint.setColor(resources.getColor(R.color.handColor));

            mHandBasePaint= new Paint(mHandPaint);

            mHandTipPaint= new Paint();
            mHandTipPaint.setColor(resources.getColor(R.color.handTipColor));

            mSecondHandPaint= new Paint();
            mSecondHandPaint.setColor(resources.getColor(R.color.secondHandColor));

            hourHand = new ClockHand(mHandPaint, mHandTipPaint, hourHandWidth, handOffsetLength, R.color.handAccentColor);
            minuteHand = new ClockHand(mHandPaint, mHandTipPaint, minuteHandWidth,handOffsetLength, R.color.handAccentColor);
            secondHand = new ClockHand(mSecondHandPaint, mHandTipPaint, secondHandWidth, handOffsetLength * 2);

            clockTicks = new ClockTick(hourTickHeight,hourHandWidth,minuteTickHeight,minuteTickWidth,mTestPaint);
            mTime = new Time();
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

            // define gradient for base
            if (mBaseGradient == null) {
                mBaseGradient = new RadialGradient(xCenter, yCenter, baseMountWidth * 1.25f,
                        R.color.handAccentColor, mHandBasePaint.getColor(),
                        android.graphics.Shader.TileMode.CLAMP);
                mHandBasePaint.setDither(true);
                mHandBasePaint.setShader(mBaseGradient);
            }

            //draw tick marks
//            drawTickMarks(canvas, xCenter, yCenter);

            // draw hours / minute / second hands
//            hourHand.setHandLength(xCenter * hourHandLengthPercent);

            //calculate hours
//            angle = mTime.hour / 12f * 360f + mTime.minute / 60f * 1f / 12f * 360f;
            //display hours
//            hourHand.drawHand(canvas, xCenter, yCenter, angle, !isInAmbientMode());

            //calculate minutes
//            angle = mTime.minute / 60f * 360f + mTime.second / 60f * 1f / 60f * 360f;
            //display minutes
//            minuteHand.setHandLength(yCenter * minuteHandLengthPercent);
//            minuteHand.drawHand(canvas,xCenter,yCenter,angle,!isInAmbientMode());

            //draw hour hand base
//            canvas.drawCircle(xCenter, yCenter, baseMountWidth, mHandBasePaint);

            //calculate seconds
//            if (!isInAmbientMode()) {
//                angle = mTime.second / 60f * 360f;
//
//                //display seconds
//                secondHand.setHandLength(yCenter * secondHandLengthPercent);
//                secondHand.drawHand(canvas,xCenter,yCenter,angle,false);
//
//                //draw second hand base
//                canvas.drawCircle(xCenter, yCenter, baseMountSecondWidth, mSecondHandPaint);
//            }

            //cork it off with a hole punched through the middle
//            canvas.drawCircle(xCenter, yCenter, baseMountHole, mBackgroundPaint);

            //display other information
//            if (!isInAmbientMode()) {
//                canvas.drawText(Float.toString(batteryPercent),xCenter+20,yCenter,mSecondHandPaint);
//                canvas.drawText(Float.toString(mChinSize),xCenter-30,yCenter,mSecondHandPaint);
//            }


            //Testing
            canvas.drawRect(mChinSize,
                    mChinSize,
                    faceWidth-mChinSize,
                    faceHeight-mChinSize,
                    mHandBasePaint);

//            drawSquareTickes(canvas, faceWidth, faceHeight);
            clockTicks.drawTicks(faceWidth,faceHeight,mChinSize,canvas);

            angle = mTime.second / 60f * 360f;

            //display seconds
            secondHand.setHandLength(yCenter * secondHandLengthPercent);
            secondHand.drawHand(canvas, xCenter, yCenter, angle, false);

            //draw second hand base
            canvas.drawCircle(xCenter, yCenter, baseMountSecondWidth, mSecondHandPaint);



        }

        /**
         * draws the tick marks for a watch face
         * @param canvas
         * @param xCenter
         * @param yCenter
         */
        private void drawTickMarks(Canvas canvas, float xCenter, float yCenter) {
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
                tickRectangle = new RectF(xCenter - tickWidth / 2f,tickHeight + mChinSize,xCenter + tickWidth / 2,tickOffset + mChinSize);
                canvas.drawRect(tickRectangle, mTickPaint);
                canvas.restore();
            }
        }


        private void drawSquareTickes(Canvas canvas, float faceWidth, float faceHeight){

            float xCenter = faceWidth / 2f;
            float yCenter = faceHeight / 2f;
            double sinVal = 0, cosVal = 0, angle = 0;
            float topHeight = 0, bottomHeight = 0;
            float x1 = 0, y1 = 0, x2 = 0, y2 = 0;

            int count = 0;

            // draw ticks
            topHeight = xCenter - mChinSize;
            for (int i = -8; i < 8; i++) {
                angle = i * 360f / 60f;
                double radians = Math.toRadians(angle);
                float len = (i % 5 == 0) ? hourTickHeight :minuteTickHeight;
                bottomHeight = topHeight - len;

                x1 = (float)Math.tan(radians)*bottomHeight;
                x2 = (float)Math.tan(radians)*topHeight;

                canvas.drawLine(xCenter, mChinSize + len, xCenter + x2,
                        mChinSize, mTestPaint);

                count++;
            }

            topHeight = yCenter - mChinSize;
            for (int i = 8; i < 28; i++) {
                angle = i * 360f / 60f -90f;
                double radians = Math.toRadians(angle);
                float len = (i % 5 == 0) ? hourTickHeight :minuteTickHeight;
                bottomHeight = topHeight - len;

                y1 = (float)Math.tan(radians)*bottomHeight;
                y2 = (float)Math.tan(radians)*topHeight;

                canvas.drawLine(faceWidth - mChinSize - len, yCenter + y1, faceWidth - mChinSize,
                        yCenter + y2, mTestPaint);

                count++;
            }

            topHeight = xCenter - mChinSize;
            for (int i = 23; i < 38; i++) {
                angle = i * 360f / 60f;
                double radians = Math.toRadians(angle);
                float len = (i % 5 == 0) ? hourTickHeight :minuteTickHeight;
                bottomHeight = topHeight - len;

                x1 = (float)Math.tan(radians)*bottomHeight;
                x2 = (float)Math.tan(radians)*topHeight;

                canvas.drawLine(xCenter + x1, faceHeight - mChinSize - len, xCenter + x2,
                        faceHeight - mChinSize, mTestPaint);

                count++;
            }

            topHeight = yCenter - mChinSize;
            for (int i = 38; i < 53; i++) {
                angle = i * 360f / 60f -90f;
                double radians = Math.toRadians(angle);
                float len = (i % 5 == 0) ? hourTickHeight : minuteTickHeight;
                bottomHeight = topHeight - len;

                y1 = (float)Math.tan(radians)*bottomHeight;
                y2 = (float)Math.tan(radians)*topHeight;

                canvas.drawLine(mChinSize + len, yCenter + y1, mChinSize,
                        yCenter + y2, mTestPaint);

                count++;
            }

            canvas.drawText(Integer.toString(count),xCenter-30,yCenter,mTestPaint);
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
