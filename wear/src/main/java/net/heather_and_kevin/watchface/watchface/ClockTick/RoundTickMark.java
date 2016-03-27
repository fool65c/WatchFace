package net.heather_and_kevin.watchface.watchface.ClockTick;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

/**
 * Created by kmager on 3/25/16.
 * Draws the marks on the round part of the watch face
 */
public class RoundTickMark implements TickMark {
    private RectF rectangle;
    private float angle;
    private float xCenter;
    private float yCenter;

    public RoundTickMark(float x1, float y1, float x2, float y2, float angle, float xCenter, float yCenter) {
        rectangle = new RectF(x1,y1,x2,y2);
        this.angle = angle;
        this.xCenter = xCenter;
        this.yCenter = yCenter;

    }

    @Override
    public void draw(Canvas canvas, Paint tickPaint) {
        canvas.save();
        canvas.rotate(angle, xCenter, yCenter);
        canvas.drawRect(rectangle, tickPaint);
        canvas.restore();
    }
}
