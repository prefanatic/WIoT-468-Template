package edu.uri.egr.wiot468template;

import android.bluetooth.BluetoothGatt;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import edu.uri.egr.hermesble.HermesBLE;
import edu.uri.egr.hermesble.attributes.BLStandardAttributes;
import edu.uri.egr.hermesble.ui.BLESelectionDialog;
import rx.Subscription;

/**
 * Created by cody on 10/8/15.
 */
public class MainActivity extends AppCompatActivity {
    public static final String UART_SERVICE = "";
    public static final String UART_RX = "";

    // Create Activity global variables for things we need across different methods.
    private BluetoothGatt mGatt;
    private Subscription mDeviceSubscription;

    /*
    This is called when the Activity is created.  In Android, this will be when the activity is started fresh
    for the first time, or when the screen is rotated.  Pressing the back button will cause the view to be
    destroyed, but pressing home, and then using multitasking to get back, will not (most of the time)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BLESelectionDialog dialog = new BLESelectionDialog();
        // Now, we need to subscribe to it.  This might look like black magic, but just follow the comments.
        mDeviceSubscription = dialog.getObservable() // Get the Observable from the device dialog.

                // We then want to "map" this observable to a different one.
                // HermesBLE.connectAndListen outputs a different observable.  So, once we get to this point,
                // we're passing our device we select in the dialog, on to HermesBLE for the connecting.
                .flatMap(device -> HermesBLE.connectAndListen(device,
                        UART_SERVICE,
                        UART_RX))

                        // After the above runs, we'll be connected.  So, the first "event" we get will be a success.
                        // Lets take out the BluetoothGatt from this event and save it.  We'll need it to clean up later.
                .doOnNext(event -> mGatt = event.gatt)

                        // Finally, we're at a point where we're getting something we can use.
                        // .subscribe tells the Observable to finally startup, and that after we've marched
                        // through the above, we'll get an event.
                .subscribe(event -> {

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
