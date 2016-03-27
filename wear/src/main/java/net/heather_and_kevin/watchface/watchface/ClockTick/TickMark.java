package net.heather_and_kevin.watchface.watchface.ClockTick;

import android.graphics.Canvas;
import android.graphics.Paint;

/**
 * Created by kmager on 3/25/16.
 * Interface for different types of tick marks
 */
public interface TickMark {
    void draw(Canvas canvas, Paint tickPaint);
}
