package name.brumla.bluetoothbutton;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Date;

/**
 * Main activity. Displays some useful information about the bluetooth connection and waits for
 * the dialer message to start the phone call intent.
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    long lastRecBytesCount = 0;
    String lastMessage = "";

    private EditText etLastPing;
    private ImageView ivBTOffline;
    private ImageView ivBTOnline;
    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etLastPing = findViewById(R.id.etLastPing);
        ivBTOffline = findViewById(R.id.ivBTOffline);
        ivBTOnline = findViewById(R.id.ivBTOnline);
        tvStatus = findViewById(R.id.tvStatus);

        ivBTOnline.setVisibility(ImageView.INVISIBLE);
        tvStatus.setText(R.string.msg_waiting_for_signal);

        // set up the received data listener, if the ping is received then display the message in
        // the status bar per cca 100 bytes received. Check the received message and if the
        // phone call request is received, do the call.
        /*
         TODO Implement phone call, only one call at one time to be possible, no caching or queue
         TODO of the call is allowed
          */
        GlobalApplication.getInstance().setReceivedDataListener((e) -> {
            ivBTOffline.setVisibility(ImageView.INVISIBLE);
            ivBTOnline.setVisibility(ImageView.VISIBLE);
            tvStatus.setText(R.string.msg_connected_to_br);

            etLastPing.setText(new Date().toString());

            if(lastRecBytesCount + 100 < e.bytesTotal) {
                GlobalApplication.getInstance().publishNotification(String.format("Received %d B", lastRecBytesCount));
                lastRecBytesCount = e.bytesTotal;
            }

            if(!lastMessage.equalsIgnoreCase(e.message)) {
                lastMessage = e.message;
                Log.i(TAG, "onCreate: Received message: " + lastMessage);
                if(e.message.equalsIgnoreCase("K#R")) {
                    tvStatus.setText(R.string.msg_phone_alert);
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuPreferences:
                Intent intent = new Intent(this, SetupActivity.class);
                startActivity(intent);
                break;
            case R.id.menuReloadService:
                ivBTOffline.setVisibility(ImageView.VISIBLE);
                ivBTOnline.setVisibility(ImageView.INVISIBLE);
                tvStatus.setText(R.string.msg_waiting_for_signal);

                Toast.makeText(this, R.string.toast_reload_service, Toast.LENGTH_SHORT).show();
                GlobalApplication.getInstance().restartRemoteButtonService();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }
}
