package com.clj.blesample.operation;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.clj.blesample.R;
import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleNotifyCallback;
import com.clj.fastble.callback.BleWriteCallback;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.exception.BleException;
import com.clj.fastble.utils.HexUtil;

import org.w3c.dom.Text;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link CommandFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link CommandFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CommandFragment extends Fragment {


    boolean isWorking = false;

    BleDevice bleDevice = null;
    BluetoothGattCharacteristic sendChar = null;
    BluetoothGattCharacteristic recvChar = null;
    int totalSent = 0;
    int totalReceived = 0;
    int totalError = 0;
    long startTime = 0;

    View contentView = null;

    Timer mainTimer = null;

    boolean noRespone = false;

    ByteArrayOutputStream buff;
    int mtu = 200;

    public CommandFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment CommandFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static CommandFragment newInstance(String param1, String param2) {
        CommandFragment fragment = new CommandFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_command, container, false);

        contentView = v;


        bleDevice = ((OperationActivity) getActivity()).getBleDevice();
        String name = bleDevice.getName();
        String mac = bleDevice.getMac();
        BluetoothGatt gatt = BleManager.getInstance().getBluetoothGatt(bleDevice);
        sendChar = null;
        recvChar = null;

//        txt_name.setText(String.valueOf(getActivity().getString(R.string.name) + name));
//        txt_mac.setText(String.valueOf(getActivity().getString(R.string.mac) + mac));

        final TextView txt = (TextView) v.findViewById(R.id.console);
//        txt.setMovementMethod(ScrollingMovementMethod.getInstance());

//        Button clearConsole = (Button) v.findViewById(R.id.clear_console);
//        clearConsole.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                txt.setText("console started\n");
//            }
//        });


//        mResultAdapter.clear();
        for (BluetoothGattService service : gatt.getServices()) {
//            mResultAdapter.addResult(service);
            Log.e("detected service uuid: ", service.getUuid().toString());

            String gattServiceUUID = Long.toHexString(
                    service.getUuid().getMostSignificantBits())
                    .substring(0, 4);

//            addText(txt, "detected service uuid: " + gattServiceUUID);

            for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
//                Log.e("detected characteristic uuid: ", service.getUuid().toString());
                String characterUUID = Long.toHexString(
                        characteristic.getUuid()
                                .getMostSignificantBits()).substring(0, 4);

//                addText(txt, "detected characteristic uuid: " + characterUUID);

                if (gattServiceUUID.equals("ffe5") && characterUUID.equals("ffe9")) {
                    Toast.makeText(getActivity(), "successfully detected ffe5:ffe9", Toast.LENGTH_SHORT);
//                    addText(txt, "successfully detected ffe5:ffe9");
                    sendChar = characteristic;
                }

                if (gattServiceUUID.equals("ffe0") && characterUUID.equals("ffe4")) {
                    Toast.makeText(getActivity(), "successfully detected ffe0:ffe4", Toast.LENGTH_SHORT);
//                    addText(txt, "successfully detected ffe0:ffe4");
                    recvChar = characteristic;

                    openNotifiy(bleDevice, characteristic, txt);
                }
            }
        }
//        mResultAdapter.notifyDataSetChanged();

        final EditText et = (EditText) v.findViewById(R.id.editCommand);
        final BluetoothGattCharacteristic characteristic = sendChar;


        final Button btnWrite = (Button) v.findViewById(R.id.Write);
        btnWrite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (isWorking) {
                    btnWrite.setText("Start");
                }
                else {
                    btnWrite.setText("Stop");
                    totalReceived = 0;
                    totalError = 0;
                    totalSent = 0;
                    startTime = new Date().getTime();
                    String hex = et.getText().toString();
                    if (TextUtils.isEmpty(hex)) {
                        Toast.makeText(getActivity(), "no command", Toast.LENGTH_SHORT);
                        et.setText("20");
                        hex = "20";
                    }

                    mtu = Integer.valueOf(hex);
                    buff = new ByteArrayOutputStream();

                    write();
                }
                isWorking = !isWorking;

            }
        });

        mainTimer = null;

        refreshRate(0, 0);

        return v;
    }

    private void write(/*final BleDevice bleDevice, final BluetoothGattCharacteristic characteristic, final TextView txt, final EditText et*/) {
        if (bleDevice == null) {
            Toast.makeText(getActivity(), "no ble device", Toast.LENGTH_SHORT);
//            addText(txt, "no ble device");
        }

        Log.e("ble device", bleDevice.getName());

        if (sendChar == null) {
            Toast.makeText(getActivity(), "no characteristic", Toast.LENGTH_SHORT);
//            addText(txt, "no characteristic");
            return;
        }

        String mtuHex = Integer.toHexString(mtu - 1);

        byte[] mtuAscii = mtuHex.getBytes(StandardCharsets.US_ASCII);

//        int mtu = Integer.valueOf(hex);
        String command = "2A" + HexUtil.formatHexString(mtuAscii) + "303132333435363738394142434445460D";
//        for (int i = 0; i < mtu - 4; i++) {
//            command += "20";
//        }
//
//        command += "0D";

        Log.e("xxxx", "command:" + command);

        buff.reset();

        BleManager.getInstance().write(
                bleDevice,
                sendChar.getService().getUuid().toString(),
                sendChar.getUuid().toString(),
                HexUtil.hexStringToBytes(command),
                true,
                new BleWriteCallback() {

                    @Override
                    public void onWriteSuccess(final int current, final int total, final byte[] justWrite) {
//                        if (isWorking) write(bleDevice, characteristic, txt, et);
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                refreshRate(justWrite.length, 0);
//                                addText(txt, "write success, current: " + current
//                                        + " total: " + justWrite.length
//                                        /*+ " justWrite: " + HexUtil.formatHexString(justWrite, true)*/);
                            }
                        });
                    }

                    @Override
                    public void onWriteFailure(final BleException exception) {
                        try {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
//                                addText(txt, exception.toString());
                                }
                            });
                        }
                        catch (NullPointerException e) {

                        }
                    }
                });

        if (mainTimer != null ) {
            mainTimer.cancel();
            mainTimer.purge();
            mainTimer = null;
        }

        mainTimer = new Timer();
        mainTimer.schedule(new TimerTask() {
            @Override
            public void run() {

                totalError ++;
                if (isWorking) write();

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        refreshRate(0, 0);
                    }
                });
            }
        }, 200);

//        addText(txt, "command has been sent to " + bleDevice.getName() + ":" + characteristic.getUuid().toString());
    }

    private void openNotifiy(final BleDevice bleDevice, final BluetoothGattCharacteristic characteristic, final TextView txt) {
        BleManager.getInstance().notify(
                bleDevice,
                characteristic.getService().getUuid().toString(),
                characteristic.getUuid().toString(),
                new BleNotifyCallback() {

                    @Override
                    public void onNotifySuccess() {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
//                                addText(txt, "notify has been successfully registered");
                                Log.e("xxxx", "onNotifySuccess");
                            }
                        });
                    }

                    @Override
                    public void onNotifyFailure(final BleException exception) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
//                                addText(txt, exception.toString());
                                Log.e("xxxx", "onNotifyFailure");
                            }
                        });
                    }

                    @Override
                    public void onCharacteristicChanged(final byte[] data) {

                        try {
                            Log.e("xxxx", "onChangeLL:" + data.length);
                            Log.e("xxxx", "onChange1:" + HexUtil.formatHexString(data));
                            buff.write(data);
                            final int length = buff.size();
                            if (length >= mtu) {
                                if (mainTimer != null ) {
                                    mainTimer.cancel();
                                    mainTimer.purge();
                                    mainTimer = null;
                                }
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        refreshRate(0, length);
                                    }
                                });
                                if (isWorking) write();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
    }

    private void addText(TextView textView, String content) {
        textView.append(content);
        textView.append("\n");
        int offset = textView.getLineCount() * textView.getLineHeight();
        if (offset > textView.getHeight()) {
            textView.scrollTo(0, offset - textView.getHeight());
        }
    }

    private void refreshRate(int sent, int received) {
        Log.e("refresh", sent + ":" + received);
        totalSent += sent;
        totalReceived += received;

        long currentTime = new Date().getTime();

        Log.e("xxxx", "elapsed:" + (currentTime - startTime));

        double sentSpeed = (double)totalSent / (double) (currentTime - startTime) * 1000.0d;
        double receivedSpeed = (double)totalReceived / (double) (currentTime - startTime) * 1000.0d;

        TextView textView;
        textView = (TextView) contentView.findViewById(R.id.textViewSent);
        textView.setText("Total Sent : " + totalSent + " B");

        textView = (TextView) contentView.findViewById(R.id.textViewSentSpeed);
        textView.setText(String.format("%.2f", sentSpeed) + " B/s");

        textView = (TextView) contentView.findViewById(R.id.textViewReceived);
        textView.setText("Total Received : " + totalReceived + " B");

        textView = (TextView) contentView.findViewById(R.id.textViewReceivedSpeed);
        textView.setText(String.format("%.2f", receivedSpeed) + " B/s");

        textView = (TextView) contentView.findViewById(R.id.textViewErrors);
        textView.setText("Error Packets : " + totalError + "/" + (totalSent/20));

//        if (sent == 0 && received == 0) {
//            textView = (TextView) contentView.findViewById(R.id.textViewSentSpeed);
//            textView.setText("? B/s");
//
//            textView = (TextView) contentView.findViewById(R.id.textViewReceivedSpeed);
//            textView.setText("? B/s");
//
//        }


    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
