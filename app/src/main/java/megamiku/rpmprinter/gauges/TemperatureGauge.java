package megamiku.rpmprinter.gauges;

import android.content.Context;
import android.util.AttributeSet;

public class TemperatureGauge extends TextGauge {
	public TemperatureGauge(Context context) {
		super(context);
	}

	public TemperatureGauge(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public TemperatureGauge(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@Override
	public String postValueIndication() {
		return " C°";
	}

	@Override
	public void setValue(float value) {
		super.setValue(value);
		setUpperText(getValueToDisp(value));
	}

	@Override
	public void moveToValue(float value) {
		super.moveToValue(value);
		setUpperText(getValueToDisp(value));
	}
}
