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


import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;

import static android.content.ContentValues.TAG;
import static name.brumla.bluetoothbutton.SetupActivity.PREF_BLUETOOTH_ADDRESS;
import static name.brumla.bluetoothbutton.SetupActivity.PREF_BLUETOOTH_DEVICE;
import static name.brumla.bluetoothbutton.SetupActivity.PREF_FILE;

/**
 * Created by bruml on 10.02.2018.
 */

public class RemoteButtonReceiverService extends Service {
    private RemoteButtonReceiverBinder binder = new RemoteButtonReceiverBinder();
    private ButtonListenerAsyncTask listenerAsyncTask;

    public ReceivedDataListener getReceivedDataListener() {
        return receivedDataListener;
    }

    private ReceivedDataListener receivedDataListener;

    public RemoteButtonReceiverService() {
    }

    public void setReceivedDataListener(ReceivedDataListener receivedDataListener) {
        this.receivedDataListener = receivedDataListener;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind: Got bind request");
        SharedPreferences preferences = getSharedPreferences(PREF_FILE, 0);
        String device = preferences.getString(PREF_BLUETOOTH_DEVICE, "");
        String address = preferences.getString(PREF_BLUETOOTH_ADDRESS, "");

        GlobalApplication.getInstance().publishNotification("Initializing service");

        listenerAsyncTask = new ButtonListenerAsyncTask();
        listenerAsyncTask.execute(address);

        return binder;
    }

    protected void fireReceivedDataEvent(ReceivedDataEvent e) {
        Log.i(TAG, "fireReceivedDataEvent: Received event from async task");
        if(receivedDataListener != null) {
            receivedDataListener.onReceiveDataEvent(e);
        }
    }

    public interface ReceivedDataListener {
        void onReceiveDataEvent(ReceivedDataEvent e);
    }

    public class ReceivedDataEvent {
        public String message;
        public Long bytesTotal;

        public ReceivedDataEvent(String message) {
            this.message = message;
        }

        public ReceivedDataEvent(String message, Long bytesTotal) {
            this.message = message;
            this.bytesTotal = bytesTotal;
        }
    }

    public class RemoteButtonReceiverBinder extends Binder {
        public RemoteButtonReceiverService getService() {
            Log.i(TAG, "getService: Binder ready");
            return RemoteButtonReceiverService.this;
        }
    }

    /**
     * Main background service, it fetches data from the remote BT device and when the complete
     * string is received (CRLF terminated), the event is fired. When the connection is establishing,
     * the all parcel UUIDs are used as the parameter to avoid any deep configuration steps.
     */
    class ButtonListenerAsyncTask extends AsyncTask<String, ReceivedDataEvent, Object> {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

        @Override
        protected Object doInBackground(String... strings) {
            Log.i(TAG, "doInBackground: Background service started: " + strings[0]);
            BluetoothDevice device = adapter.getRemoteDevice(strings[0]);

            InputStream ios = null;
            for (ParcelUuid uuid : device.getUuids()) {
                Log.i(TAG, "doInBackground: UUID=" + uuid.getUuid().toString());
                try {
                    BluetoothSocket socket = device.createInsecureRfcommSocketToServiceRecord(uuid.getUuid());
                    socket.connect();
                    ios = socket.getInputStream();
                    if (ios != null) {
                        break;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            adapter.cancelDiscovery();
            StringBuilder builder = new StringBuilder();

            long bytes = 0;

            if (ios != null) {
                try {
                    while (!isCancelled()) {
                        if (ios.available() > 0) {
                            int data = ios.read();
                            bytes ++;
                            char recv = (char) data;

                            if (recv < ' ') {
                                String str = builder.toString();
                                Log.i(TAG, "doInBackground: End of message: " + str);

                                if(!str.isEmpty()) {
                                    publishProgress(new ReceivedDataEvent(str, bytes));
                                }

                                builder.setLength(0);
                            } else {
                                builder.append(recv);
                            }

                        }
                    }
                    ios.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            Log.i(TAG, "doInBackground: *** SERVICE STOPPED ***");

            return null;
        }

        @Override
        protected void onProgressUpdate(ReceivedDataEvent... values) {
            RemoteButtonReceiverService.this.fireReceivedDataEvent(values[0]);
        }

        @Override
        protected void onPostExecute(Object o) {
            Toast.makeText(getApplicationContext(), "The background service was stopped!", Toast.LENGTH_LONG).show();
        }
    }
}
