package com.fadstoobsessions.bluetooth;

import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.AsyncTask;
import android.widget.ToggleButton;

import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.w3c.dom.Text;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.UUID;


public class ledControl extends ActionBarActivity{

   // Button btnOn, btnOff, btnDis;
    SeekBar simpleSeekBar;
    TextView simpleSeekBar_TextView;
    Button SendMessage, DisconnectBtn, buttonTest;
    Switch switchLEDred, switchLEDgreen;
    ToggleButton toggleLEDyellow;

    String address = null;
    Integer simpleSeekBar_Progress = 50;
    private ProgressDialog progress;
    BluetoothAdapter myBluetooth = null;
    BluetoothSocket btSocket = null;
    private boolean isBtConnected = false;
    //SPP UUID. Look for it
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    Handler bluetoothIn;
    final int handlerState = 0;
    private StringBuilder recDataString = new StringBuilder();
    TextView txtString;

    private ConnectedThread mConnectedThread;

    LineGraphSeries<DataPoint> series = new LineGraphSeries<DataPoint>(new DataPoint[] {
            new DataPoint(0, 1),
            new DataPoint(1, 5),
            new DataPoint(2, 3),
            new DataPoint(3, 2),
            new DataPoint(4, 6)
    });

    private Integer lastX=5;
    private static final Random RANDOM = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent theIntent = getIntent();
        address = theIntent.getStringExtra(DeviceList.EXTRA_ADDRESS); //receive the address of the bluetooth device

        //view of the ledControl
        setContentView(R.layout.activity_led_control);

        //call the widgets
        simpleSeekBar = (SeekBar)findViewById(R.id.simpleSeekBar);
        simpleSeekBar_TextView = (TextView)findViewById(R.id.textView_simpleSeekBar);
        txtString = (TextView)findViewById(R.id.textView_ReceivedMessage);
        DisconnectBtn = (Button)findViewById(R.id.button_Disconnect);
        SendMessage = (Button)findViewById(R.id.button_SendMessage);
        buttonTest = (Button)findViewById(R.id.buttonTEST);
        switchLEDgreen = (Switch)findViewById(R.id.switch_LEDgreen);
        switchLEDred = (Switch)findViewById(R.id.switch_LEDred);
        toggleLEDyellow = (ToggleButton)findViewById(R.id.toggleButton_LEDyellow);


        String Date= DateFormat.getDateTimeInstance().format(new Date());
        txtString.setText(Date + ">Start\n");

       // simpleSeekBar.setMax(100);
        simpleSeekBar_Progress = 90;
        simpleSeekBar.setProgress(simpleSeekBar_Progress);
        simpleSeekBar_TextView.setText(simpleSeekBar_Progress.toString());

        new ConnectBT().execute(); //Call the class to connect

        //commands to be sent to bluetooth

        final GraphView graph = (GraphView) findViewById(R.id.graph);
        graph.getViewport().setBackgroundColor(197379);
        graph.addSeries(series); // this needs to come before "formatting" stuff below

        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setScrollable(true);
        //graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setYAxisBoundsStatus(Viewport.AxisBoundsStatus.AUTO_ADJUSTED);
        graph.getViewport().setMinX(0); //graph.getViewport().getMinX(true));
        //graph.getViewport().setMaxX(10); //graph.getViewport().getMaxX(false));
        graph.getViewport().setMinY(0); //graph.getViewport().getMinY(true));
        graph.getViewport().setMaxY(graph.getViewport().getMaxY(false));


        NumberFormat nfx = NumberFormat.getInstance();
        nfx.setMinimumFractionDigits(0);
        nfx.setMaximumFractionDigits(1);
        nfx.setMinimumIntegerDigits(1);
        NumberFormat nfy = NumberFormat.getInstance();
        nfy.setMinimumFractionDigits(0);
        nfy.setMaximumFractionDigits(2);
        nfy.setMinimumIntegerDigits(1);
        graph.getGridLabelRenderer().setHumanRounding(true);
        graph.getGridLabelRenderer().setNumVerticalLabels(13); //includes "axis bottom line" 11 means 10 "internal" divisions
        graph.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter(nfx,nfy));






        toggleLEDyellow.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                handleLEDs(3);
            }
        });

        switchLEDred.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                handleLEDs(1);
            }
        });

        switchLEDgreen.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                handleLEDs(2);
            }
        });


        simpleSeekBar_TextView.setText(" " + simpleSeekBar.getMax());
        simpleSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progress = 0;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progresValue, boolean fromUser) {
                progress = progresValue;
                simpleSeekBar_TextView.setText(" " + progress);
                //Toast.makeText(getApplicationContext(), "Changing seekbar's progress", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                //Toast.makeText(getApplicationContext(), "Started tracking seekbar", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                //simpleSeekBar_TextView.setText("Covered: " + progress + "/" + seekBar.getMax());
                //Toast.makeText(getApplicationContext(), "Stopped tracking seekbar", Toast.LENGTH_SHORT).show();
                String text = Integer.toString(progress);
                if (btSocket!=null) {
                    try {
                        btSocket.getOutputStream().write(text.getBytes());
                        msg("Message Sent");
                    }
                    catch (IOException e) {
                        msg("Error");
                    }
                }
            }
        });

        buttonTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Double tempD = RANDOM.nextDouble()*10D;
                series.appendData(new DataPoint(lastX++, tempD),true,10);//auto-scroll, with 10 data points

                series.


                graph.getViewport().calcCompleteRange();
                graph.getViewport().setMaxY(graph.getViewport().getMaxY(false));

                //Double tempValueX = graph.getViewport().getMaxX(false);
                //Double tempValueY = graph.getViewport().getMaxY(false);
                //msg(tempValueX.toString() + "," + String.format("%.2f",tempValueY) + ":" + String.format("%.2f",tempD));
            }
        });

        SendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();      //method to send message to uP
            }
        });

        DisconnectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Disconnect(); //close connection
            }
        });

//        On.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                turnOnLed();      //method to turn on
//            }
//        });
//
//        Off.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                turnOffLed();   //method to turn off
//            }
//        });
//
//        Discnt.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Disconnect(); //close connection
//            }
//        });

        bluetoothIn = new Handler() {
            public void handleMessage(android.os.Message msg) {
                if (msg.what == handlerState) {                                         // if message is what we want
                    String readMessage = (String) msg.obj;                              // msg.arg1 = bytes from connect thread
                    recDataString.append(readMessage);                                  // keep appending to string until ~
                    int endOfLineIndex = recDataString.indexOf("~");                    // determine the end-of-line
                    if (endOfLineIndex > 0) {                                           // make sure there data before ~
                        String dataInPrint = recDataString.substring(0, endOfLineIndex);    // extract string
                        String Date= DateFormat.getDateTimeInstance().format(new Date());

                        //txtString.setText(Date + ">" + dataInPrint + "\n");
                        txtString.append(Date + ">" + dataInPrint + "\n");
                        recDataString.delete(0, recDataString.length());                    //clear all string data
                    }
                }
            }
        };

    }

// This was part of attempt to have two layouts with a single activity
//    @Override
//    public void onBackPressed() {
//        if (layoutNumber == 1) {
//            layoutNumber = 0;
//            setContentView(R.layout.activity_led_control);
//        } else {
//            Disconnect();
//            super.onBackPressed();
//        };
//
//    }

private void sendMessage() {
    EditText editText = (EditText)findViewById(R.id.editText_SendMessage);
    String text = editText.getText().toString();
    if (btSocket!=null) {
        try {
            btSocket.getOutputStream().write(text.getBytes());
            msg("Message Sent");
        }
        catch (IOException e) {
            msg("Error");
        }
    }
}

    private void Disconnect() {
        if (btSocket!=null){ //If the btSocket is busy
            try {
                btSocket.close(); //close connection
            }
            catch (IOException e) {
                msg("Error");
            }
        }
        finish(); //return to the first layout
    }

    private void handleLEDs(Integer LEDid){
        if (btSocket!=null){ //If the btSocket is busy
            try {
                String text = Integer.toString(LEDid);
                btSocket.getOutputStream().write(text.getBytes());
                msg("Message Sent: LED " + text);
            }
            catch (IOException e) {
                msg("Error");
            }
        }
    }
//
//    private void turnOffLed()
//    {
//        if (btSocket!=null)
//        {
//            try
//            {
//                btSocket.getOutputStream().write("0".toString().getBytes());
//            }
//            catch (IOException e)
//            {
//                msg("Error");
//            }
//        }
//    }
//
//    private void turnOnLed()
//    {
//        if (btSocket!=null)
//        {
//            try
//            {
//                btSocket.getOutputStream().write("1".toString().getBytes());
//            }
//            catch (IOException e)
//            {
//                msg("Error");
//            }
//        }
//    }

    // fast way to call Toast
    private void msg(String s)
    {
        Toast.makeText(getApplicationContext(),s,Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_led_control, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private class ConnectBT extends AsyncTask<Void, Void, Void>  // UI thread
    {
        private boolean ConnectSuccess = true; //if it's here, it's almost connected

        @Override
        protected void onPreExecute()
        {
            progress = ProgressDialog.show(ledControl.this, "Connecting...", "Please wait!!!");  //show a progress dialog
        }

        @Override
        protected Void doInBackground(Void... devices) //while the progress dialog is shown, the connection is done in background
        {
            try {
                if (btSocket == null || !isBtConnected) {
                    myBluetooth = BluetoothAdapter.getDefaultAdapter();//get the mobile bluetooth device
                    BluetoothDevice device = myBluetooth.getRemoteDevice(address);//connects to the device's address and checks if it's available
                    btSocket = device.createInsecureRfcommSocketToServiceRecord(myUUID);//create a RFCOMM (SPP) connection
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    btSocket.connect();//start connection
                    mConnectedThread = new ConnectedThread(btSocket);
                    mConnectedThread.start();
                }
            }
            catch (IOException e) {
                ConnectSuccess = false;//if the try failed, you can check the exception here
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) //after the doInBackground, it checks if everything went fine
        {
            super.onPostExecute(result);

            if (!ConnectSuccess)
            {
                msg("Connection Failed. Is it a SPP Bluetooth? Try again.");
                finish();
            }
            else
            {
                msg("Connected.");
                isBtConnected = true;
            }
            progress.dismiss();
        }
    }

    //create new class for connect thread
    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        //creation of the connect thread
        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                //Create I/O streams for connection
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[256];
            int bytes;

            // Keep looping to listen for received messages
            while (true) {
                try {
                    bytes = mmInStream.read(buffer);            //read bytes from input buffer
                    String readMessage = new String(buffer, 0, bytes);
                    // Send the obtained bytes to the UI Activity via handler
                    bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }
        //write method
        public void write(String input) {
            byte[] msgBuffer = input.getBytes();           //converts entered String into bytes
            try {
                mmOutStream.write(msgBuffer);                //write bytes over BT connection via outstream
            } catch (IOException e) {
                //if you cannot write, close the application
                Toast.makeText(getBaseContext(), "Connection Failure", Toast.LENGTH_LONG).show();
                finish();

            }
        }
    }

}
