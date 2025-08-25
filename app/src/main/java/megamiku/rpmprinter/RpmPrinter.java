package megamiku.rpmprinter;

import android.annotation.SuppressLint;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseArray;
import android.widget.Switch;
import android.widget.TextView;

import java.text.DecimalFormat;

import de.nitri.gauge.Gauge;
import megamiku.rpmprinter.datafeed.Obd2BlueFeed;
import megamiku.rpmprinter.datafeed.ObdPid;
import megamiku.rpmprinter.datafeed.VehicleFeed;

/**
 * Commentaires AS de base
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class RpmPrinter extends AppCompatActivity {

	// Indicateur de tours
	private Gauge rpmGauge;
	//Indicateur de temperature
	private Gauge tempGauge;
	//TextView de vitesse
	private TextView speedTV;
	//TextView de niveau de carburant
	private TextView fuelLevelTV;

	private TextView throttleTV;

	//Le tag pour le logcat
	private static final String TAG = "RpmPrinter";

	//Le formateur pour afficher le carburant
	private final DecimalFormat decFormat2 = new DecimalFormat("##.#");

	//Le thread de rafraishissement d'affichage
	private Thread refresherThread;

	//Le feed de données a obtenir
	private VehicleFeed vehicleFeed;

	private Switch speedCapButton;
	private Switch tempCapButton;
	private Switch voiceSwitcher;
	private Switch recordDataSwitcher;

	public static boolean writeSpeed = false;
	public static boolean writeTemp = false;
	public static boolean writeRpm = true;
	public static boolean hasVoice = false;
	private String indisponible;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main_display);
		/*
		Objects.requireNonNull(getSupportActionBar()).setTitle("Probleme Recuperation");
		getSupportActionBar().*/

		rpmGauge = findViewById(R.id.RpmGauge);
		tempGauge = findViewById(R.id.TempGauge);
		speedTV = findViewById(R.id.SpeedTextView);
		throttleTV = findViewById(R.id.ThrottleTextView);
		speedCapButton = findViewById(R.id.captureSpeedButton);
		recordDataSwitcher = findViewById(R.id.recordData);
		indisponible = getResources().getString(R.string.indisponible);

		speedCapButton.setOnCheckedChangeListener(
				(compoundButton, ischecked) -> writeSpeed = ischecked
		);
		tempCapButton = findViewById(R.id.captureTempButton);
		tempCapButton.setOnCheckedChangeListener(
				(compoundButton, ischecked) -> writeTemp = ischecked
		);

		voiceSwitcher = findViewById(R.id.hasLimitsVoice);
		voiceSwitcher.setOnCheckedChangeListener(((compoundButton, b) -> hasVoice = b));

		rpmGauge.setOnTouchListener((a,b) -> writeRpm = !writeRpm);

		tempCapButton.setChecked(true);
		speedCapButton.setChecked(true);

		recordDataSwitcher.setOnCheckedChangeListener((compoundButton, b) -> {
			if (b)
				vehicleFeed.startDataRec();
			else
				vehicleFeed.stopDataRec();
		});

		ActionBar ab = getSupportActionBar();
		if (ab != null)
			ab.hide();
	}

	private MediaPlayer mpsound_warning_bleep;
	private MediaPlayer mpsound_warning_30;
	private MediaPlayer mpsound_warning_50;
	private MediaPlayer mpsound_warning_70;
	private MediaPlayer mpsound_warning_80;
	private MediaPlayer mpsound_warning_90;
	private MediaPlayer mpsound_warning_110;
	private MediaPlayer mpsound_warning_130;
	private MediaPlayer mpsound_warning_140;

	private SparseArray<MediaPlayer> soundwarnMap;

	@Override
	protected void onStart() {
		super.onStart();

		Log.i(TAG, "onStart: Lancement, instantiation du feed");
		try {
			vehicleFeed = new Obd2BlueFeed();
			Log.i(TAG, "onStart: Connection avec le feed");
			vehicleFeed.connect();
		} catch (Exception e) {
			Log.e(TAG, "onStart: Exception occured", e);
			runOnUiThread(() -> {
				ActionBar ab = getSupportActionBar();
				if (ab == null)
					throw new RuntimeException(e);
				runOnUiThread(() -> {
					ab.setTitle(e.getMessage());
					ab.show();
				});
			});
		}

		mpsound_warning_bleep = MediaPlayer.create(getApplicationContext(), R.raw.warning_sound);
		/*
		mpsound_warning_30 = MediaPlayer.create(getApplicationContext(), R.raw.warning_30);
		mpsound_warning_50 = MediaPlayer.create(getApplicationContext(), R.raw.warning_50);
		mpsound_warning_70 = MediaPlayer.create(getApplicationContext(), R.raw.warning_70);
		mpsound_warning_80 = MediaPlayer.create(getApplicationContext(), R.raw.warning_80);
		mpsound_warning_90 = MediaPlayer.create(getApplicationContext(), R.raw.warning_90);
		mpsound_warning_110 = MediaPlayer.create(getApplicationContext(), R.raw.warning_110);
		mpsound_warning_130 = MediaPlayer.create(getApplicationContext(), R.raw.warning_130);
		mpsound_warning_140 = MediaPlayer.create(getApplicationContext(), R.raw.warning_140);*/

		soundwarnMap = new SparseArray<>();
		for(int sp : speedWarnings) {
			try {
				soundwarnMap.put(sp, (MediaPlayer) RpmPrinter.class.getDeclaredField("mpsound_warning_" + sp).get(this));
			} catch (IllegalAccessException | NoSuchFieldException e) {
				e.printStackTrace();
			}
		}

		refresherThread = new Thread(() -> {

			while(vehicleFeed != null && vehicleFeed.isValid()) {

				setSpeed((int) vehicleFeed.get(ObdPid.VEHICLE_SPEED));
				setRpm((int) vehicleFeed.get(ObdPid.ENGINE_RPM));
				setTemp((int) vehicleFeed.get(ObdPid.ENGINE_COOLANT_TEMPERATURE));
				setThrottle((int) vehicleFeed.get(ObdPid.THROTTLE_POSITION));

				try {
					Thread.sleep(30);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}

			Log.i(TAG, "onStart: Refresher thread ended");
		});

		refresherThread.setUncaughtExceptionHandler((t,e) -> {
			ActionBar ab = getSupportActionBar();
			Log.e(TAG, "onResume: Uncaught exception in thread", e);
			if (ab==null)
				throw new RuntimeException(e);
			runOnUiThread(() -> {
				if (e.getStackTrace()[0].getMethodName().equals("isValid")) {
					ab.setTitle("Erreur de connexion");
				}
				else {
					ab.setTitle(e.getMessage());
				}
				ab.show();
			}
			);

		});

		refresherThread.start();

	}


	@Override
	protected void onStop() {
		super.onStop();
		if (vehicleFeed != null)
			vehicleFeed.close();
		refresherThread = null;
		for(int i = 0; i < soundwarnMap.size(); ++i) {
			soundwarnMap.get(soundwarnMap.keyAt(i)).release();
		}
		mpsound_warning_bleep.release();
	}

	private static final double fuelDividerRatio = 0.16470588235294117; // 42/255

	private void setTemp(int temp) {
		runOnUiThread(()->tempGauge.moveToValue(temp));
	}

	private void setRpm(int rpm) {
		runOnUiThread(()->rpmGauge.moveToValue(rpm));
	}

	@SuppressLint("SetTextI18n")
	private void setThrottle(int pos) {
		runOnUiThread(() -> throttleTV.setText(pos + "%"));
	}


	private int lastSpeed = 0;
	private int[] speedWarnings = {30,50,70,80,90,110,130, 140};
	private final int decalage = -2;

	@SuppressLint("SetTextI18n")
	private void setSpeed(int speed) {

		if (hasVoice && writeSpeed) {
			for (int spd : speedWarnings) {
				int spd2 = spd + decalage;
				if (lastSpeed < spd2 && speed >= spd2) {
					MediaPlayer mp = soundwarnMap.get(spd);
					if (mp != null) {
						mp.start();
					} else {
						Log.e(TAG, "Can't run sound warning for " + spd);
					}
				}
			}
			lastSpeed = speed;

			if (speed > 145 && !mpsound_warning_bleep.isPlaying()) {
				mpsound_warning_bleep.start();
			}
		}

		runOnUiThread(()->speedTV.setText( writeSpeed ? (speed + " km/h") : indisponible));
	}

	@SuppressLint("SetTextI18n")
	private void setFuelLevel(int level) {
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
	}

}
