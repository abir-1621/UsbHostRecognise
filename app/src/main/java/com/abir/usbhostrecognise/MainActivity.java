package com.abir.usbhostrecognise;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbRequest;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;

public class MainActivity extends AppCompatActivity {

    PendingIntent mPermissionIntent;
    Button btnCheck;
    TextView textInfo;
    UsbDevice device;
    UsbManager manager;
    private static final String ACTION_USB_PERMISSION = "com.abir.usbhostrecognise.USB_PERMISSION";
    private final byte[] bytes = {'a'};
    private static final int TIMEOUT = 0;
    private final boolean forceClaim = true;
    UsbEndpoint endpoint;
    UsbDeviceConnection connection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnCheck = (Button) findViewById(R.id.check);
        textInfo = (TextView) findViewById(R.id.info);
        btnCheck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                checkDevices();
            }
        });

    }

    private void checkDevices() {
        manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(
                ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(mUsbReceiver, filter);
        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
        String i = "No value";
        while (deviceIterator.hasNext()) {
            device = deviceIterator.next();
            manager.requestPermission(device, mPermissionIntent);
            i += "\n" + "DeviceID: " + device.getDeviceId() + "\n"
                    + "DeviceName: " + device.getDeviceName() + "\n"
                    + "DeviceClass: " + device.getDeviceClass() + " - "
                    + "DeviceSubClass: " + device.getDeviceSubclass() + "\n"
                    + "VendorID: " + device.getVendorId() + "\n"
                    + "ProductID: " + device.getProductId() + "\n";
            System.out.println("#####################################" );
        }
if(device!=null){
    UsbInterface intf = device.getInterface(0);
    endpoint = intf.getEndpoint(0);
    connection = manager.openDevice(device);
    connection.claimInterface(intf, forceClaim);
    connection.bulkTransfer(endpoint, bytes, bytes.length, TIMEOUT);
}
        textInfo.setText(i);
    }

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            System.out.println("#####################################" + action);
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = (UsbDevice) intent
                            .getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(
                            UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            checkDevices();
                            Toast.makeText(MainActivity.this,"Found device",Toast.LENGTH_LONG).show();
                           // sendData();               // call method to set up device communication
                        }else {
                            Toast.makeText(MainActivity.this,"Not Found device",Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Log.d("ERROR", "permission denied for device " + device);
                    }
                }
            }
        }
    };


    public void sendData() {
        int bufferMaxLength = endpoint.getMaxPacketSize();
        ByteBuffer buffer = ByteBuffer.allocate(bufferMaxLength);
        UsbRequest request = new UsbRequest(); // create an URB
        request.initialize(connection, endpoint);
        buffer.put(bytes);

        // queue the outbound request
        boolean retval = request.queue(buffer, 1);
        if (connection.requestWait() == request) {
            // wait for confirmation (request was sent)
            UsbRequest inRequest = new UsbRequest();
            // URB for the incoming data
            inRequest.initialize(connection, endpoint);
            // the direction is dictated by this initialisation to the incoming endpoint.
            if (inRequest.queue(buffer, bufferMaxLength)) {
                connection.requestWait();
                System.out.println("#####################################" + buffer);
                // wait for this request to be completed
                // at this point buffer contains the data received
            }
        }
    }
}