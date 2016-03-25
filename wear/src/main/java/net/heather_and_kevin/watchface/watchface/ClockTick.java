package net.heather_and_kevin.watchface.watchface;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by kmager on 3/24/16.
 */
public class ClockTick {
    private float hourTickHeight;
    private float hourTickHalfWidth;
    private float minuteTickHeight;
    private float minuteTickHalfWidth;
    private Paint tickPaint;
    private ArrayList<Map<String,Float>> tickPositions;

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
        double angle;
        double radians;
        float len;
        float width;
        float topHeight;
        float bottomHeight;

        // save ticks
        topHeight = xCenter - chinSize;
        for (int i = -7; i < 8; i++) {
            Map<String,Float> tickLocations = new HashMap<>();
            angle = i * 360f / 60f;
            radians = Math.toRadians(angle);
            if (i % 5 == 0) {
                len = hourTickHeight;
                width = hourTickHalfWidth;
            } else {
                len = minuteTickHeight;
                width = minuteTickHalfWidth;
            }

            bottomHeight = topHeight - len;

            tickLocations.put("x1", xCenter + (float) (Math.tan(radians) * bottomHeight) - width);
            tickLocations.put("y1", chinSize + len);
            tickLocations.put("x2", xCenter + (float) (Math.tan(radians) * topHeight) - width);
            tickLocations.put("y2", chinSize);
            tickLocations.put("x3", xCenter + (float) (Math.tan(radians) * topHeight) + width);
            tickLocations.put("y3", chinSize);
            tickLocations.put("x4", xCenter + (float) (Math.tan(radians) * bottomHeight) + width);
            tickLocations.put("y4", chinSize + len);
            this.tickPositions.add(tickLocations);
        }

        topHeight = yCenter - chinSize;
        for (int i = 8; i < 23; i++) {
            Map<String,Float> tickLocations = new HashMap<>();
            angle = i * 360f / 60f -90f;
            radians = Math.toRadians(angle);

            if (i % 5 == 0) {
                len = hourTickHeight;
                width = hourTickHalfWidth;
            } else {
                len = minuteTickHeight;
                width = minuteTickHalfWidth;
            }

            bottomHeight = topHeight - len;

            tickLocations.put("x1", faceWidth - chinSize - len);
            tickLocations.put("y1", yCenter + (float)Math.tan(radians)*bottomHeight +- width);
            tickLocations.put("x2", faceWidth - chinSize);
            tickLocations.put("y2", yCenter + (float) Math.tan(radians) * topHeight - width);
            tickLocations.put("x3", faceWidth - chinSize);
            tickLocations.put("y3", yCenter + (float) Math.tan(radians) * topHeight + width);
            tickLocations.put("x4", faceWidth - chinSize - len);
            tickLocations.put("y4", yCenter + (float)Math.tan(radians)*bottomHeight + width);
            this.tickPositions.add(tickLocations);
        }

        topHeight = xCenter - chinSize;
        for (int i = 23; i < 38; i++) {
            Map<String,Float> tickLocations = new HashMap<>();
            angle = i * 360f / 60f;
            radians = Math.toRadians(angle);

            if (i % 5 == 0) {
                len = hourTickHeight;
                width = hourTickHalfWidth;
            } else {
                len = minuteTickHeight;
                width = minuteTickHalfWidth;
            }

            bottomHeight = topHeight - len;

            tickLocations.put("x1", xCenter + (float) Math.tan(radians) * bottomHeight - width);
            tickLocations.put("y1", faceHeight - chinSize - len);
            tickLocations.put("x2", xCenter + (float)Math.tan(radians)*topHeight - width);
            tickLocations.put("y2", faceHeight - chinSize);
            tickLocations.put("x3", xCenter + (float)Math.tan(radians)*topHeight + width);
            tickLocations.put("y3", faceHeight - chinSize);
            tickLocations.put("x4", xCenter + (float) Math.tan(radians) * bottomHeight + width);
            tickLocations.put("y4", faceHeight - chinSize - len);
            this.tickPositions.add(tickLocations);
        }

        topHeight = yCenter - chinSize;
        for (int i = 38; i < 53; i++) {
            Map<String,Float> tickLocations = new HashMap<>();
            angle = i * 360f / 60f -90f;
            radians = Math.toRadians(angle);

            if (i % 5 == 0) {
                len = hourTickHeight;
                width = hourTickHalfWidth;
            } else {
                len = minuteTickHeight;
                width = minuteTickHalfWidth;
            }

            bottomHeight = topHeight - len;

            tickLocations.put("x1", chinSize + len);
            tickLocations.put("y1", yCenter + (float)Math.tan(radians)*bottomHeight - width);
            tickLocations.put("x2", chinSize);
            tickLocations.put("y2", yCenter + (float)Math.tan(radians)*topHeight - width);
            tickLocations.put("x3", chinSize);
            tickLocations.put("y3", yCenter + (float)Math.tan(radians)*topHeight + width);
            tickLocations.put("x4", chinSize + len);
            tickLocations.put("y4", yCenter + (float)Math.tan(radians)*bottomHeight + width);
            this.tickPositions.add(tickLocations);
        }
    }

    public void drawTicks(float faceWidth, float faceHeight, float chinHeight, Canvas canvas) {
        if (this.tickPositions == null){
            this.tickPositions = new ArrayList<>(60);
            calculateTicks(faceWidth,faceHeight,chinHeight);
        }

        for (Map<String,Float> ticks : this.tickPositions) {
            Path tickMark = new Path();
            tickMark.moveTo(ticks.get("x1"),ticks.get("y1"));
            tickMark.lineTo(ticks.get("x2"), ticks.get("y2"));
            tickMark.lineTo(ticks.get("x3"),ticks.get("y3"));
            tickMark.lineTo(ticks.get("x4"),ticks.get("y4"));
//            canvas.drawLine(ticks.get("x1"),
//                    ticks.get("y1"),
//                    ticks.get("x2"),
//                    ticks.get("y2"),
//                    this.tickPaint);
            canvas.drawPath(tickMark,tickPaint);
        }
    }


}
