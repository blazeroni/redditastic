package com.blazeroni.reddit.view;

import java.lang.reflect.Method;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;

import com.blazeroni.reddit.util.Log;

/**
 * A modified Spinner that doesn't automatically select the first entry in the
 * list.
 *
 * Shows the prompt if nothing is selected.
 *
 * Limitations: does not display prompt if the entry list is empty.
 */
public class NoDefaultSpinner extends Spinner {
    private Method method1;
    private Method method2;

    public NoDefaultSpinner(Context context) {
        super(context);
    }

    public NoDefaultSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NoDefaultSpinner(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void setAdapter(SpinnerAdapter adapter) {
        super.setAdapter(adapter);

        try {
            if (this.method1 == null) {
                this.method1= AdapterView.class.getDeclaredMethod("setNextSelectedPositionInt", int.class);
                this.method2 = AdapterView.class.getDeclaredMethod("setSelectedPositionInt", int.class);

                this.method1.setAccessible(true);
                this.method2.setAccessible(true);
            }

            this.method1.invoke(this, -1);
            this.method2.invoke(this, -1);

        } catch (Exception e) {
            // it failed, oh well...
            Log.error("Failed to set initial Spinner position", e);
        }
    }
}
