package com.sleep.fan.cutomView;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.NumberPicker;

import androidx.annotation.Nullable;
import com.sleep.fan.R;

public class CustomTimePickerView extends LinearLayout {
    private NumberPicker hourPicker;
    private NumberPicker minutePicker;

    public CustomTimePickerView(Context context) {
        super(context);
        init(context);
    }

    public CustomTimePickerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public CustomTimePickerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.custom_time_picker, this);
        hourPicker = findViewById(R.id.hourPicker);
        minutePicker = findViewById(R.id.minutePicker);

        setPickerProperties(hourPicker);
        setPickerProperties(minutePicker);
    }

    private void setPickerProperties(NumberPicker picker) {

        picker.setMinValue(0);
        picker.setMaxValue(picker == hourPicker ? 23 : 59);
        picker.setFormatter(value -> String.format("%02d", value));

    }

    public void setTime(int hour, int minute) {
        hourPicker.setValue(hour);
        minutePicker.setValue(minute);
    }

    public int getHour() {
        return hourPicker.getValue();
    }

    public int getMinute() {
        return minutePicker.getValue();
    }
}
