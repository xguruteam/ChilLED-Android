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

import java.io.ByteArrayInputStream;
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


    public static final int MTU = 20;

    BleDevice bleDevice = null;
    BluetoothGattCharacteristic sendChar = null;
    BluetoothGattCharacteristic recvChar = null;

    View contentView = null;

    ByteArrayOutputStream buff = new ByteArrayOutputStream();

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
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
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

        for (BluetoothGattService service : gatt.getServices()) {
            Log.e("detected service uuid: ", service.getUuid().toString());

            String gattServiceUUID = Long.toHexString(
                    service.getUuid().getMostSignificantBits())
                    .substring(0, 4);


            for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                String characterUUID = Long.toHexString(
                        characteristic.getUuid()
                                .getMostSignificantBits()).substring(0, 4);


                if (gattServiceUUID.equals("ffe5") && characterUUID.equals("ffe9")) {
                    Toast.makeText(getActivity(), "successfully detected ffe5:ffe9", Toast.LENGTH_SHORT);
                    sendChar = characteristic;
                }

                if (gattServiceUUID.equals("ffe0") && characterUUID.equals("ffe4")) {
                    Toast.makeText(getActivity(), "successfully detected ffe0:ffe4", Toast.LENGTH_SHORT);
                    recvChar = characteristic;

                    openNotifiy(bleDevice, characteristic);
                }
            }
        }


        Button button;

        button = v.findViewById(R.id.btClear);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView textView;

                textView = (TextView) contentView.findViewById(R.id.tvID);
                textView.setText("");
                textView = (TextView) contentView.findViewById(R.id.tvIS);
                textView.setText("");
                textView = (TextView) contentView.findViewById(R.id.tvLS);
                textView.setText("");
                textView = (TextView) contentView.findViewById(R.id.tvblaa55);
                textView.setText("");
                textView = (TextView) contentView.findViewById(R.id.tvbuaa55);
                textView.setText("");
                textView = (TextView) contentView.findViewById(R.id.tvcd);
                textView.setText("");
                textView = (TextView) contentView.findViewById(R.id.tvcu);
                textView.setText("");
            }
        });

        button = v.findViewById(R.id.btID);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendID();
            }
        });

        button = v.findViewById(R.id.btIS);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendIS();
            }
        });

        button = v.findViewById(R.id.btLM0F);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendLM0F();
            }
        });

        button = v.findViewById(R.id.btLM1E);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendLM1E();
            }
        });

        button = v.findViewById(R.id.btLS);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendLS();
            }
        });

        button = v.findViewById(R.id.btblaa55);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendBLAA55();
            }
        });

        button = v.findViewById(R.id.btbuaa55);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendBUAA55();
            }
        });

        button = v.findViewById(R.id.btcd);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText editText = (EditText) contentView.findViewById(R.id.etcd);
                sendCD(editText.getText().toString());
            }
        });

        button = v.findViewById(R.id.btcu);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText editText = (EditText) contentView.findViewById(R.id.etcu);
                sendCU(editText.getText().toString());
            }
        });

        return v;
    }

    private void sendID() {
        byte[] packet = makePacket("ID");
        write(packet);
    }

    private void sendIS() {
        byte[] packet = makePacket("IS");
        write(packet);
    }

    private void sendLM0F() {
        byte[] packet = makePacket("LM", "0F");
        write(packet);
    }

    private void sendLM1E() {
        byte[] packet = makePacket("LM", "1E");
        write(packet);
    }

    private void sendLS() {
        byte[] packet = makePacket("LS");
        write(packet);
    }

    private void sendBLAA55() {
        byte[] packet = makePacket("BL", "AA55");
        write(packet);
    }

    private void sendBUAA55() {
        byte[] packet = makePacket("BU", "AA55");
        write(packet);
    }

    private void sendCD(String address) {
        if (address == null || address.length() != 4) return;
        
        byte[] packet = makePacket("CD", address, true, true);
        write(packet);
    }

    private void sendCU(String address) {
        if (address == null || address.length() != 4) return;

        byte[] packet = makePacket("CU", address, true, true);
        write(packet);
    }

    private byte[] makePacket(String command) {
        return makePacket(command, null, false, true);
    }

    private byte[] makePacket(String command, String data) {
        return makePacket(command, data, false, true);
    }

    private byte[] makePacket(String command, String data, boolean need2C, boolean endWithCR) {
        String packetX = null;

        packetX = command + ":";

        if (data != null) {
            packetX += data;
        }

        int paddingLength = MTU - packetX.length();
        if (need2C) paddingLength -= 2;
        if (endWithCR) paddingLength -= 1;

        if (paddingLength < 0) return null;

        while (paddingLength > 0) {
            packetX += " "; // a space
            paddingLength --;
        }

        if (need2C) packetX += "00";

        if (endWithCR) packetX += "\r";

        return packetX.getBytes();
    }

    private void write(byte[] packet) {
        if (bleDevice == null) {
            Toast.makeText(getActivity(), "no ble device", Toast.LENGTH_SHORT);
        }

        Log.e("ble device", bleDevice.getName());

        if (sendChar == null) {
            Toast.makeText(getActivity(), "no characteristic", Toast.LENGTH_SHORT);
            return;
        }

        if (packet== null || packet.length > MTU) return;

        BleManager.getInstance().write(
                bleDevice,
                sendChar.getService().getUuid().toString(),
                sendChar.getUuid().toString(),
                packet,
                true,
                new BleWriteCallback() {

                    @Override
                    public void onWriteSuccess(final int current, final int total, final byte[] justWrite) {
                    }

                    @Override
                    public void onWriteFailure(final BleException exception) {
                    }
                });

    }

    private void openNotifiy(final BleDevice bleDevice, final BluetoothGattCharacteristic characteristic) {
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
                            if (isEndingWithCR(data)) {
                                parse();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
    }

    private void parse() {
        if (buff.size() < 2) return;

        byte[] input = buff.toByteArray();
        buff.reset();


        if (!isEndingWithCR(input)) return;

        if (input[0] == 0x40) {
            final String hex = HexUtil.formatHexString(input);

            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    TextView textView = (TextView) contentView.findViewById(R.id.tvcu);
                    textView.setText(hex);
                }
            });

            return;
        }



        String packetX = new String(input);
        Log.e("xxxx", "---------->" + packetX);

        String commandX = packetX.substring(0, 2);

        if (commandX.equals("ID")) {
            final String id = packetX.substring(3, 19);
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    TextView textView = (TextView) contentView.findViewById(R.id.tvID);
                    textView.setText(id);
                }
            });
        }

        if (commandX.equals("IS")) {
            final String serialNumber = packetX.substring(3, 19);
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    TextView textView = (TextView) contentView.findViewById(R.id.tvIS);
                    textView.setText(serialNumber);
                }
            });
        }

        if (commandX.equals("LS")) {
            final String status = packetX.substring(3, 19);
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    TextView textView = (TextView) contentView.findViewById(R.id.tvLS);
                    textView.setText(status);
                }
            });
        }

        if (commandX.equals("BL")) {
            final String status = packetX.substring(3, 19);
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    TextView textView = (TextView) contentView.findViewById(R.id.tvblaa55);
                    textView.setText(status);
                }
            });
        }

        if (commandX.equals("BU")) {
            final String status = packetX.substring(3, 19);
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    TextView textView = (TextView) contentView.findViewById(R.id.tvbuaa55);
                    textView.setText(status);
                }
            });
        }

        if (commandX.equals("CD")) {
            final String status = packetX.substring(3, 19);
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    TextView textView = (TextView) contentView.findViewById(R.id.tvcd);
                    textView.setText(status);
                }
            });
        }
    }

    private boolean isEndingWithCR(byte[] input) {
        byte cr = input[input.length - 1];
        return (cr == 0x0d);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

}
