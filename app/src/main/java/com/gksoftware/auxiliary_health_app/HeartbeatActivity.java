package com.gksoftware.auxiliary_health_app;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.gksoftware.auxiliary_health_app.model.Device;
import com.gksoftware.auxiliary_health_app.services.BluetoothThreadConnection;
import com.gksoftware.auxiliary_health_app.utils.Common;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class HeartbeatActivity extends AppCompatActivity {

    @OnClick(R.id.imageView)
    void imageView(){
        finish();
    }
    @BindView(R.id.user_current)
    TextView user_current;
    @BindView(R.id.take_data)
    Button takeData;
    @BindView(R.id.store_data)
    Button store_data;
    @BindView(R.id.heartbeat_pulse)
    TextView heartbeat_pulse;
    @BindView(R.id.data_log)
    TextView data_log;

    private static final int REQUEST_ENABLE_BT = 1002;
    private BluetoothAdapter bluetoothAdapter;
    private List<Device> devices = new ArrayList<>();
    private String[] devicesUID;
    private List<String> dataLogs = new ArrayList<>();
    private StringBuilder builder = new StringBuilder();

    private FirebaseFirestore db;

    //For BT Connection
    private String macAddress;
    Handler handlerBluetoothIn;
    final int handlerState = 0;
    private BluetoothSocket bluetoothSocket = null;
    private StringBuilder dataStringInput = new StringBuilder();
    //private ConnectedThread connectedThread;
    private BluetoothThreadConnection connectedThread;
    private static final UUID UUIDBT = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_heartbeat);
        ButterKnife.bind(this);
        db = FirebaseFirestore.getInstance();
        macAddress = Common.currentBluetoothSelected.getDeviceUid();
        handlerBluetoothIn = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                Log.d("Read", "Handler");
                if (msg.what == handlerState) {
                    String readMessage = (String) msg.obj;
                    dataStringInput.append(readMessage);

                    int endOfLineIndex = dataStringInput.indexOf("#");

                    if (endOfLineIndex > 0) {
                        String inputData = dataStringInput.substring(0, endOfLineIndex);
                        String[] splitData = inputData.split(",");
                        Log.d(this.getClass().getName(), Arrays.toString(splitData));
                        if (splitData.length >= 4) {
                            if (Common.currentSelectedUser != null) {
                                String result = workDataSplit(splitData[1]);
                                heartbeat_pulse.setText(result + "ppm");
                                dataLogs.add(result);
                                builder.append(result).append(" ,");
                                data_log.setText(builder.toString());
                            }
                        }
                        Log.d("Data", inputData);
                        dataStringInput.delete(0, dataStringInput.length());
                    }
                }
            }
        };

        takeData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Common.currentSelectedUser != null) {
                    connectedThread.write("0");
                } else {
                    Snackbar.make(findViewById(R.id.heartbeat_layout), "Debe seleccionar un usuario para poder registrar los datos.", Snackbar.LENGTH_LONG).show();
                }
            }
        });

        store_data.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Common.currentSelectedUser != null) {
                    try {
                        storeHeartbeat();
                        bluetoothSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    Snackbar.make(findViewById(R.id.heartbeat_layout), "Debe seleccionar un usuario para poder registrar los datos.", Snackbar.LENGTH_LONG).show();
                }
            }
        });

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        checkState();
    }

    private String workDataSplit(String splitDatum) {
        double pulse = Double.parseDouble(splitDatum);
        String result = "";
        if (pulse > 100 && pulse < 200) {
            result = String.valueOf(pulse / 2.5);
        } else if (pulse > 300 && pulse < 400) {
            result = String.valueOf(pulse / 4.2);
        } else if (pulse > 500 && pulse < 600) {
            result = String.valueOf(pulse / 7);
        } else {
            result = String.valueOf(pulse / 10);
        }
        return result;
    }

    private void storeHeartbeat() {
        Common.currentSelectedUser.setHeartbeat(dataLogs);
        db.collection(Common.KEY_USER_PATIENT)
                .document(String.valueOf(Common.currentSelectedUser.getIdentify()))
                .update("heartbeat", Common.currentSelectedUser.getHeartbeat())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        new AlertDialog.Builder(HeartbeatActivity.this)
                                .setTitle("Datos registrados")
                                .setMessage("Se agregaron los datos al registro.")
                                .setIcon(R.drawable.ic_info)
                                .setPositiveButton("Aceptar", (dialog, which) -> {
                                    dialog.cancel();
                                    finish();
                                })
                                .setCancelable(true)
                                .create().show();
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                showAlertForSelectedBt(e.getMessage());
            }
        });
    }

    @OnClick(R.id.user_current)
    void userCurrent() {
        startActivity(new Intent(this, UsersActivity.class));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Common.currentSelectedUser != null) {
            user_current.setText(String.format("Paciente: %s", Common.currentSelectedUser.getName()));
            Log.d(this.getClass().getName(), "onResume: " + macAddress);
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(macAddress);
            try {
                bluetoothSocket = createConnectionSecure(device);
                bluetoothAdapter.cancelDiscovery();
                bluetoothSocket.connect();
                connectedThread = new BluetoothThreadConnection(bluetoothSocket, handlerBluetoothIn, HeartbeatActivity.this, handlerState);
                connectedThread.start();
            } catch (IOException e) {
                Log.e("IOException onResume", e.toString());
                Toast.makeText(HeartbeatActivity.this, "Se presento un error al conectar.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            if (Common.currentSelectedUser != null) {
                bluetoothSocket.close();
                Log.d("onPause", "Connection Close");
            }
        } catch (IOException e) {
            Log.d("IOException On Pause", e.toString());
            Toast.makeText(HeartbeatActivity.this, "Error: " + e.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (Common.currentSelectedUser != null) {
                bluetoothSocket.close();
                Log.d("BTSocket", "Connection Close");
            }
        } catch (IOException e) {
            Log.d("IOException on Destroy", e.toString());
            Toast.makeText(HeartbeatActivity.this, "Error: " + e.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT && bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "Bluetooth Encendido", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "No se Activo el Bluetooth", Toast.LENGTH_SHORT).show();
        }
    }


    private void checkState() {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "El dispositivo no soporta el Bluetooth", Toast.LENGTH_SHORT).show();
        } else {
            if (!bluetoothAdapter.isEnabled()) {
                Intent enablebt = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enablebt, REQUEST_ENABLE_BT);
            } else {
                Log.d("ONLINE", "=======================");
            }
        }
    }

    private BluetoothSocket createConnectionSecure(BluetoothDevice bDevice) throws IOException {
        Log.e("UUIDBT", "" + UUIDBT);
        return bDevice.createRfcommSocketToServiceRecord(UUIDBT);
    }

    private void showAlertForSelectedBt(String e) {
        new AlertDialog.Builder(this)
                .setTitle("Error al capturar datos")
                .setMessage(e)
                .setIcon(R.drawable.ic_error)
                .setPositiveButton("Aceptar", (dialog, which) -> dialog.cancel())
                .setCancelable(true)
                .create().show();
    }
}
