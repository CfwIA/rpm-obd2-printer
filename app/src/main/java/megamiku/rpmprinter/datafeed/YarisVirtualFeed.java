package megamiku.rpmprinter.datafeed;

import android.util.Log;

public class YarisVirtualFeed implements VehicleFeed {

	/**
	 * Car torque curve
	 * @param x : rpm
	 * @return
	 */
	public static double f(double x) {
		return  3.0322226515997849e-002 * Math.pow(x,1)
				+  1.0734847629637859e-004 * Math.pow(x,2)
				+ -1.7718872251240319e-007 * Math.pow(x,3)
				+  3.0045353294397007e-011 * Math.pow(x,4)
				+  2.0034330297719490e-013 * Math.pow(x,5)
				+ -2.3221984387179725e-016 * Math.pow(x,6)
				+  1.1614519176128144e-019 * Math.pow(x,7)
				+ -2.9419343117352579e-023 * Math.pow(x,8)
				+  3.6646076882812609e-027 * Math.pow(x,9)
				+ -2.6216804807352869e-031 * Math.pow(x,10)
				+  6.1999039854727485e-036 * Math.pow(x,11)
				+  1.5641675823950772e-038 * Math.pow(x,12)
				+ -1.9294725843989103e-042 * Math.pow(x,13)
				+ -7.4274197357572972e-046 * Math.pow(x,14)
				+  4.7455779561775420e-050 * Math.pow(x,15)
				+  2.2442604208556862e-053 * Math.pow(x,16)
				+  2.4301955148487257e-057 * Math.pow(x,17)
				+ -8.2589970440281281e-061 * Math.pow(x,18)
				+ -7.5587661463273623e-065 * Math.pow(x,19)
				+  6.4904967599811292e-069 * Math.pow(x,20)
				+  2.4126008262156680e-072 * Math.pow(x,21)
				+  1.8164540466831792e-076 * Math.pow(x,22)
				+ -8.9360236092195206e-080 * Math.pow(x,23)
				+  5.3571238809020278e-084 * Math.pow(x,24);
	}

	private double[] gearRatios = new double[] {
			3.250, 3.545, 1.904, 1.392, 1.031, 0.864
	};

	private double kineticEnergy = (1500.0/2.0) * (2.0*2.0);
	private int currentGear = 1;
	private double fuelLev = 192.0;
	private boolean continueRun = false;
	private Thread updateThread;

	private static String logtag = "YarisVirtualFeed";
	private boolean reverse = false;

	@Override
	public void run() {
		while (continueRun) {

			double vitesse = Math.sqrt(2.0*kineticEnergy/1500.0)*3.6;
			double rpm = (vitesse*30.0*gearRatios[currentGear]);
			if (vitesse >= 250)
				reverse = true;
			if (rpm < 6200.0) {
				if (reverse)
					kineticEnergy -= (1.0 - vitesse / 240.0) * (f(rpm) * rpm) / 200.0;
				else
					kineticEnergy += (1.0 - vitesse / 240.0) * (f(rpm) * rpm) / 200.0;
			}
			if (reverse) {
				if (rpm < (currentGear * 800) && currentGear > 1)
					currentGear--;
			}
			else {
				if (rpm > (6200.0 - currentGear * 200.0) && currentGear < 5)
					currentGear++;
			}

			fuelLev -= 0.01;
			try {
				Thread.sleep(16);
			} catch (InterruptedException e) {
				Log.e(logtag, "Interrupted exception => " + e.getMessage());
				throw new RuntimeException(e);
			}
		}
	}

	@Override
	public void connect() {
		Log.i(logtag, "Connection en cours");
		continueRun = true;
		updateThread = new Thread(this);
		updateThread.start();
		Log.i(logtag, "Connection faite");
	}

	@Override
	public boolean isValid() {
		return continueRun;
	}

	@Override
	public void close() {
		Log.i(logtag, "Closing connection");
		continueRun = false;
		updateThread = null;
		Log.i(logtag, "Connection closed");
	}


	@Override
	public boolean canGet(ObdPid pid) {
		return true;
	}

	@Override
	public double get(ObdPid pid) {

		switch (pid) {

			case ENGINE_RPM:
				return Math.sqrt(2.0 * kineticEnergy / 1500.0) * 108.0 * gearRatios[currentGear];
			case VEHICLE_SPEED:
				return Math.sqrt(2.0*kineticEnergy/1500.0)*3.6*0.8;
			case FUEL_TANK_LEVEL:
				return fuelLev;
			case ENGINE_COOLANT_TEMPERATURE:
				return Math.sqrt(2.0*kineticEnergy/1500.0)*1.8+25.0;
			default:
				return 0.0;
		}
	}

	@Override
	public void startDataRec() {

	}

	@Override
	public void stopDataRec() {

	}

}
