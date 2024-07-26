package com.sleep.fan;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.DisplayMetrics;
import android.view.View;

public class GameView extends View
{
	private Bitmap fanblade=null;
	private Bitmap fanFront=null;
	private Bitmap fanBack=null;

	private float degrees=0.0f;
	private float xBack=0;
	private float yBack=0;

	private Matrix rotate=null;
	private Paint paint=null;

	int fanType;

	public GameView(Context context, int fanType)
	{
		super(context);

		degrees=0;
		this.fanType = fanType;

		if(rotate==null)
		{
			rotate = new Matrix();
		}


		switch (this.fanType)
		{
			case 1:
				fanBack=BitmapFactory.decodeResource(context.getResources(), R.drawable.fan_back1);
				fanblade=BitmapFactory.decodeResource(context.getResources(), R.drawable.fan_blade1);
				fanFront=BitmapFactory.decodeResource(context.getResources(), R.drawable.fan_front1);
				break;
			case 2:
				fanBack=BitmapFactory.decodeResource(context.getResources(), R.drawable.fan_back2);
				fanblade=BitmapFactory.decodeResource(context.getResources(), R.drawable.fan_blade2);
				fanFront=BitmapFactory.decodeResource(context.getResources(), R.drawable.fan_front2);
				break;
			case 3:
				fanBack=BitmapFactory.decodeResource(context.getResources(), R.drawable.fan_back3);
				fanblade=BitmapFactory.decodeResource(context.getResources(), R.drawable.fan_blade3);
				fanFront=BitmapFactory.decodeResource(context.getResources(), R.drawable.fan_front3);
				break;
			case 4:
				fanBack=BitmapFactory.decodeResource(context.getResources(), R.drawable.fan_back4);
				fanblade=BitmapFactory.decodeResource(context.getResources(), R.drawable.fan_blade4);
				fanFront=BitmapFactory.decodeResource(context.getResources(), R.drawable.fan_front4);
				break;
		}

		DisplayMetrics metrics = getResources().getDisplayMetrics();
		ActivityMenu.screenWidth = metrics.widthPixels;
		ActivityMenu.screenHeight = metrics.heightPixels;

		xBack=ActivityMenu.screenWidth/5.5f - fanBack.getWidth()/5.5f;
		yBack=ActivityMenu.screenHeight * 0.35f -fanBack.getHeight();

		// TODO Auto-generated constructor stub
	}
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredWidth = 0;
        int desiredHeight = 0;

        // Calculate the width and height based on the size of fanBack, fanblade, and fanFront
        // Here we assume that the images are centered and may rotate around their center
        if (fanBack != null) {
            desiredWidth = Math.max(desiredWidth, fanBack.getWidth());
            desiredHeight = Math.max(desiredHeight, fanBack.getHeight());
        }
        if (fanblade != null) {
            desiredWidth = Math.max(desiredWidth, fanblade.getWidth());
            desiredHeight = Math.max(desiredHeight, fanblade.getHeight());
        }
        if (fanFront != null) {
            desiredWidth = Math.max(desiredWidth, fanFront.getWidth());
            desiredHeight = Math.max(desiredHeight, fanFront.getHeight());
        }

        // Account for padding
        desiredWidth += getPaddingLeft() + getPaddingRight();
        desiredHeight += getPaddingTop() + getPaddingBottom();

        // Respect AT_MOST measurement specs
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int width;
        int height;

        // Measure Width
        if (widthMode == MeasureSpec.EXACTLY) {
            // Must be this size
            width = widthSize;
        } else if (widthMode == MeasureSpec.AT_MOST) {
            // Can't be bigger than...
            width = Math.min(desiredWidth, widthSize);
        } else {
            // Be whatever you want
            width = desiredWidth;
        }

        // Measure Height
        if (heightMode == MeasureSpec.EXACTLY) {
            height = heightSize;
        } else if (heightMode == MeasureSpec.AT_MOST) {
            height = Math.min(desiredHeight, heightSize);
        } else {
            height = desiredHeight;
        }

        // MUST call this to store the measurements
        setMeasuredDimension(width + 100, height + 100);
    }
	@Override
	protected void onDraw(Canvas canvas)
	{
		// TODO Auto-generated method stub
		super.onDraw(canvas);


		if(paint==null)
		{
			paint=new Paint();
			paint.setAntiAlias(true);
			paint.setFilterBitmap(true);
			paint.setDither(true);
		}

		if(rotate!=null)
		{
			rotate=null;
			rotate=new Matrix();
		}

		switch (fanType)
		{
		case 1:
			canvas.drawBitmap(fanBack, xBack,yBack, null);
			rotate.postRotate(degrees, fanblade.getWidth()/2f, fanblade.getHeight()/2f);
			rotate.postTranslate(ActivityMenu.screenWidth/5.5f - fanblade.getWidth()/5.5f, ActivityMenu.screenHeight * 0.35f -fanblade.getHeight());
			canvas.drawBitmap(fanblade, rotate, null);
			canvas.drawBitmap(fanFront, xBack,yBack, null);
			break;

		case 2:
			canvas.drawBitmap(fanBack, xBack,yBack, null);
			rotate.postRotate(degrees, fanblade.getWidth()/2f, fanblade.getHeight()/2f);
			rotate.postTranslate(ActivityMenu.screenWidth/5.5f - fanblade.getWidth()/5.5f, ActivityMenu.screenHeight * 0.35f -fanblade.getHeight());
			canvas.drawBitmap(fanblade, rotate, null);
			canvas.drawBitmap(fanFront, xBack,yBack, null);
			break;

		case 3:
			canvas.drawBitmap(fanBack, xBack,yBack, null);
			rotate.postRotate(degrees, fanblade.getWidth()/2f, fanblade.getHeight()/2f);
			rotate.postTranslate(ActivityMenu.screenWidth/5.5f - fanblade.getWidth()/5.5f, ActivityMenu.screenHeight * 0.35f -fanblade.getHeight());
			canvas.drawBitmap(fanblade, rotate, null);
			canvas.drawBitmap(fanFront, xBack,yBack, null);
			break;

		case 4:
			canvas.drawBitmap(fanBack, xBack,yBack, null);
			canvas.drawBitmap(fanFront, xBack,yBack, null);
			rotate.postRotate(degrees, fanblade.getWidth()/2f, fanblade.getHeight()/2f);
			rotate.postTranslate(ActivityMenu.screenWidth/5.5f - fanblade.getWidth()/5.5f, ActivityMenu.screenHeight * 0.35f -fanblade.getHeight());
			canvas.drawBitmap(fanblade, rotate, null);
			break;
		}

		if(ActivityFan.isTimerSet)
			setFanSpeed(fanType);

		invalidate();


	}

	public void setFanSpeed(int fanType) {
		switch(fanType){
			case 1:
				setFanOneSpeed();
				break;
			case 2:
				setFanTwoSpeed();
				break;
			case 3:
				setFanThreeSpeed();
				break;
			case 4:
				setFanFourSpeed();
				break;
		}
	}

	private void setFanOneSpeed() {

		if(!ActivityFan.isFanPlaying){
			degrees = 0;
		}else if(!ActivityFan.isSpinEnabled){
			degrees = 0;
		} else
		{
			if (ActivityFan.onTouchFastBtn && !ActivityFan.onTouchMediumBtn && !ActivityFan.onTouchSlowBtn) {
				degrees += 20.0f;
			} else if (ActivityFan.onTouchMediumBtn && !ActivityFan.onTouchFastBtn && !ActivityFan.onTouchSlowBtn) {
				degrees += 18.0f;
			} else if (ActivityFan.onTouchSlowBtn && !ActivityFan.onTouchMediumBtn && !ActivityFan.onTouchFastBtn) {
				degrees += 15.0f;
			} else {
				degrees += 20.0f;
			}
		}

	}

	private void setFanTwoSpeed() {

		if(!ActivityFan.isFanPlaying){
			degrees = 0;
		}else if(!ActivityFan.isSpinEnabled){
			degrees = 0;
		}else {
			if (ActivityFan.onTouchFastBtn && !ActivityFan.onTouchMediumBtn && !ActivityFan.onTouchSlowBtn) {
				degrees += 20.0f;
			} else if (ActivityFan.onTouchMediumBtn && !ActivityFan.onTouchFastBtn && !ActivityFan.onTouchSlowBtn) {
				degrees += 16.0f;
			} else if (ActivityFan.onTouchSlowBtn && !ActivityFan.onTouchMediumBtn && !ActivityFan.onTouchFastBtn) {
				degrees += 12.0f;
			} else {
				degrees += 20.0f;
			}
		}

	}

	private void setFanThreeSpeed() {

		if(!ActivityFan.isFanPlaying){
			degrees = 0;
		}else if(!ActivityFan.isSpinEnabled){
			degrees = 0;
		}else {
			if (ActivityFan.onTouchFastBtn && !ActivityFan.onTouchMediumBtn && !ActivityFan.onTouchSlowBtn) {
				degrees += 20.0f;
			} else if (ActivityFan.onTouchMediumBtn && !ActivityFan.onTouchFastBtn && !ActivityFan.onTouchSlowBtn) {
				degrees += 16.0f;
			} else if (ActivityFan.onTouchSlowBtn && !ActivityFan.onTouchMediumBtn && !ActivityFan.onTouchFastBtn) {
				degrees += 12.0f;
			} else {
				degrees += 20.0f;
			}
		}

	}

	private void setFanFourSpeed() {

		if(!ActivityFan.isFanPlaying){
			degrees = 0;
		}else if(!ActivityFan.isSpinEnabled){
			degrees = 0;
		}else {
			if (ActivityFan.onTouchFastBtn && !ActivityFan.onTouchMediumBtn && !ActivityFan.onTouchSlowBtn) {
				degrees += 20.0f;
			} else if (ActivityFan.onTouchMediumBtn && !ActivityFan.onTouchFastBtn && !ActivityFan.onTouchSlowBtn) {
				degrees += 16.0f;
			} else if (ActivityFan.onTouchSlowBtn && !ActivityFan.onTouchMediumBtn && !ActivityFan.onTouchFastBtn) {
				degrees += 12.0f;
			} else {
				degrees += 20.0f;
			}
		}

	}

	public void RecycleImage()
	{
		if(fanblade!=null)
		{
			fanblade.recycle();
			fanblade=null;
		}

		if(fanBack!=null)
		{
			fanBack.recycle();
			fanBack=null;
		}

		if(fanFront!=null)
		{
			fanFront.recycle();
			fanFront=null;
		}
	}

}
