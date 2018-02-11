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

import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import static android.content.ContentValues.TAG;

/**
 * Created by bruml on 10.02.2018.
 */

/**
 * Global application class, starts the local service which must be able to be running without
 * interruption and be not depended on any activity state or configuration change.
 */
public class GlobalApplication extends Application {
    private static final int NTF_ID = 1;
    private static final String NTF_CHANNEL = "name.brumla.bluetoothbutton";

    private static GlobalApplication instance;
    private Intent remoteButtonServiceIntent = null;
    private RemoteButtonReceiverService remoteButtonReceiverService = null;
    private RemoteButtonReceiverService.ReceivedDataListener receivedDataListener = null;
    private NotificationManager notificationManager;

    /**
     * Bound service connection
     */
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.i(TAG, "onServiceConnected: Service connected");
            remoteButtonReceiverService = ((RemoteButtonReceiverService.RemoteButtonReceiverBinder) iBinder).getService();

            remoteButtonReceiverService.setReceivedDataListener((e) -> {
                Log.i(TAG, "onServiceConnected: Received event: " + e.message);

                if (receivedDataListener != null) {
                    receivedDataListener.onReceiveDataEvent(e);
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.i(TAG, "onServiceConnected: Service disconnected");
            remoteButtonReceiverService = null;
        }
    };

    /**
     * Return this instance, GlobalApplication is singleton.
     * @return
     */
    public static GlobalApplication getInstance() {
        return instance;
    }

    /**
     * Sets the received data listener. The listener is activated after the bound service is started.
     * @param receivedDataListener
     */
    public void setReceivedDataListener(RemoteButtonReceiverService.ReceivedDataListener receivedDataListener) {
        this.receivedDataListener = receivedDataListener;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate: *********** CREATED ***********");
        instance = this;

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        restartRemoteButtonService();
    }

    /**
     * Publishes the message as Android status message
     * @param message
     */
    public void publishNotification(String message) {
        Notification ntf = new NotificationCompat.Builder(getApplicationContext(), NTF_CHANNEL)
                .setSmallIcon(android.R.drawable.ic_menu_revert)
                .setContentTitle("Bluetooth Button")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setShowWhen(true)
                .build();
        notificationManager.notify(NTF_ID, ntf);
    }

    /**
     * Restarts the bound service
     */
    public void restartRemoteButtonService() {
        Log.i(TAG, "restartRemoteButtonService: Restart requested");
        Notification ntf = new NotificationCompat.Builder(getApplicationContext(), NTF_CHANNEL)
                .setSmallIcon(android.R.drawable.ic_menu_revert)
                .setTicker("Restart")
                .setContentTitle("Bluetooth Button")
                .setContentText("Bluetooth Button Service is running")
                .setTimeoutAfter(3000)
                .setShowWhen(true)
                .build();
        notificationManager.notify(NTF_ID, ntf);

        if (remoteButtonServiceIntent != null) {
            unbindService(serviceConnection);
            remoteButtonServiceIntent = null;
        }

        remoteButtonServiceIntent = new Intent(getApplicationContext(), RemoteButtonReceiverService.class);
        Log.i(TAG, "restartRemoteButtonService: Intent=" + remoteButtonServiceIntent);
        boolean isBound = bindService(remoteButtonServiceIntent, serviceConnection, BIND_AUTO_CREATE);
        Log.i(TAG, "restartRemoteButtonService: Service is running=" + isBound);
    }
}
