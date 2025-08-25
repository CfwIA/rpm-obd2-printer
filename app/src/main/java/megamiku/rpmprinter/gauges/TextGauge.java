package megamiku.rpmprinter.gauges;

import android.content.Context;
import android.util.AttributeSet;

import de.nitri.gauge.Gauge;

public class TextGauge extends Gauge {
	public TextGauge(Context context) {
		super(context);
	}

	public TextGauge(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public TextGauge(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	public String postValueIndication() {
		return "";
	}

	public String getValueToDisp(float value) {
		return Math.round(value) + postValueIndication();
	}

	@Override
	public void setValue(float value) {
		super.setValue(value);
		super.setLowerText(getValueToDisp(value));
	}

	@Override
	public void moveToValue(float value) {
		super.moveToValue(value);
		super.setLowerText(getValueToDisp(value));	}
}
