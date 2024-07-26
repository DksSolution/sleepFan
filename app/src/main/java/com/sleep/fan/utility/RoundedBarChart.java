package com.sleep.fan.utility;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;

import com.github.mikephil.charting.animation.ChartAnimator;
import com.github.mikephil.charting.buffer.BarBuffer;
import com.github.mikephil.charting.interfaces.dataprovider.BarDataProvider;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.model.GradientColor;
import com.github.mikephil.charting.renderer.HorizontalBarChartRenderer;
import com.github.mikephil.charting.utils.Transformer;
import com.github.mikephil.charting.utils.Utils;
import com.github.mikephil.charting.utils.ViewPortHandler;

public class RoundedBarChart extends HorizontalBarChartRenderer {

    private int mRadius;

    public RoundedBarChart(BarDataProvider chart, ChartAnimator animator, ViewPortHandler viewPortHandler, int radius) {
        super(chart, animator, viewPortHandler);
        this.mRadius = radius;
    }

    public void setRadius(int radius) {
        this.mRadius = radius;
    }

    @Override
    protected void drawDataSet(Canvas c, IBarDataSet dataSet, int index) {
        Transformer trans = mChart.getTransformer(dataSet.getAxisDependency());
        mBarBorderPaint.setColor(dataSet.getBarBorderColor());
        mBarBorderPaint.setStrokeWidth(Utils.convertDpToPixel(dataSet.getBarBorderWidth()));
        mShadowPaint.setColor(dataSet.getBarShadowColor());
        boolean drawBorder = dataSet.getBarBorderWidth() > 0f;

        float phaseX = mAnimator.getPhaseX();
        float phaseY = mAnimator.getPhaseY();

        // initialize the buffer
        BarBuffer buffer = mBarBuffers[index];
        buffer.setPhases(phaseX, phaseY);
        buffer.setDataSet(index);
        buffer.setInverted(mChart.isInverted(dataSet.getAxisDependency()));
        buffer.setBarWidth(mChart.getBarData().getBarWidth());

        buffer.feed(dataSet);

        trans.pointValuesToPixel(buffer.buffer);

        boolean isSingleColor = dataSet.getColors().size() == 1;

        if (isSingleColor) {
            mRenderPaint.setColor(dataSet.getColor());
        }

        for (int j = 0; j < buffer.size(); j += 4) {
            if (!mViewPortHandler.isInBoundsLeft(buffer.buffer[j + 2])) {
                continue;
            }

            if (!mViewPortHandler.isInBoundsRight(buffer.buffer[j])) {
                break;
            }

            if (!isSingleColor) {
                mRenderPaint.setColor(dataSet.getColor(j / 4));
            }

            if (dataSet.getGradientColor() != null) {
                GradientColor gradientColor = dataSet.getGradientColor();
                mRenderPaint.setShader(new LinearGradient(
                    buffer.buffer[j],
                    buffer.buffer[j + 3],
                    buffer.buffer[j],
                    buffer.buffer[j + 1],
                    gradientColor.getStartColor(),
                    gradientColor.getEndColor(),
                    Shader.TileMode.MIRROR));
            }

            if (dataSet.getGradientColors() != null) {
                mRenderPaint.setShader(new LinearGradient(
                    buffer.buffer[j],
                    buffer.buffer[j + 3],
                    buffer.buffer[j],
                    buffer.buffer[j + 1],
                    dataSet.getGradientColor(j / 4).getStartColor(),
                    dataSet.getGradientColor(j / 4).getEndColor(),
                    Shader.TileMode.MIRROR));
            }

            RectF barRect = new RectF(buffer.buffer[j], buffer.buffer[j + 1], buffer.buffer[j + 2], buffer.buffer[j + 3]);
            Path path = roundRect(barRect, mRadius, mRadius, true, true, true, true);
            c.drawPath(path, mRenderPaint);

            if (drawBorder) {
                Path borderPath = roundRect(barRect, mRadius, mRadius, true, true, true, true);
                c.drawPath(borderPath, mBarBorderPaint);
            }
        }
    }

    private Path roundRect(RectF rect, float rx, float ry, boolean tl, boolean tr, boolean br, boolean bl) {
        float top = rect.top;
        float left = rect.left;
        float right = rect.right;
        float bottom = rect.bottom;
        Path path = new Path();
        if (rx < 0) rx = 0;
        if (ry < 0) ry = 0;
        float width = right - left;
        float height = bottom - top;
        if (rx > width / 2) rx = width / 2;
        if (ry > height / 2) ry = height / 2;
        float widthMinusCorners = (width - (2 * rx));
        float heightMinusCorners = (height - (2 * ry));

        path.moveTo(right, top + ry);
        if (tr)
            path.rQuadTo(0, -ry, -rx, -ry);//top-right corner
        else {
            path.rLineTo(0, -ry);
            path.rLineTo(-rx, 0);
        }
        path.rLineTo(-widthMinusCorners, 0);
        if (tl)
            path.rQuadTo(-rx, 0, -rx, ry); //top-left corner
        else {
            path.rLineTo(-rx, 0);
            path.rLineTo(0, ry);
        }
        path.rLineTo(0, heightMinusCorners);

        if (bl)
            path.rQuadTo(0, ry, rx, ry);//bottom-left corner
        else {
            path.rLineTo(0, ry);
            path.rLineTo(rx, 0);
        }

        path.rLineTo(widthMinusCorners, 0);
        if (br)
            path.rQuadTo(rx, 0, rx, -ry); //bottom-right corner
        else {
            path.rLineTo(rx, 0);
            path.rLineTo(0, -ry);
        }

        path.rLineTo(0, -heightMinusCorners);

        path.close();//Given close, last lineto can be removed.

        return path;
    }
}
