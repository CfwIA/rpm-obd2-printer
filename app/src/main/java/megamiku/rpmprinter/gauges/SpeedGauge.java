package megamiku.rpmprinter.gauges;

import android.content.Context;
import android.util.AttributeSet;

public class SpeedGauge extends TextGauge {
	public SpeedGauge(Context context) {
		super(context);
	}

	public SpeedGauge(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public SpeedGauge(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@Override
	public String postValueIndication() {
		return " km/h";
	}
}
