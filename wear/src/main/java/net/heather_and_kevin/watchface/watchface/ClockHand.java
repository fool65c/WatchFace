package net.heather_and_kevin.watchface.watchface;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;

/**
 * Created by kmager on 3/20/16.
 */
public class ClockHand {
    private Paint handPaint;
    private Paint gradientHandPaint;
    private Paint handPaintOpening;
    private Paint handTipPaint;
    private float handWidth;
    private float halfWidth;
    private float handLength;
    private float handOffSetLength;
    private float borderWidth;
    private int gradientColor;
    private float top;

    public ClockHand(Paint paint, Paint handTipPaint, float handWidth, int gradientColor) {
        this.gradientColor = gradientColor;
        init(paint, handTipPaint, handWidth, handOffSetLength);
    }
    public ClockHand(Paint paint, Paint handTipPaint, float handWidth, float handOffSetLength){
        init(paint, handTipPaint, handWidth, handOffSetLength);
    }
    private void init(Paint paint, Paint handTipPaint, float handWidth, float handOffSetLength){
        this.handPaint = paint;
        this.gradientHandPaint = new Paint(paint);
        this.handTipPaint = handTipPaint;
        this.borderWidth = 2f;

        this.handWidth = handWidth;
        this.halfWidth = handWidth / 2f;

        this.handOffSetLength = handOffSetLength;

        this.gradientHandPaint.setShader(new LinearGradient(0,
                0 ,
                this.halfWidth / 2f,
                0,
                this.gradientColor, this.handPaint.getColor(),
                Shader.TileMode.REPEAT));

        this.handPaintOpening = new Paint(handTipPaint);
        this.handPaintOpening.setStyle(Paint.Style.STROKE);
        this.handPaintOpening.setStrokeWidth(this.borderWidth);
    }

    public void setHandLength(float handLength) {
        this.handLength = handLength;
    }

    public void drawHand(Canvas canvas, float xCenter, float yCenter, float angle) {
        canvas.save();
        canvas.rotate(angle, xCenter, yCenter);

            canvas.drawRect(xCenter - this.halfWidth,
                    yCenter - this.handLength,
                    xCenter + this.halfWidth,
                    yCenter + this.handOffSetLength,
                    this.handPaint);

//
//
//        canvas.drawRect(xCenter - this.halfWidth + this.borderWidth / 2,
//                yCenter - this.handLength,
//                xCenter + this.halfWidth - this.borderWidth / 2,
//                yCenter + this.handOffSetLength,
//                this.handPaintOpening);

//        Path triangle = new Path();
//        triangle.moveTo(xCenter - this.halfWidth, yCenter - this.handLength);
//        triangle.rLineTo(this.halfWidth, - this.handWidth);
//        triangle.rLineTo(this.halfWidth, this.handWidth);
//        canvas.drawPath(triangle, this.handTipPaint);
        canvas.restore();
    }
}
