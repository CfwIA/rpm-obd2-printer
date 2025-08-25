package megamiku.rpmprinter.datafeed;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

import megamiku.rpmprinter.RpmPrinter;

public class Obd2BlueFeed implements VehicleFeed {

	private static final String TAG = "OBD2 Bluetooth Feed";

	protected BluetoothAdapter bAdapter;
	protected UpdateData updateData = new UpdateData();

	public static final String DEVICE_NAME = "OBDII";
	public static final UUID DEVICE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

	protected BluetoothSocket bSocket = null;
	protected OutputStream oStream;
	protected InputStream iStream;
	protected BufferedInputStream bis;

	private Thread dataGetter;

	private boolean dataWrite;
	private long dataWriteStart;
	private PrintWriter dataPw;

	public Obd2BlueFeed() {
		bAdapter = BluetoothAdapter.getDefaultAdapter();
		if (bAdapter == null)
			throw new IllegalStateException("No bluetooth device on this machine");
	}


	@Override
	public void connect() {

		Log.d(TAG, "connect: connection method called");

		int tries = 0;
		if (!bAdapter.isEnabled())
			throw new RuntimeException("Bluetooth is disabled");

		//Find the device
		Set<BluetoothDevice> deviceSet = bAdapter.getBondedDevices();
		BluetoothDevice bDevice = null;
		for(BluetoothDevice bd : deviceSet) {
			Log.d(TAG, "Bluetooth name : " + bd.getName());
			if (bd.getName().equals(DEVICE_NAME)) {
				bDevice = bd;
				break;
			}
		}

		if (bDevice == null) {
			Log.w(TAG, "connect: Device not found");
			throw new IllegalStateException("Bluetooth device not found");
		}

		Log.i(TAG, "connect: Discovery cancellation");
		if (bAdapter.isDiscovering())
			bAdapter.cancelDiscovery();

		try {
			bSocket = bDevice.createRfcommSocketToServiceRecord(DEVICE_UUID);
			Log.i(TAG, "connect: trying to connect");
			bSocket.connect();
			Log.i(TAG, "connect: opening the output stream");
			oStream = bSocket.getOutputStream();
			Log.i(TAG, "connect: opening the input stream");
			iStream = bSocket.getInputStream();
			bis = new BufferedInputStream(iStream);
		} catch (IOException e) {
			Log.e(TAG, "connect: ", e);
			throw new RuntimeException(e);
		}

		//Launch data refresher thread
		dataGetter = new Thread(this);
		dataGetter.start();
	}

	@Override
	public synchronized boolean isValid() {
		return bSocket.isConnected();
	}

	@Override
	public synchronized void close() {
		try {
			iStream.close();
			oStream.close();
			bSocket.close();
		} catch (IOException e) {
			Log.e(TAG, "close: ", e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean canGet(ObdPid pid) {
		return true;
	}

	@Override
	public double get(ObdPid pid) {
		switch (pid) {

			case ENGINE_RPM:
				return updateData.rpm;
			case VEHICLE_SPEED:
				return updateData.speed;
			case FUEL_TANK_LEVEL:
				return updateData.fuelLevel;
			case ENGINE_COOLANT_TEMPERATURE:
				return updateData.coolantTemperature;
			case ENGINE_LOAD:
				return updateData.engineLoad;
			case THROTTLE_POSITION:
				return updateData.throttlePosition;
			default:
				break;
		}
		return 0;
	}

	@RequiresApi(api = Build.VERSION_CODES.O)
	@Override
	public void startDataRec() {
		synchronized (this) {
			dataWrite = true;
			dataWriteStart = System.currentTimeMillis();
			try {

				File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM + "/obd2_data/");
				Log.d(TAG, "startDataRec: Creating data writer at " + path.getAbsolutePath());
				File dataF = new File(path,"data.txt");
				dataF.createNewFile();
				dataPw = new PrintWriter(new FileOutputStream(dataF));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void stopDataRec() {
		synchronized (this) {
			dataWrite = false;
			dataPw = null;
		}
	}

	public final static int REQUEST_DELAY = 150;

	public static int speedRequestPending = 0;
	public static int rpmRequestPending = 0;

	private int countDown = 0;

	@Override
	public void run() {

	    while(isValid()) {

            try {
            	send(ObdPid.THROTTLE_POSITION);
	            dataReceived = false;
	            Thread.sleep(REQUEST_DELAY);
	            recv();
            	if (RpmPrinter.writeRpm) {
		            send(ObdPid.ENGINE_RPM);
		            dataReceived = false;
		            Thread.sleep(REQUEST_DELAY);
		            recv();
	            }
	            Thread.sleep(10);
				if (RpmPrinter.writeSpeed) {
					send(ObdPid.VEHICLE_SPEED);
					dataReceived = false;
					Thread.sleep(REQUEST_DELAY);
					recv();
				} else {
					updateData.speed = 0;
				}
				if (countDown <= 0) {
					Thread.sleep(10);
					if (RpmPrinter.writeTemp) {
						send(ObdPid.ENGINE_COOLANT_TEMPERATURE);
						dataReceived = false;
						Thread.sleep(REQUEST_DELAY);
						recv();
						countDown = 8;
						if (updateData.coolantTemperature >= 83 && updateData.coolantTemperature <= 87)
							countDown = 40;
					} else {
						updateData.coolantTemperature = -65;
					}
				}
				--countDown;
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }

        }

    }

    private void writeDataToFile(String lineToAdd) {
		double secPassed = (double)(System.currentTimeMillis() - dataWriteStart) / 1000.0;
		synchronized (this) {
			if (dataPw != null) {
				dataPw.println(secPassed + ":" + lineToAdd);
				dataPw.flush();
			}
		}
    }

    private byte[] recData = new byte[256];
	private boolean dataReceived = false;

	private String totalData = "";

    private void recv() throws IOException {

    	bis.read(recData, 0, recData.length);

		String str = new String(recData);
		str = str.substring(0, str.indexOf(str.charAt(str.length()-1)));
	    Log.i(TAG, "recv: Chain received = \"" + str + "\"");

		if (str.contains("NO DATA")) {
			// No data
			Log.i(TAG, "recv: NO DATA");
			return;
//			throw new RuntimeException("No data");
		}

		if (str.contains("BUFFER FULL")) {
			Log.i(TAG, "recv: BUFFER FULL");
			throw new RuntimeException("Buffer full");
		}

		String pstr = str.replace(" ", "");

		if (pstr.contains("410C")) {
			// Engine rpm
			String dt = pstr.substring(pstr.indexOf("410C") + 4);

			if (dt.length() < 4) {
				Log.e(TAG, "recv: Engine RPM data is not long enough : " + pstr);
				return;
			}

			int val = (hex2val(dt.charAt(0)) << 12)
					+ (hex2val(dt.charAt(1)) << 8)
					+ (hex2val(dt.charAt(2)) << 4)
					+ hex2val(dt.charAt(3));

			updateData.rpm = ((double)val) / 4.0;
			dataReceived = true;
			writeDataToFile("RPM=" + updateData.rpm);
		} else {

			if (pstr.contains("410D")) {
				// Vehicle speed
				String dt = pstr.substring(pstr.indexOf("410D") + 4);
				if (dt.length() < 2) {
					Log.e(TAG, "recv: Vehcile speed data is not long enough");
					return;
				}

				updateData.speed = (hex2val(dt.charAt(0)) << 4) + hex2val(dt.charAt(1));
				dataReceived = true;
				writeDataToFile("Speed=" + updateData.speed);
			}

			if (pstr.contains("4105")) {
				// Engine coolant temp
				String dt = pstr.substring(pstr.indexOf("4105") + 4);
				if (dt.length() < 2) {
					Log.e(TAG, "recv: Temp data not long enough");
					return;
				}

				updateData.coolantTemperature = (hex2val(dt.charAt(0)) << 4) + hex2val(dt.charAt(1)) - 40;
				dataReceived = true;
				writeDataToFile("Coolant Temperature=" + updateData.coolantTemperature);
			}

			if (pstr.contains("4104")) {
				// Engine load
				String dt = pstr.substring(pstr.indexOf("4104") + 4);
				if (dt.length() < 2) {
					Log.e(TAG, "recv: Engine load data is not long enough");
					return;
				}

				updateData.engineLoad = (hex2val(dt.charAt(0)) << 4) + hex2val(dt.charAt(1));
				updateData.engineLoad = Math.round(updateData.engineLoad / 2.55f);
				Log.d(TAG,"VALUE = " + updateData.engineLoad + " / 100");
			}

			if (pstr.contains("4111")) {
				// Engine load
				String dt = pstr.substring(pstr.indexOf("4111") + 4);
				if (dt.length() < 2) {
					Log.e(TAG, "recv: Throttle position data is not long enough");
					return;
				}

				updateData.throttlePosition = (hex2val(dt.charAt(0)) << 4) + hex2val(dt.charAt(1));
				updateData.throttlePosition = Math.round(updateData.throttlePosition / 2.55f);
				Log.d(TAG,"VALUE = " + updateData.engineLoad + " / 100");
			}
		}

	}

	private int hex2val(char c) {
		if (c >= '0' && c <= '9')
			return c-'0';
		if (c >= 'A' && c <= 'F')
			return c-'A'+10;
		throw new RuntimeException("Bad character sent : " + c);
	}

	private void send(ObdPid pid) throws IOException {
		Log.i(TAG, "send obd2 command " + pid.name() + "\t-\tHEXA : " + pid.getStringCommand());
		synchronized (this) {
			oStream.write(pid.getELMCommand());
			if (pid == ObdPid.ENGINE_RPM)
			    ++rpmRequestPending;
			if (pid == ObdPid.VEHICLE_SPEED)
			    ++speedRequestPending;
		}
	}
}
