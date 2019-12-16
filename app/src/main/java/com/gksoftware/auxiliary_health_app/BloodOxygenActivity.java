package com.gksoftware.auxiliary_health_app;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

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
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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

public class BloodOxygenActivity extends AppCompatActivity {

    @BindView(R.id.back_button_bo)
    ImageView back_button_bo;
    @BindView(R.id.user_current_bo)
    TextView user_current_bo;
    @BindView(R.id.take_data_bo)
    Button takeData;
    @BindView(R.id.store_data_bo)
    Button store_data;
    @BindView(R.id.blood_oxygen_data)
    TextView blood_oxygen_data;
    @BindView(R.id.data_log_bo)
    TextView data_log_bo;

    private static final int REQUEST_ENABLE_BT = 1002;
    private BluetoothAdapter bluetoothAdapter;
    private StringBuilder builder = new StringBuilder();
    private List<String> boxygen = new ArrayList<>();

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
    private boolean flag = false;

    @OnClick(R.id.back_button_bo)
    void back_button_bo(){
        finish();
    }

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blood_oxygen);
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
                                String bloodO = String.valueOf(splitData[1]);
                                String lastNum = bloodO.substring(bloodO.length()-1, bloodO.length());
                                Log.d(this.getClass().getName(), "Last number: "+lastNum);
                                int val = Integer.parseInt(lastNum)+90;
                                boxygen.add(String.valueOf(val));
                                blood_oxygen_data.setText(val + "%os");
                                if (!flag){
                                    flag = true;
                                }
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
                        storeData();
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

    private void storeData() {
        Common.currentSelectedUser.setBloodOxygen(boxygen);
        db.collection(Common.KEY_USER_PATIENT)
                .document(String.valueOf(Common.currentSelectedUser.getIdentify()))
                .update("bloodOxygen", Common.currentSelectedUser.getHeartbeat())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        new AlertDialog.Builder(BloodOxygenActivity.this)
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

    @OnClick(R.id.user_current_bo)
    void userCurrent() {
        startActivity(new Intent(this, UsersActivity.class));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Common.currentSelectedUser != null) {
            user_current_bo.setText(String.format("Paciente: %s", Common.currentSelectedUser.getName()));
            Log.d(this.getClass().getName(), "onResume: " + macAddress);
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(macAddress);
            try {
                bluetoothSocket = createConnectionSecure(device);
                bluetoothAdapter.cancelDiscovery();
                bluetoothSocket.connect();
                connectedThread = new BluetoothThreadConnection(bluetoothSocket, handlerBluetoothIn, BloodOxygenActivity.this, handlerState);
                connectedThread.start();
            } catch (IOException e) {
                Log.e("IOException onResume", e.toString());
                Toast.makeText(BloodOxygenActivity.this, "Se presento un error al conectar.", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(BloodOxygenActivity.this, "Error: " + e.toString(), Toast.LENGTH_SHORT).show();
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
            Toast.makeText(BloodOxygenActivity.this, "Error: " + e.toString(), Toast.LENGTH_SHORT).show();
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
