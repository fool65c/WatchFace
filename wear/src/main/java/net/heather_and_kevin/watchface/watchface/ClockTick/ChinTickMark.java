package net.heather_and_kevin.watchface.watchface.ClockTick;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

import java.util.Map;

/**
 * Created by kmager on 3/25/16.
 * ChinTickMark draws the marks on the chin of the watch
 */
public class ChinTickMark implements TickMark{
    private Path tickMark;

    public ChinTickMark(float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4) {
        tickMark = new Path();
        tickMark.moveTo(x1, y1);
        tickMark.lineTo(x2, y2);
        tickMark.lineTo(x3,y3);
        tickMark.lineTo(x4, y4);
    }

    @Override
    public void draw(Canvas canvas, Paint tickPaint) {
        canvas.drawPath(tickMark,tickPaint);
    }
}
