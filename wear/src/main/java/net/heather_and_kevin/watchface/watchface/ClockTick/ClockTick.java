package net.heather_and_kevin.watchface.watchface.ClockTick;

import android.graphics.Canvas;
import android.graphics.Paint;

import java.util.ArrayList;

/**
 * Created by kmager on 3/24/16.
 * Clock Tick will display the clock tick marks for a watch with a chin
 */
public class ClockTick {
    private float hourTickHeight;
    private float hourTickHalfWidth;
    private float minuteTickHeight;
    private float minuteTickHalfWidth;
    private Paint tickPaint;
    private ArrayList<TickMark> tickPositions;

    public ClockTick(float hourTickHeight,
                     float hourTickWidth,
                     float minuteTickHeight,
                     float minuteTickWidth,
                     Paint tickPaint) {
        this.hourTickHeight = hourTickHeight;
        this.hourTickHalfWidth = hourTickWidth / 2f;
        this.minuteTickHeight = minuteTickHeight;
        this.minuteTickHalfWidth = minuteTickWidth / 2f;
        this.tickPaint = tickPaint;
        this.tickPositions = null;
    }


    private void calculateTicks(float faceWidth, float faceHeight, float chinSize){
        float xCenter = faceWidth / 2f;
        float yCenter = faceHeight / 2f;
        float tickWidth;
        float tickHeight;
        float angle;
        double radians;
        float topHeight;
        float bottomHeight;

        for (int i = 0; i < 60; i++) {
            //calculate the angle
            angle = i * 360f / 60f;

            if (i % 5 == 0) {
                tickWidth = hourTickHalfWidth;
                tickHeight = hourTickHeight;
            } else {
                tickWidth = minuteTickHalfWidth;
                tickHeight = minuteTickHeight;
            }

            if (i < 24 || i > 36) {
                tickPositions.add(new RoundTickMark(
                        xCenter - tickWidth,
                        tickHeight,
                        xCenter + tickWidth,
                        0f,
                        angle,
                        xCenter,
                        yCenter));
            } else {
                topHeight = xCenter - chinSize;
                bottomHeight = topHeight - tickHeight;
                radians = Math.toRadians(angle);
                tickPositions.add(new ChinTickMark(
                        xCenter + (float) Math.tan(radians) * bottomHeight - tickWidth,
                        faceHeight - chinSize - tickHeight,
                        xCenter + (float) Math.tan(radians) * topHeight - tickWidth,
                        faceHeight - chinSize,
                        xCenter + (float) Math.tan(radians) * topHeight + tickWidth,
                        faceHeight - chinSize,
                        xCenter + (float) Math.tan(radians) * bottomHeight + tickWidth,
                        faceHeight - chinSize - tickHeight
                ));
            }
        }
    }

    public void drawTickMarks(float faceWidth, float faceHeight, float chinHeight, Canvas canvas) {
        if (this.tickPositions == null){
            this.tickPositions = new ArrayList<>(60);
            calculateTicks(faceWidth,faceHeight,chinHeight);
        }

        for(TickMark tickMark : this.tickPositions) {
            tickMark.draw(canvas, tickPaint);
        }
    }
}
