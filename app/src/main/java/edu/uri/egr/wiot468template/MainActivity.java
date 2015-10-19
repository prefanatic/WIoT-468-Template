package edu.uri.egr.wiot468template;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothProfile;
import android.os.Bundle;
import android.widget.Switch;
import android.widget.TextView;

import butterknife.Bind;
import butterknife.OnClick;
import edu.uri.egr.hermes.manipulators.FileLog;
import edu.uri.egr.hermesble.HermesBLE;
import edu.uri.egr.hermesble.attributes.RBLGattAttributes;
import edu.uri.egr.hermesble.ui.BLESelectionDialog;
import edu.uri.egr.hermesui.activity.HermesActivity;
import rx.Subscription;
import timber.log.Timber;

/**
 * Created by cody on 10/8/15.
 */
public class MainActivity extends HermesActivity {
    public static final String UART_SERVICE = RBLGattAttributes.BLE_SHIELD_SERVICE;
    public static final String UART_RX = RBLGattAttributes.BLE_SHIELD_RX;
    public static final String UART_TX = RBLGattAttributes.BLE_SHIELD_TX;

    // Create Activity global variables for things we need across different methods.
    private BluetoothGatt mGatt;
    private Subscription mDeviceSubscription;

    // Access the views from our layout.
    @Bind(R.id.control_button) Switch mControlButton;
    @Bind(R.id.analog_value) TextView mTextValue;
    @Bind(R.id.heart_value) TextView mHeartRate;

    // Hook into our control button, and allow us to run code when one clicks on it.
    @OnClick(R.id.control_button)
    public void onControlClicked() {
        // Create a byte package to send over to the nano.
        byte[] data = new byte[]{(byte) 0xA0, (byte) 0x00, (byte) 0x00};

        // Trigger the value we send.  This is a toggle button - so whenever we're running, shut off.  Whenever we're off, turn on.
        if (mControlButton.isChecked())
            data[1] = (byte) 0x01; // Send a value of 1 if we're enabled.

        // Finally, write the value!
        HermesBLE.write(mGatt, UART_SERVICE, UART_TX, data);
    }

    /*
    This is called when the Activity is created.  In Android, this will be when the activity is started fresh
    for the first time, or when the screen is rotated.  Pressing the back button will cause the view to be
    destroyed, but pressing home, and then using multitasking to get back, will not (most of the time)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create the log file.
        FileLog heartRateLog = new FileLog("wiot-heart-rate.csv");
        heartRateLog.setHeaders("Date", "Time", "Heart Rate");

        FileLog analogLog = new FileLog("wiot-analog.csv");
        analogLog.setHeaders("Date", "Time", "Analog Value");

        BLESelectionDialog dialog = new BLESelectionDialog();
        // Now, we need to subscribe to it.  This might look like black magic, but just follow the comments.
        mDeviceSubscription = dialog.getObservable() // Get the Observable from the device dialog.

                // We'll want to close our activity if we don't select any devices.
                //.doOnCompleted(() -> { if (mGatt == null) finish(); })

                // Once we get the device, we hit this flatMap.  Using flatMap, we can convert this device into a connection.
                .flatMap(HermesBLE::connect)

                        // Only continue if our connection event type is STATE_CONNECTED
                .filter(event -> event.type == BluetoothProfile.STATE_CONNECTED)

                        // After the above runs, we'll be connected.  So, the first "event" we get will be a success.
                        // Lets take out the BluetoothGatt from this event and save it.  We'll need it to clean up later.
                .doOnNext(event -> mGatt = event.gatt)

                .doOnNext(event -> HermesBLE.getServices(event.gatt)
                        .doOnCompleted(() -> mControlButton.setEnabled(true))
                        .subscribe())

                        // At this point, we've got a connection event, and we want to transform it into a characteristic event.
                        // We do that, again, by running flatMap on the connection event.  We receive the characteristic in return.
                .flatMap(event -> HermesBLE.listen(event.gatt, UART_SERVICE, UART_RX))

                        // We want to transform the byte values we get from the UART event into something readable.
                        // UartEvaluator will take the flag for what type of event it is, and the data associated with it, and return it as UartEvent.
                .flatMap(event -> new UartEvaluator().handle(event))

                        // Finally, we're at a point where we're getting something we can use.
                        // .subscribe tells the Observable to finally startup, and that after we've marched
                        // through the above, we'll get an event.
                .subscribe(uartEvent -> {
                    String value = String.valueOf(uartEvent.data);

                    switch (uartEvent.type) {
                        case 0x0B:
                            mTextValue.setText(value);
                            analogLog.write(analogLog.date(), analogLog.time(), value);

                            break;
                        case 0x0A:
                            mHeartRate.setText(value);
                            heartRateLog.write(heartRateLog.date(), heartRateLog.time(), value);

                            break;
                    }
                });

        // We also need to make sure our dialog can be seen.  If this isn't run, then nothing shows up!.
        dialog.show(getFragmentManager(), "dialog");
    }

    /*
    onDestroy is ran every time the activity is destroyed.  This is normally the last we see of the Activity.
    Because of this, we don't want our bluetooth subscriptions to continue to run.
    We NEED to tell HermesBLE to clean up our mess.  Otherwise, good luck connecting again!
     */
    @Override
    protected void onDestroy() {
        super.onDestroy(); // Call super, because things.

        HermesBLE.close(mGatt); // Have Hermes handle closing out our bluetooth connection for us.
        mDeviceSubscription.unsubscribe(); // And unsubscribe from the dialog we created.

        // Finally, just incase we're not really closing, make sure we do - by running finish.
        finish();
    }
}
