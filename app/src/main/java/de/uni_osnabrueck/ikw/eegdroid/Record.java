package de.uni_osnabrueck.ikw.eegdroid;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewStub;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;


public class Record extends AppCompatActivity {

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    public static final String EXTRAS_DEVICE_MODEL = "DEVICE_MODEL"; // "2": old, "3": new
    private final static String TAG = Record.class.getSimpleName();
    private final Handler handler = new Handler();
    private final List<Float> dp_received = new ArrayList<>();
    private final List<List<Float>> accumulated = new ArrayList<>();
    private final int MAX_VISIBLE = 500;  // see 500ms at the time on the plot
    private final ArrayList<Entry> lineEntries1 = new ArrayList<>();
    private final ArrayList<Entry> lineEntries2 = new ArrayList<>();
    private final ArrayList<Entry> lineEntries3 = new ArrayList<>();
    private final ArrayList<Entry> lineEntries4 = new ArrayList<>();
    private final ArrayList<Entry> lineEntries5 = new ArrayList<>();
    private final ArrayList<Entry> lineEntries6 = new ArrayList<>();
    private final ArrayList<Entry> lineEntries7 = new ArrayList<>();
    private final ArrayList<Entry> lineEntries8 = new ArrayList<>();
    private TextView mConnectionState;
    private TextView viewDeviceAddress;
    private boolean mNewDevice;
    private String mDeviceAddress;
    private BluetoothLeService mBluetoothLeService;
    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }

            // hack for ensuring a successful connection
            // constants
            int CONNECT_DELAY = 2000;
            handler.postDelayed(() -> mBluetoothLeService.connect(mDeviceAddress), CONNECT_DELAY);  // connect with a defined delay
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            if (mBluetoothLeService != null) mBluetoothLeService = null;
        }
    };
    private ArrayList<Integer> pkgIDs = new ArrayList<>();
    private BluetoothGattCharacteristic mNotifyCharacteristic;
    private boolean recording = false;
    private boolean notifying = false;
    private String selected_gain;
    private float res_time;
    private float res_freq;
    private int cnt = 0;
    private int ch1_color;
    private int ch2_color;
    private int ch3_color;
    private int ch4_color;
    private int ch5_color;
    private int ch6_color;
    private int ch7_color;
    private int ch8_color;
    private boolean show_ch1 = true;
    private boolean show_ch2 = true;
    private boolean show_ch3 = true;
    private boolean show_ch4 = true;
    private boolean show_ch5 = true;
    private boolean show_ch6 = true;
    private boolean show_ch7 = true;
    private boolean show_ch8 = true;
    private int enabledCheckboxes = 8;
    private TextView mCh1;
    private TextView mCh2;
    private TextView mCh3;
    private TextView mCh4;
    private TextView mCh5;
    private TextView mCh6;
    private TextView mCh7;
    private TextView mCh8;
    private CheckBox chckbx_ch1;
    private CheckBox chckbx_ch2;
    private CheckBox chckbx_ch3;
    private CheckBox chckbx_ch4;
    private CheckBox chckbx_ch5;
    private CheckBox chckbx_ch6;
    private CheckBox chckbx_ch7;
    private CheckBox chckbx_ch8;
    private TextView mXAxis;
    private TextView mDataResolution;
    private Spinner gain_spinner;
    private LineChart mChart;
    private ImageButton imageButtonRecord;
    private ImageButton imageButtonSave;
    private ImageButton imageButtonDiscard;
    private Switch switch_plots;
    private View layout_plots;
    private boolean plotting = false;
    private List<float[]> main_data;
    private final View.OnClickListener imageDiscardOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            main_data = new ArrayList<>();
            Toast.makeText(
                    getApplicationContext(),
                    "Your EEG session was discarded.",
                    Toast.LENGTH_LONG
            ).show();
            buttons_prerecording();
        }
    };
    private float data_cnt = 0;
    private String start_time;
    private String end_time;
    private long start_watch;
    private String recording_time;
    private long start_timestamp;
    private long end_timestamp;
    private final View.OnClickListener imageRecordOnClickListener = v -> {
        if (!recording) {
            startTrial();
            Toast.makeText(
                    getApplicationContext(),
                    "Recording in process.",
                    Toast.LENGTH_LONG
            ).show();
            buttons_recording();
        } else {
            endTrial();
            buttons_postrecording();
        }
    };
    private final View.OnClickListener imageSaveOnClickListener = v -> {
        LayoutInflater layoutInflaterAndroid = LayoutInflater.from(getApplicationContext());
        View mView = layoutInflaterAndroid.inflate(R.layout.input_dialog_string, null);
        AlertDialog.Builder alertDialogBuilderUserInput = new AlertDialog.Builder(Record.this);
        alertDialogBuilderUserInput.setView(mView);

        final EditText userInputLabel = mView.findViewById(R.id.input_dialog_string_Input);

        alertDialogBuilderUserInput
                .setCancelable(false)
                .setTitle(R.string.session_label_title)
                .setMessage(getResources().getString(R.string.enter_session_label))
                .setPositiveButton(R.string.save, (dialogBox, id) -> {
                    if (!userInputLabel.getText().toString().isEmpty()) {
                        saveSession(userInputLabel.getText().toString());
                    } else saveSession();
                    Toast.makeText(getApplicationContext(), "Your EEG session was successfully stored.", Toast.LENGTH_LONG).show();
                });

        AlertDialog alertDialogAndroid = alertDialogBuilderUserInput.create();
        alertDialogAndroid.show();
        buttons_prerecording();
    };
    private Thread thread;
    private long plotting_start;
    private final CompoundButton.OnCheckedChangeListener switchPlotsOnCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (!isChecked) {
                layout_plots.setVisibility(ViewStub.GONE);
                mXAxis.setVisibility(ViewStub.GONE);
                plotting = false;
            } else {
                layout_plots.setVisibility(ViewStub.VISIBLE);
                mXAxis.setVisibility(ViewStub.VISIBLE);
                plotting = true;
                plotting_start = System.currentTimeMillis();
            }
        }
    };
    private boolean deviceConnected = false;
    private boolean casting = false;
    private Menu menu;
    private List<List<Float>> recentlyDisplayedData;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private List<Float> microV;
    private CastThread caster;
    private Timer timer;
    private TimerTask timerTask;
    private boolean timerRunning = false;
    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
//            Log.d("Device connected: ", deviceConnected ? "true" : "false");
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                deviceConnected = true;
                buttons_prerecording();
                setConnectionStatus(true);
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                deviceConnected = false;
                setConnectionStatus(false);
                clearUI();
                disableCheckboxes();
                data_cnt = 0;
                if (timer != null) {
                    timer.cancel();
                    timer.purge();
                }
                timerRunning = false;
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                data_cnt = 0;
                readGattCharacteristic(mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action) && deviceConnected) {
                data_cnt++;
                if (!timerRunning) startTimer();
                long last_data = System.currentTimeMillis();
                enableCheckboxes();
                microV = transData(Objects.requireNonNull(intent.getIntArrayExtra(BluetoothLeService.EXTRA_DATA)));
                displayData(microV);
                if (plotting) {
                    accumulated.add(microV);
                    long plotting_elapsed = last_data - plotting_start;
                    int ACCUM_PLOT = 30;
                    if (plotting_elapsed > ACCUM_PLOT) {
                        addEntries(accumulated);
                        accumulated.clear();
                        plotting_start = System.currentTimeMillis();
                    }
                }
                if (recording) {
                    storeData(microV);
                    mConnectionState.setText(R.string.recording);
                    mConnectionState.setTextColor(Color.RED);
                } else {
                    mConnectionState.setText(R.string.device_connected);
                    mConnectionState.setTextColor(Color.GREEN);
                }
            }
        }
    };

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.EXTRA_DATA);
        return intentFilter;
    }

    private void setGainSpinner() {
        int gains_set = mNewDevice ? R.array.gains_new : R.array.gains_old;
        gain_spinner.setAdapter(new ArrayAdapter<>(getApplicationContext(),
                android.R.layout.simple_spinner_dropdown_item,
                getResources().getStringArray(gains_set)));
        int gain_default = mNewDevice ? 0 : 1;
        gain_spinner.setSelection(gain_default);
        gain_spinner.setEnabled(true);
        selected_gain = gain_spinner.getSelectedItem().toString();
        gain_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!mNewDevice) {
                    switch (position) {
                        case 0:
                            selected_gain = "0.5";
                            break;
                        case 2:
                            selected_gain = "2";
                            break;
                        case 3:
                            selected_gain = "4";
                            break;
                        case 4:
                            selected_gain = "8";
                            break;
                        case 5:
                            selected_gain = "16";
                            break;
                        case 6:
                            selected_gain = "32";
                            break;
                        case 7:
                            selected_gain = "64";
                            break;
                        default:
                            selected_gain = "1";
                    }
                } else {
                    switch (position) {
                        case 1:
                            selected_gain = "2";
                            break;
                        case 2:
                            selected_gain = "4";
                            break;
                        case 3:
                            selected_gain = "8";
                            break;
                        default:
                            selected_gain = "1";
                    }
                }
                if (deviceConnected)
                    writeGattCharacteristic(mBluetoothLeService.getSupportedGattServices());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // sometimes you need nothing here
            }
        });
    }

    private void initializeTimerTask() {
        timerTask = new TimerTask() {
            public void run() {
                handler.post(() -> {
                    res_time = 1000 / data_cnt;
                    String hertz = (int) data_cnt + "Hz";
                    res_freq = data_cnt;
                    @SuppressLint("DefaultLocale") String resolution = String.format("%.2f", res_time) + "ms - ";
                    String content = resolution + hertz;
                    mDataResolution.setText(content);
                    data_cnt = 0;
                });
            }
        };
    }

    private void startTimer() {
        //set a new Timer
        timer = new Timer();
        //initialize the TimerTask's job
        initializeTimerTask();
        // schedule the timer, the stimulus presence will repeat every 1 seconds
        timer.schedule(timerTask, 1000, 1000);
        timerRunning = true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        setContentView(R.layout.activity_record);

        ch1_color = ContextCompat.getColor(getApplicationContext(), R.color.aqua);
        ch2_color = ContextCompat.getColor(getApplicationContext(), R.color.fuchsia);
        ch3_color = ContextCompat.getColor(getApplicationContext(), R.color.green);
        ch4_color = ContextCompat.getColor(getApplicationContext(), android.R.color.holo_purple);
        ch5_color = ContextCompat.getColor(getApplicationContext(), R.color.orange);
        ch6_color = ContextCompat.getColor(getApplicationContext(), R.color.red);
        ch7_color = ContextCompat.getColor(getApplicationContext(), R.color.yellow);
        ch8_color = ContextCompat.getColor(getApplicationContext(), R.color.black);
        imageButtonRecord = findViewById(R.id.imageButtonRecord);
        imageButtonSave = findViewById(R.id.imageButtonSave);
        imageButtonDiscard = findViewById(R.id.imageButtonDiscard);
        switch_plots = findViewById(R.id.switch_plots);
        gain_spinner = findViewById(R.id.gain_spinner);

        layout_plots = findViewById(R.id.linearLayout_chart);
        layout_plots.setVisibility(ViewStub.GONE);
        mXAxis = findViewById(R.id.XAxis_title);
        mXAxis.setVisibility(ViewStub.GONE);
        imageButtonRecord.setOnClickListener(imageRecordOnClickListener);
        imageButtonSave.setOnClickListener(imageSaveOnClickListener);
        imageButtonDiscard.setOnClickListener(imageDiscardOnClickListener);
        switch_plots.setOnCheckedChangeListener(switchPlotsOnCheckedChangeListener);

        // Sets up UI references.
        mConnectionState = findViewById(R.id.connection_state);

        viewDeviceAddress = findViewById(R.id.device_address);
        mConnectionState = findViewById(R.id.connection_state);
        mCh1 = findViewById(R.id.ch1);
        mCh2 = findViewById(R.id.ch2);
        mCh3 = findViewById(R.id.ch3);
        mCh4 = findViewById(R.id.ch4);
        mCh5 = findViewById(R.id.ch5);
        mCh6 = findViewById(R.id.ch6);
        mCh7 = findViewById(R.id.ch7);
        mCh8 = findViewById(R.id.ch8);
        mCh1.setTextColor(ch1_color);
        mCh2.setTextColor(ch2_color);
        mCh3.setTextColor(ch3_color);
        mCh4.setTextColor(ch4_color);
        mCh5.setTextColor(ch5_color);
        mCh6.setTextColor(ch6_color);
        mCh7.setTextColor(ch7_color);
        mCh8.setTextColor(ch8_color);
        chckbx_ch1 = findViewById(R.id.checkBox_ch1);
        chckbx_ch2 = findViewById(R.id.checkBox_ch2);
        chckbx_ch3 = findViewById(R.id.checkBox_ch3);
        chckbx_ch4 = findViewById(R.id.checkBox_ch4);
        chckbx_ch5 = findViewById(R.id.checkBox_ch5);
        chckbx_ch6 = findViewById(R.id.checkBox_ch6);
        chckbx_ch7 = findViewById(R.id.checkBox_ch7);
        chckbx_ch8 = findViewById(R.id.checkBox_ch8);
        chckbx_ch1.setOnCheckedChangeListener((buttonView, isChecked) -> {
            show_ch1 = isChecked;
            if (!isChecked) enabledCheckboxes--;
            else enabledCheckboxes++;
            if (enabledCheckboxes == 0) {
                chckbx_ch1.setChecked(true);
                show_ch1 = true;
                enabledCheckboxes++;
            }
        });
        chckbx_ch2.setOnCheckedChangeListener((buttonView, isChecked) -> {
            show_ch2 = isChecked;
            if (!isChecked) enabledCheckboxes--;
            else enabledCheckboxes++;
            if (enabledCheckboxes == 0) {
                chckbx_ch2.setChecked(true);
                show_ch2 = true;
                enabledCheckboxes++;
            }
        });
        chckbx_ch3.setOnCheckedChangeListener((buttonView, isChecked) -> {
            show_ch3 = isChecked;
            if (!isChecked) enabledCheckboxes--;
            else enabledCheckboxes++;
            if (enabledCheckboxes == 0) {
                chckbx_ch3.setChecked(true);
                show_ch3 = true;
                enabledCheckboxes++;
            }
        });
        chckbx_ch4.setOnCheckedChangeListener((buttonView, isChecked) -> {
            show_ch4 = isChecked;
            if (!isChecked) enabledCheckboxes--;
            else enabledCheckboxes++;
            if (enabledCheckboxes == 0) {
                chckbx_ch4.setChecked(true);
                show_ch4 = true;
                enabledCheckboxes++;
            }
        });
        chckbx_ch5.setOnCheckedChangeListener((buttonView, isChecked) -> {
            show_ch5 = isChecked;
            if (!isChecked) enabledCheckboxes--;
            else enabledCheckboxes++;
            if (enabledCheckboxes == 0) {
                chckbx_ch5.setChecked(true);
                show_ch5 = true;
                enabledCheckboxes++;
            }
        });
        chckbx_ch6.setOnCheckedChangeListener((buttonView, isChecked) -> {
            show_ch6 = isChecked;
            if (!isChecked) enabledCheckboxes--;
            else enabledCheckboxes++;
            if (enabledCheckboxes == 0) {
                chckbx_ch6.setChecked(true);
                show_ch6 = true;
                enabledCheckboxes++;
            }
        });
        chckbx_ch7.setOnCheckedChangeListener((buttonView, isChecked) -> {
            show_ch7 = isChecked;
            if (!isChecked) enabledCheckboxes--;
            else enabledCheckboxes++;
            if (enabledCheckboxes == 0) {
                chckbx_ch7.setChecked(true);
                show_ch7 = true;
                enabledCheckboxes++;
            }
        });
        chckbx_ch8.setOnCheckedChangeListener((buttonView, isChecked) -> {
            show_ch8 = isChecked;
            if (!isChecked) enabledCheckboxes--;
            else enabledCheckboxes++;
            if (enabledCheckboxes == 0) {
                chckbx_ch8.setChecked(true);
                show_ch8 = true;
                enabledCheckboxes++;
            }
        });
        mDataResolution = findViewById(R.id.resolution_value);
        setChart();
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.bluethoot_conect, menu);
        this.menu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        int id = item.getItemId();
        if (id == R.id.scan) {
            if (!deviceConnected) {
                Intent intent = new Intent(this, DeviceScanActivity.class);
                startActivityForResult(intent, 1200);
            } else {
                //Handles the Dialog to confirm the closing of the activity
                AlertDialog.Builder alert = new AlertDialog.Builder(this)
                        .setTitle(R.string.dialog_title)
                        .setMessage(getResources().getString(R.string.confirmation_disconnect));
                alert.setPositiveButton(android.R.string.yes, (dialog, which) -> mBluetoothLeService.disconnect());
                alert.setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                    // close dialog
                    dialog.cancel();
                });
                alert.show();
            }
            return true;
        }

        if (id == android.R.id.home) {
            if (recording) {
                //Handles the Dialog to confirm the closing of the activity
                AlertDialog.Builder alert = new AlertDialog.Builder(this)
                        .setTitle(R.string.dialog_title)
                        .setMessage(getResources().getString(R.string.confirmation_close_record));
                alert.setPositiveButton(android.R.string.yes, (dialog, which) -> onBackPressed());
                alert.setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                    // close dialog
                    dialog.cancel();
                });
                alert.show();
            } else {
                onBackPressed();
            }
            return true;
        }

        if (id == R.id.notify) toggleNotifying();

        if (id == R.id.cast) {
            MenuItem menuItemCast = menu.findItem(R.id.cast);
            if (!casting) {
                casting = true;
                caster = new CastThread();
                caster.start();
                menuItemCast.setIcon(R.drawable.ic_cast_blue_24dp);
            } else {
                casting = false;
                caster.staph();
                menuItemCast.setIcon(R.drawable.ic_cast_white_24dp);
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void toggleNotifying() {
        MenuItem menuItemNotify = menu.findItem(R.id.notify);
        if (!notifying) {
            notifying = true;
            readGattCharacteristic(mBluetoothLeService.getSupportedGattServices());
            menuItemNotify.setIcon(R.drawable.ic_notifications_active_blue_24dp);
        } else {
            notifying = false;
            readGattCharacteristic(mBluetoothLeService.getSupportedGattServices());
            menuItemNotify.setIcon(R.drawable.ic_notifications_off_white_24dp);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        // Check which request we're responding to
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == 1200) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                // The user picked a contact.
                // The Intent's data Uri identifies which contact was selected
                String mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
                mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
                String model = intent.getStringExtra(EXTRAS_DEVICE_MODEL);
                if (model != null) mNewDevice = model.equals("3");
                setGainSpinner();
                Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
                bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
            }
        }
    }

    private void writeGattCharacteristic(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid;
        String charUuid;
        toggleNotifying();
        for (BluetoothGattService gattService : gattServices) {
            uuid = gattService.getUuid().toString();

            if (((!mNewDevice && uuid.equals("05bbfe57-2f19-ab84-c448-6769fe64d994")) ||
                    (mNewDevice && uuid.equals("00000ee6-0000-1000-8000-00805f9b34fb")))) {
                List<BluetoothGattCharacteristic> gattCharacteristics =
                        gattService.getCharacteristics();

                // Loops through available Characteristics.
                for (final BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                    charUuid = gattCharacteristic.getUuid().toString();
                    if ((!mNewDevice && charUuid.equals("fcbea85a-4d87-18a2-2141-0d8d2437c0a4")) ||
                            (mNewDevice && charUuid.equals("0000ecc0-0000-1000-8000-00805f9b34fb"))) {
                        final int charaProp = gattCharacteristic.getProperties();
                        if (((charaProp & BluetoothGattCharacteristic.PROPERTY_WRITE) |
                                (charaProp & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) > 0) {
                            /*  gains:\
                                old -> {0.5:0b111, 1:0b000, 2:0b001, 4:0b010, 8:0b011, 16:0b100, 32:0b101, 64:0b110}
                                new -> {1:0b00, 2:0b01, 4:0b10, 8:0b11}
                             */
                            final byte[] newValue;
                            if (!mNewDevice) {
                                newValue = new byte[6];
                                switch (selected_gain) {
                                    case "0.5":
                                        newValue[4] = 0b111;
                                        break;
                                    case "1":
                                        newValue[4] = 0b000;
                                        break;
                                    case "2":
                                        newValue[4] = 0b001;
                                        break;
                                    case "4":
                                        newValue[4] = 0b010;
                                        break;
                                    case "8":
                                        newValue[4] = 0b011;
                                        break;
                                    case "16":
                                        newValue[4] = 0b100;
                                        break;
                                    case "32":
                                        newValue[4] = 0b101;
                                        break;
                                    case "64":
                                        newValue[4] = 0b110;
                                        break;
                                }
                            } else {
                                newValue = new byte[1];
                                // set bits 3 and 4 to 1 for real + dummy data: 0b00xx0000 -> x to 1
                                // set only bit 3 to 1 for dummy data only:     0b00x00000 -> x to 1
                                switch (selected_gain) {
                                    case "1":
                                        newValue[0] = (byte) 0b00000000;
                                        break;
                                    case "2":
                                        newValue[0] = (byte) 0b01000000;
                                        break;
                                    case "4":
                                        newValue[0] = (byte) 0b10000000;
                                        break;
                                    case "8":
                                        newValue[0] = (byte) 0b11000000;
                                        break;
                                }
                            }
                            int WRITECHAR_DELAY = 500;
                            final int TOGGLE_DELAY = 500;
                            handler.postDelayed(() -> {
                                gattCharacteristic.setValue(newValue);
                                mBluetoothLeService.writeCharacteristic(gattCharacteristic);
                                handler.postDelayed(this::toggleNotifying, TOGGLE_DELAY);
                            }, WRITECHAR_DELAY);
                        }
                    }
                }
            }
        }
    }

    private void readGattCharacteristic(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid;
        String charUuid;
        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            uuid = gattService.getUuid().toString();
            // for the new Traumschreiber the uuid is "00000ee6-0000-1000-8000-00805f9b34fb"
            if ((!mNewDevice && uuid.equals("a22686cb-9268-bd91-dd4f-b52d03d85593")) || (mNewDevice && uuid.equals("00000ee6-0000-1000-8000-00805f9b34fb"))) {
                List<BluetoothGattCharacteristic> gattCharacteristics =
                        gattService.getCharacteristics();
                // Loops through available Characteristics.
                for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                    final int charaProp = gattCharacteristic.getProperties();
                    charUuid = gattCharacteristic.getUuid().toString();
                    if ((!mNewDevice && charUuid.equals("faa7b588-19e5-f590-0545-c99f193c5c3e")) || (mNewDevice && charUuid.equals("0000e617-0000-1000-8000-00805f9b34fb"))) {
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                            if (mNotifyCharacteristic != null) {
                                mBluetoothLeService.setCharacteristicNotification(
                                        mNotifyCharacteristic, false);
                                mNotifyCharacteristic = null;
                            }
                            mNotifyCharacteristic = gattCharacteristic;
                            // hack for reconnection and in case of notification set to true
                            mBluetoothLeService.setCharacteristicNotification(
                                    gattCharacteristic, false);
                            // normal setCharNotification call
                            mBluetoothLeService.setCharacteristicNotification(
                                    gattCharacteristic, notifying);
                            if (notifying)
                                mBluetoothLeService.readCharacteristic(gattCharacteristic, mNewDevice);
                        }
                    }
                }
            }
        }
    }

    private void clearUI() {
        mCh1.setText("");
        mCh2.setText("");
        mCh3.setText("");
        mCh4.setText("");
        mCh5.setText("");
        mCh6.setText("");
        mCh7.setText("");
        mCh8.setText("");
        mDataResolution.setText(R.string.no_data);
        data_cnt = 0;
    }

    private void enableCheckboxes() {
        chckbx_ch1.setEnabled(true);
        chckbx_ch2.setEnabled(true);
        chckbx_ch3.setEnabled(true);
        chckbx_ch4.setEnabled(true);
        chckbx_ch5.setEnabled(true);
        chckbx_ch6.setEnabled(true);
        chckbx_ch7.setEnabled(true);
        chckbx_ch8.setEnabled(true);
    }

    private void disableCheckboxes() {
        chckbx_ch1.setEnabled(false);
        chckbx_ch2.setEnabled(false);
        chckbx_ch3.setEnabled(false);
        chckbx_ch4.setEnabled(false);
        chckbx_ch5.setEnabled(false);
        chckbx_ch6.setEnabled(false);
        chckbx_ch7.setEnabled(false);
        chckbx_ch8.setEnabled(false);
    }

    private List<Float> transData(int[] data) {
        // Conversion formula (old): V_in = X * 1.65V / (1000 * GAIN * PRECISION)
        // Conversion formula (new): V_in = X * (298 / (1000 * gain))
        float gain = Float.parseFloat(selected_gain);
        List<Float> data_trans = new ArrayList<>();
        if (!mNewDevice) { // old model
            pkgIDs.add((int) data_cnt); // store pkg ID
            float precision = 2048;
            float numerator = 1650;
            float denominator = gain * precision;
            for (int datapoint : data) data_trans.add((datapoint * numerator) / denominator);
        } else {
            pkgIDs.add(data[0]); // store pkg ID
            int[] dataNoID = new int[data.length - 1]; // array without the pkg id slot
            // copy the array without the pkg id
            System.arraycopy(data, 1, dataNoID, 0, data.length - 1);
//            for (int datapoint : dataNoID) data_trans.add((float) datapoint); // for testing raw data
            for (int datapoint : dataNoID) data_trans.add(datapoint * (298 / (1000000 * gain)));
        }
//        for (int datapoint : data) data_trans.add((float) datapoint); // for testing raw data
        return data_trans;
    }

    @SuppressLint("DefaultLocale")
    private void displayData(List<Float> data_microV) {
        if (data_microV != null) {
            // data format example: +01012 -00234 +01374 -01516 +01656 +01747 +00131 -00351
            StringBuilder trans = new StringBuilder();
            List<String> values = new ArrayList<>();
            for (Float value : data_microV) {
                if (value >= 0) trans.append("+");
                trans.append(String.format("%5.2f", value));
                values.add(trans.toString());
                trans = new StringBuilder();
            }
            mCh1.setText(values.get(0));
            mCh2.setText(values.get(1));
            mCh3.setText(values.get(2));
            mCh4.setText(values.get(3));
            mCh5.setText(values.get(4));
            mCh6.setText(values.get(5));
            if (!mNewDevice) {
                mCh7.setText(values.get(6));
                mCh8.setText(values.get(7));
            }
        }
    }

    private void setChart() {
        OnChartValueSelectedListener ol = new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry entry, Highlight h) {
                //entry.getData() returns null here
            }

            @Override
            public void onNothingSelected() {

            }
        };
        mChart = findViewById(R.id.layout_chart);
        mChart.setOnChartValueSelectedListener(ol);
        // enable description text
        mChart.getDescription().setEnabled(false);
        // enable touch gestures
        mChart.setTouchEnabled(true);
        // enable scaling and dragging
        mChart.setDragEnabled(true);
        mChart.setScaleEnabled(true);
        mChart.setDrawGridBackground(true);
        // if disabled, scaling can be done on x- and y-axis separately
        mChart.setPinchZoom(true);
        // set an alternative background color
        LineData data = new LineData();
        data.setValueTextColor(Color.BLACK);
        // add empty data
        mChart.setData(data);
        // get the legend (only possible after setting data)
        Legend l1 = mChart.getLegend();
        // modify the legend ...
        l1.setForm(Legend.LegendForm.LINE);
        l1.setTextColor(Color.BLACK);
        // set the y left axis
        YAxis leftAxis = mChart.getAxisLeft();
        leftAxis.setTextColor(Color.GRAY);
        leftAxis.setAxisMaximum(30f);
        leftAxis.setAxisMinimum(-30f);
        leftAxis.setLabelCount(13, true); // from -35 to 35, a label each 5 microV
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.WHITE);
        // disable the y right axis
        YAxis rightAxis = mChart.getAxisRight();
        rightAxis.setEnabled(false);
        // set the x bottom axis
        XAxis bottomAxis = mChart.getXAxis();
        bottomAxis.setLabelCount(5, true);
        bottomAxis.setValueFormatter(new MyXAxisValueFormatter());
        bottomAxis.setPosition(XAxis.XAxisPosition.TOP);
        bottomAxis.setGridColor(Color.WHITE);
        bottomAxis.setTextColor(Color.GRAY);
    }

    private LineDataSet createSet1(ArrayList<Entry> le, boolean show) {
        LineDataSet set1 = new LineDataSet(le, "Ch-1");
        set1.setAxisDependency(YAxis.AxisDependency.LEFT);
        set1.setColor(ch1_color);
        set1.setDrawCircles(false);
        set1.setLineWidth(1f);
        set1.setValueTextColor(ch1_color);
        set1.setVisible(show);
        return set1;
    }

    private LineDataSet createSet2(ArrayList<Entry> le, boolean show) {
        LineDataSet set2 = new LineDataSet(le, "Ch-2");
        set2.setAxisDependency(YAxis.AxisDependency.LEFT);
        set2.setColor(ch2_color);
        set2.setDrawCircles(false);
        set2.setLineWidth(1f);
        set2.setValueTextColor(ch2_color);
        set2.setVisible(show);
        return set2;
    }

    private LineDataSet createSet3(ArrayList<Entry> le, boolean show) {
        LineDataSet set3 = new LineDataSet(le, "Ch-3");
        set3.setAxisDependency(YAxis.AxisDependency.LEFT);
        set3.setColor(ch3_color);
        set3.setDrawCircles(false);
        set3.setLineWidth(1f);
        set3.setValueTextColor(ch3_color);
        set3.setVisible(show);
        return set3;
    }

    private LineDataSet createSet4(ArrayList<Entry> le, boolean show) {
        LineDataSet set4 = new LineDataSet(le, "Ch-4");
        set4.setAxisDependency(YAxis.AxisDependency.LEFT);
        set4.setColor(ch4_color);
        set4.setDrawCircles(false);
        set4.setLineWidth(1f);
        set4.setValueTextColor(ch4_color);
        set4.setVisible(show);
        return set4;
    }

    private LineDataSet createSet5(ArrayList<Entry> le, boolean show) {
        LineDataSet set5 = new LineDataSet(le, "Ch-5");
        set5.setAxisDependency(YAxis.AxisDependency.LEFT);
        set5.setColor(ch5_color);
        set5.setDrawCircles(false);
        set5.setLineWidth(1f);
        set5.setValueTextColor(ch5_color);
        set5.setVisible(show);
        return set5;
    }

    private LineDataSet createSet6(ArrayList<Entry> le, boolean show) {
        LineDataSet set6 = new LineDataSet(le, "Ch-6");
        set6.setAxisDependency(YAxis.AxisDependency.LEFT);
        set6.setColor(ch6_color);
        set6.setDrawCircles(false);
        set6.setLineWidth(1f);
        set6.setValueTextColor(ch6_color);
        set6.setVisible(show);
        return set6;
    }

    private LineDataSet createSet7(ArrayList<Entry> le, boolean show) {
        LineDataSet set7 = new LineDataSet(le, "Ch-7");
        set7.setAxisDependency(YAxis.AxisDependency.LEFT);
        set7.setColor(ch7_color);
        set7.setDrawCircles(false);
        set7.setLineWidth(1f);
        set7.setValueTextColor(ch7_color);
        set7.setVisible(show);
        return set7;
    }

    private LineDataSet createSet8(ArrayList<Entry> le, boolean show) {
        LineDataSet set8 = new LineDataSet(le, "Ch-8");
        set8.setAxisDependency(YAxis.AxisDependency.LEFT);
        set8.setColor(ch8_color);
        set8.setDrawCircles(false);
        set8.setLineWidth(1f);
        set8.setValueTextColor(ch8_color);
        set8.setVisible(show);
        return set8;
    }

    private void addEntries(final List<List<Float>> e_list) {
        adjustScale(e_list);
        final List<ILineDataSet> datasets = new ArrayList<>();  // for adding multiple plots
        float x = 0;
        float DATAPOINT_TIME = mNewDevice ? 4f : 4.5f;
        for (List<Float> f : e_list) {
            cnt++;
            x = cnt * DATAPOINT_TIME;
            lineEntries1.add(new Entry(x, f.get(0)));
            lineEntries2.add(new Entry(x, f.get(1)));
            lineEntries3.add(new Entry(x, f.get(2)));
            lineEntries4.add(new Entry(x, f.get(3)));
            lineEntries5.add(new Entry(x, f.get(4)));
            lineEntries6.add(new Entry(x, f.get(5)));
            if (!mNewDevice) {
                lineEntries7.add(new Entry(x, f.get(6)));
                lineEntries8.add(new Entry(x, f.get(7)));
            }
        }
        final float f_x = x;
        if (thread != null) thread.interrupt();
        final Runnable runnable = () -> {
            LineDataSet set1 = createSet1(lineEntries1, show_ch1);
            datasets.add(set1);
            LineDataSet set2 = createSet2(lineEntries2, show_ch2);
            datasets.add(set2);
            LineDataSet set3 = createSet3(lineEntries3, show_ch3);
            datasets.add(set3);
            LineDataSet set4 = createSet4(lineEntries4, show_ch4);
            datasets.add(set4);
            LineDataSet set5 = createSet5(lineEntries5, show_ch5);
            datasets.add(set5);
            LineDataSet set6 = createSet6(lineEntries6, show_ch6);
            datasets.add(set6);
            LineDataSet set7 = createSet7(lineEntries7, show_ch7);
            datasets.add(set7);
            LineDataSet set8 = createSet8(lineEntries8, show_ch8);
            datasets.add(set8);
            LineData linedata = new LineData(datasets);
            linedata.notifyDataChanged();
            mChart.setData(linedata);
            mChart.notifyDataSetChanged();
            // limit the number of visible entries
            mChart.setVisibleXRangeMaximum(MAX_VISIBLE);
            // move to the latest entry
            mChart.moveViewToX(f_x);
        };
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(runnable);
            }
        });
        thread.start();
        // max time range in ms (x value) to store on plot
        int PLOT_MEMO = 3000;
        if (x > PLOT_MEMO) {
            for (int j = 0; j < e_list.size(); j++) {
                for (int i = 0; i < mChart.getData().getDataSetCount(); i++) {
                    mChart.getData().getDataSetByIndex(i).removeFirst();
                }
            }
        }
    }

    /**
     * adjusts the scale according to the maximal and minimal value of the data given in
     *
     * @param e_list
     */
    private void adjustScale(final List<List<Float>> e_list) {
        if (recentlyDisplayedData == null) {
            recentlyDisplayedData = new ArrayList<>();
        }
        if (recentlyDisplayedData.size() > 50 * e_list.size())
            recentlyDisplayedData = recentlyDisplayedData.subList(e_list.size(), recentlyDisplayedData.size());
        for (List<Float> innerList : e_list) {
            recentlyDisplayedData.add(innerList);
        }
        int max = 0;
        int min = 0;
        for (List<Float> innerList : recentlyDisplayedData) {
            int channel = 0;
            for (Float entry : innerList) {
                if ((show_ch1 && channel == 0) || (show_ch2 && channel == 1) || (show_ch3 && channel == 2) || (show_ch4 && channel == 3) ||
                        (show_ch5 && channel == 4) || (show_ch6 && channel == 5) || (show_ch7 && channel == 6) || (show_ch8 && channel == 7)) {
                    if (entry > max) {
                        max = entry.intValue();
                    }
                    if (entry < min) {
                        min = entry.intValue();
                    }
                }
                channel++;
            }
        }
        // include this part to make the axis symmetric (0 always visible in the middle)
        if (max < min * -1) max = min * -1;
        min = max * -1;

        int range = max - min;
        max += 0.1 * range;
        min -= 0.1 * range;
        YAxis leftAxis = mChart.getAxisLeft();
        leftAxis.setAxisMaximum(max);
        leftAxis.setAxisMinimum(min);
    }

    //Starts a recording session
    @SuppressLint({"SimpleDateFormat", "SetTextI18n"})
    private void startTrial() {
        cnt = 0;
        main_data = new ArrayList<>();
        start_time = new SimpleDateFormat("HH:mm:ss.SSS").format(new Date());
        start_timestamp = new Timestamp(start_watch).getTime();
        recording = true;
    }

    //Finish a recording session
    @SuppressLint("SimpleDateFormat")
    private void endTrial() {
        recording = false;
        end_time = new SimpleDateFormat("HH:mm:ss.SSS").format(new Date());
        long stop_watch = System.currentTimeMillis();
        end_timestamp = new Timestamp(stop_watch).getTime();
        recording_time = Long.toString(stop_watch - start_watch);
    }


    //Stores data while session is running
    private void storeData(List<Float> data_microV) {
        if (dp_received.size() == 0) start_watch = System.currentTimeMillis();
        float[] f_microV = new float[data_microV.size()];
        float curr_received = System.currentTimeMillis() - start_watch;
        dp_received.add(curr_received);
        int i = 0;
        for (Float f : data_microV)
            f_microV[i++] = (f != null ? f : Float.NaN); // Or whatever default you want
        main_data.add(f_microV);
    }

    private void saveSession() {
        saveSession("default");
    }

    //Saves the data at the end of session
    private void saveSession(final String tag) {
        final String username = getSharedPreferences("userPreferences", 0).getString("username", "user");
        final String userID = getSharedPreferences("userPreferences", 0).getString("userID", "12345678");
        final String top_header = "Username, User ID, Session ID,Session Tag,Date,Shape (rows x columns)," +
                "Duration (ms),Starting Time,Ending Time,Resolution (ms),Resolution (Hz)," +
                "Unit Measure,Starting Timestamp,Ending Timestamp";
        final String dp_header = "Pkg ID,Time,Ch-1,Ch-2,Ch-3,Ch-4,Ch-5,Ch-6,Ch-7,Ch-8";
        final UUID id = UUID.randomUUID();
        @SuppressLint("SimpleDateFormat") final String date = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss").format(new Date());
        final char delimiter = ',';
        final char break_line = '\n';
        new Thread(() -> {
            try {
                File formatted = new File(MainActivity.getDirSessions(),
                        date + "_" + tag + ".csv");
                // if file doesn't exists, then create it
                if (!formatted.exists()) //noinspection ResultOfMethodCallIgnored
                    formatted.createNewFile();
                FileWriter fileWriter = new FileWriter(formatted);
                int rows = main_data.size();
                int cols = main_data.get(0).length;
                fileWriter.append(top_header);
                fileWriter.append(break_line);
                fileWriter.append(username);
                fileWriter.append(delimiter);
                fileWriter.append(userID);
                fileWriter.append(delimiter);
                fileWriter.append(id.toString());
                fileWriter.append(delimiter);
                fileWriter.append(tag);
                fileWriter.append(delimiter);
                fileWriter.append(date);
                fileWriter.append(delimiter);
                fileWriter.append(String.valueOf(rows)).append("x").append(String.valueOf(cols));
                fileWriter.append(delimiter);
                fileWriter.append(recording_time);
                fileWriter.append(delimiter);
                fileWriter.append(start_time);
                fileWriter.append(delimiter);
                fileWriter.append(end_time);
                fileWriter.append(delimiter);
                fileWriter.append(String.valueOf(res_time));
                fileWriter.append(delimiter);
                fileWriter.append(String.valueOf(res_freq));
                fileWriter.append(delimiter);
                fileWriter.append("µV");
                fileWriter.append(delimiter);
                fileWriter.append(Long.toString(start_timestamp));
                fileWriter.append(delimiter);
                fileWriter.append(Long.toString(end_timestamp));
                fileWriter.append(delimiter);
                fileWriter.append(break_line);
                fileWriter.append(dp_header);
                fileWriter.append(break_line);
                for (int i = 0; i < rows; i++) {
                    fileWriter.append(String.valueOf(pkgIDs.get(i)));
                    fileWriter.append(delimiter);
                    fileWriter.append(String.valueOf(dp_received.get(i)));
                    fileWriter.append(delimiter);
                    for (int j = 0; j < cols; j++) {
                        fileWriter.append(String.valueOf(main_data.get(i)[j]));
                        fileWriter.append(delimiter);
                    }
                    fileWriter.append(break_line);
                }
                fileWriter.flush();
                fileWriter.close();
            } catch (Exception e) {
                Log.e(TAG, "Error storing the data into a CSV file: " + e);
            }
        }).start();
    }

    private void buttons_nodata() {
        imageButtonRecord.setImageResource(R.drawable.ic_fiber_manual_record_pink_24dp);
        imageButtonRecord.setEnabled(false);
        imageButtonSave.setImageResource(R.drawable.ic_save_gray_24dp);
        imageButtonSave.setEnabled(false);
        imageButtonDiscard.setImageResource(R.drawable.ic_delete_gray_24dp);
        imageButtonDiscard.setEnabled(false);
    }

    private void buttons_prerecording() {
        imageButtonRecord.setImageResource(R.drawable.ic_fiber_manual_record_red_24dp);
        imageButtonRecord.setEnabled(true);
        imageButtonSave.setImageResource(R.drawable.ic_save_gray_24dp);
        imageButtonSave.setEnabled(false);
        imageButtonDiscard.setImageResource(R.drawable.ic_delete_gray_24dp);
        imageButtonDiscard.setEnabled(false);
    }

    private void buttons_recording() {
        imageButtonRecord.setImageResource(R.drawable.ic_stop_black_24dp);
        imageButtonSave.setImageResource(R.drawable.ic_save_gray_24dp);
        imageButtonSave.setEnabled(false);
        imageButtonDiscard.setImageResource(R.drawable.ic_delete_gray_24dp);
        imageButtonDiscard.setEnabled(false);
    }

    private void buttons_postrecording() {
        imageButtonRecord.setImageResource(R.drawable.ic_fiber_manual_record_pink_24dp);
        imageButtonRecord.setEnabled(true);
        imageButtonSave.setEnabled(true);
        imageButtonSave.setImageResource(R.drawable.ic_save_black_24dp);
        imageButtonDiscard.setEnabled(true);
        imageButtonDiscard.setImageResource(R.drawable.ic_delete_black_24dp);
    }

    private void setConnectionStatus(boolean connected) {
        MenuItem menuItem = menu.findItem(R.id.scan);
        MenuItem menuItemNotify = menu.findItem(R.id.notify);
        MenuItem menuItemCast = menu.findItem(R.id.cast);
        if (connected) {
            menuItem.setIcon(R.drawable.ic_bluetooth_connected_blue_24dp);
            mConnectionState.setText(R.string.device_connected);
            mConnectionState.setTextColor(Color.GREEN);
            switch_plots.setEnabled(true);
            gain_spinner.setEnabled(true);
            viewDeviceAddress.setText(mDeviceAddress);
            menuItemNotify.setVisible(true);
            menuItemCast.setVisible(true);
        } else {
            menuItem.setIcon(R.drawable.ic_bluetooth_searching_white_24dp);
            mConnectionState.setText(R.string.no_device);
            mConnectionState.setTextColor(Color.LTGRAY);
            buttons_nodata();
            switch_plots.setEnabled(false);
            gain_spinner.setEnabled(false);
            viewDeviceAddress.setText(R.string.no_address);
            menuItemNotify.setVisible(false);
            menuItemCast.setVisible(false);
        }
    }


    class CastThread extends Thread {
        String IP = getSharedPreferences("userPreferences", 0).getString("IP", getResources().getString(R.string.default_IP));
        String PORT = getSharedPreferences("userPreferences", 0).getString("port", getResources().getString(R.string.default_port));
        // best way found until now to encode the values, a stringified JSON. Looks like:
        JSONObject toSend = new JSONObject();
//        private volatile boolean exit = false;
        // {'pkg': 1, 'time': 1589880540884, '1': -149.85352, '2': -18.530273, '3': 191.74805, '4': -305.34668, '5': 0, '6': -142.60254, '7': -1.6113281, '8': -29.80957}

        public void run() {
            try {
                WSClient c = new WSClient(new URI("ws://" + IP + ":" + PORT));
                c.setReuseAddr(true);
                // c.setConnectionLostTimeout(0); // default is 60 seconds
                // TODO: check if TCP_NODELAY improves speed, also .connect() vs .connectBlocking()
                // TODO: Add connect/disconnect control by cast button pressed and message received
                c.setTcpNoDelay(true);
                c.connectBlocking();
                int pkg = 0;
                List<Float> lastV = null; // store last octet of EEG values
                while (c.isOpen()) {
                    if (microV != null && lastV != microV) {
                        toSend = new JSONObject();
                        // timestamp in milliseconds since January 1, 1970, 00:00:00 GMT
                        long time = new Date().getTime();
                        toSend.put("pkg", pkg); // add pkg number
                        toSend.put("time", time); // add time
                        for (int i = 0; i < microV.size(); i++) {
                            // add voltage amplitudes
                            toSend.put(Integer.toString(i + 1), microV.get(i));
                        }
                        c.send(toSend.toString());
                        lastV = microV; // store current as last
                        pkg++; // increase package counter
//                        Log.d("WS", "Sent: " + toSend.toString());
                    }
                }
            } catch (URISyntaxException | JSONException | InterruptedException e) {
                e.printStackTrace();
                Log.d("WS", "URI error:" + e);
            }
        }

        public void staph() {

            Log.d("CastThread", "Stopped");
//            exit = true;
            if (out != null) {
                out.close();
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }


}
