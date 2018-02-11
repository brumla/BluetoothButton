package name.brumla.bluetoothbutton;

/*
    This file is part of BluetoothButton.

    BluetoothButton is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    BluetoothButton is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with BluetoothButton.  If not, see <http://www.gnu.org/licenses/>.
 */

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Connected bluetooth device detail activity. Shows the complete details including the ParcelUUID
 * and provides basic "ping" functionality.
 */
public class BluetoothDeviceDetailActivity extends AppCompatActivity {
    public static final String DEVICE_ADDRESS = "DEVICE_ADDRESS";
    public static final String DEVICE_NAME = "DEVICE_NAME";

    private EditText etDeviceName;
    private EditText etDeviceAddress;
    private EditText etDeviceType;
    private EditText etBondState;
    private ListView lvParcelUUIDs;
    private ProgressBar progressBar;

    private String deviceName;
    private String deviceAddress;
    private ArrayAdapter<String> parcelUuidAdapter;
    private List<String> parcels = new ArrayList<>();
    private BluetoothDevice currentDevice;

    private PingAsyncTask asyncTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_device_detail);

        deviceAddress = getIntent().getStringExtra(DEVICE_ADDRESS).trim();
        deviceName = getIntent().getStringExtra(DEVICE_NAME).trim();

        etBondState = findViewById(R.id.etBondState);
        etDeviceAddress = findViewById(R.id.etDeviceAddress);
        etDeviceName = findViewById(R.id.etDeviceName);
        etDeviceType = findViewById(R.id.etDeviceType);
        lvParcelUUIDs = findViewById(R.id.lvParcelUUIDs);
        progressBar = findViewById(R.id.progressBar);

        parcelUuidAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, parcels);
        lvParcelUUIDs.setAdapter(parcelUuidAdapter);

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        for (BluetoothDevice bluetoothDevice : adapter.getBondedDevices()) {
            if (bluetoothDevice.getAddress().equalsIgnoreCase(deviceAddress)) {
                currentDevice = bluetoothDevice;
                etDeviceName.setText(bluetoothDevice.getName());
                etDeviceAddress.setText(bluetoothDevice.getAddress());

                String bondState = "";
                switch (bluetoothDevice.getBondState()) {
                    case BluetoothDevice.BOND_NONE:
                        bondState = "None";
                        break;
                    case BluetoothDevice.BOND_BONDED:
                        bondState = "Bonded";
                        break;
                    case BluetoothDevice.BOND_BONDING:
                        bondState = "Bonding";
                        break;
                    default:
                        break;
                }
                etBondState.setText(bondState);

                for (ParcelUuid it : bluetoothDevice.getUuids()) {
                    parcels.add(it.getUuid().toString());
                }
                parcelUuidAdapter.notifyDataSetChanged();
            }
        }
        ;

        adapter.cancelDiscovery();

    }

    public void tryToPingClicked(View view) {
        asyncTask = new PingAsyncTask();
        asyncTask.execute(currentDevice);
    }

    /**
     * Ping-like function written as asynchronous task, it tries to use all the ParcelUUIDs to
     * access the remote device.
     */
    class PingAsyncTask extends AsyncTask<BluetoothDevice, Integer, Object> {
        private static final int PR_IOS_CONNECTED = 1;
        private static final int PR_IOS_AVAILABLE = 2;
        private static final int PR_IOS_READ = 3;
        private static final int PR_IOS_CLOSED = 4;
        private static final int PR_IOS_ERR_WRONG_UUID_CONNECT = 1001;
        private static final int PR_IOS_ERR_CANNOT_READ = 1002;
        private static final String TAG = "PingAsyncTask";

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

        @Override
        protected Object doInBackground(BluetoothDevice... bluetoothDevices) {
            BluetoothDevice device = adapter.getRemoteDevice(bluetoothDevices[0].getAddress());

            InputStream ios = null;
            for (ParcelUuid uuid : device.getUuids()) {
                try {
                    BluetoothSocket socket = device.createInsecureRfcommSocketToServiceRecord(uuid.getUuid());
                    socket.connect();
                    ios = socket.getInputStream();
                    if (ios != null) {
                        publishProgress(PR_IOS_CONNECTED);
                        break;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    publishProgress(PR_IOS_ERR_WRONG_UUID_CONNECT);
                }
            }

            adapter.cancelDiscovery();

            if (ios != null) {
                try {
                    if (ios.available() > 0) {
                        publishProgress(PR_IOS_AVAILABLE);
                        int read = ios.read();
                        Log.i(TAG, "doInBackground: Byte read: " + read);
                        publishProgress(PR_IOS_READ);
                    }
                    ios.close();
                    publishProgress(PR_IOS_CLOSED);
                } catch (IOException e) {
                    e.printStackTrace();
                    publishProgress(PR_IOS_ERR_CANNOT_READ);
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            progressBar.setIndeterminate(false);
        }

        @Override
        protected void onPreExecute() {
            progressBar.setIndeterminate(true);
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            switch (values[0]) {
                case PR_IOS_CONNECTED:
                    break;
                case PR_IOS_AVAILABLE:
                    break;
                case PR_IOS_CLOSED:
                    Toast.makeText(BluetoothDeviceDetailActivity.this, "Success. Byte was read.", Toast.LENGTH_LONG).show();
                    break;
                case PR_IOS_READ:
                    break;
                case PR_IOS_ERR_CANNOT_READ:
                    Toast.makeText(BluetoothDeviceDetailActivity.this, "Cannot read from device.", Toast.LENGTH_LONG).show();
                    break;
                case PR_IOS_ERR_WRONG_UUID_CONNECT:
                    Toast.makeText(BluetoothDeviceDetailActivity.this, "Cannot connect using UUID connection", Toast.LENGTH_LONG).show();
                    break;
                default:
            }
        }
    }
}
