package megamiku.rpmprinter.datafeed;

public interface VehicleFeed extends Runnable {

	void connect();

	boolean isValid();

	void close();

	boolean canGet(ObdPid pid);

	double get(ObdPid pid);

	void startDataRec();

	void stopDataRec();

}
