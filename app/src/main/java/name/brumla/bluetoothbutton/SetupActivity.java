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
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Setup activity, sets up the target phone number and remote bluetooth device.
 */
public class SetupActivity extends AppCompatActivity {
    public static final String PREF_BLUETOOTH_ADDRESS = "PREF_BLUETOOTH_ADDRESS";
    public static final String PREF_BLUETOOTH_DEVICE = "PREF_BLUETOOTH_DEVICE";
    public static final String PREF_PHONE_NUMBER = "PREF_PHONE_NUMBER";
    public static final String PREF_FILE = "BLUETOOTH_BUTTON";

    private ListView lvDevices;
    private EditText etSelectedBtDevice;

    private ArrayAdapter<String> bluetoothAdapters;
    private List<String> btDevices = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        lvDevices = findViewById(R.id.lvDevices);
        bluetoothAdapters = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, btDevices);

        bluetoothAdapters.notifyDataSetChanged();

        etSelectedBtDevice = findViewById(R.id.etSelectedBtDevice);

        lvDevices.setAdapter(bluetoothAdapters);
        lvDevices.setOnItemClickListener((adapterView, view, position, id) -> {
            etSelectedBtDevice.setText(btDevices.get(position));
        });

        loadPreferences();
    }

    private void loadPreferences() {
        SharedPreferences preferences = getSharedPreferences(PREF_FILE, 0);
        String device = preferences.getString(PREF_BLUETOOTH_DEVICE, "");
        String address = preferences.getString(PREF_BLUETOOTH_ADDRESS, "");

        if(device.isEmpty() || address.isEmpty()) {
            Toast.makeText(this, R.string.err_no_device_setup, Toast.LENGTH_SHORT).show();
            etSelectedBtDevice.setText("");
            return;
        }

        etSelectedBtDevice.setText(String.format("%s; %s", device, address));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.settings_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuSavePrefs:
                savePreferences();
                break;
            case R.id.menu_refresh_bt:
                refreshAllBTDevices();
                break;
            default:
                super.onOptionsItemSelected(item);
        }
        return true;
    }

    private void savePreferences() {
        String devInfo = etSelectedBtDevice.getText().toString();

        if (devInfo == null || devInfo.trim().isEmpty() || devInfo.trim().indexOf(";") < 0) {
            Toast.makeText(this, R.string.err_preferences_cannot_store_empty_btinfo, Toast.LENGTH_LONG).show();
            return;
        }

        String[] keyVal = devInfo.split(";");

        SharedPreferences preferences = getSharedPreferences(PREF_FILE, 0);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(PREF_BLUETOOTH_ADDRESS, keyVal[1].trim());
        editor.putString(PREF_BLUETOOTH_DEVICE, keyVal[0].trim());
        editor.commit();
    }

    private void refreshAllBTDevices() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        btDevices.clear();
        for (BluetoothDevice bluetoothDevice : adapter.getBondedDevices()) {
            btDevices.add(bluetoothDevice.getName() + "; " + bluetoothDevice.getAddress());
        }
        ;

        bluetoothAdapters.notifyDataSetChanged();
        adapter.cancelDiscovery();
    }

    public void openDeviceDetailActivity(View view) {
        String btDevice = etSelectedBtDevice.getText().toString();
        if (btDevice == null || btDevice.trim().isEmpty() || btDevice.indexOf(';') < 0) {
            return;
        }

        String[] nameAddress = btDevice.trim().split(";");

        Intent intent = new Intent(this, BluetoothDeviceDetailActivity.class);
        intent.putExtra(BluetoothDeviceDetailActivity.DEVICE_NAME, nameAddress[0]);
        intent.putExtra(BluetoothDeviceDetailActivity.DEVICE_ADDRESS, nameAddress[1]);
        startActivity(intent);
    }
}
