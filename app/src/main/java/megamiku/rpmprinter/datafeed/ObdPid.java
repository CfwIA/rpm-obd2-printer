package megamiku.rpmprinter.datafeed;

public enum ObdPid {
	PID_SUPPORTED_1_20(			0x00,   "0100"),
	ENGINE_LOAD(                0x04,   "0104"),
	ENGINE_RPM(					0x0C,   "010C"),
	VEHICLE_SPEED(				0x0D,   "010D"),
	THROTTLE_POSITION(          0x11,   "0111"),

	// Fuel level is not supported by vehicle
	FUEL_TANK_LEVEL(			0x2F, 	"012F"),
	PID_SUPPORTED_41_60(		0x40, 	"0140"),
	ENGINE_COOLANT_TEMPERATURE(	0x5C,	"0105");

	private byte pid;
	private String obdCommand;

	ObdPid(int pid, String obdComm) {
		this.pid = (byte) pid;
		obdCommand = obdComm;
	}

	public byte[] getELMCommand() {
		return (obdCommand+"\r").getBytes();
	}

	public String getStringCommand() {
		return obdCommand;
	}
}
