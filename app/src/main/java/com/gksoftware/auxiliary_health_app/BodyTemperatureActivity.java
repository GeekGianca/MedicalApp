package com.gksoftware.auxiliary_health_app;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
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

public class BodyTemperatureActivity extends AppCompatActivity {

    @BindView(R.id.back_temperature)
    ImageView back_temperature;
    @BindView(R.id.bt_state_temperature)
    ImageView bt_state_temperature;
    @BindView(R.id.state_bt_temperature)
    TextView state_bt_temperature;
    @BindView(R.id.temperature_rate)
    TextView temperature_rate;
    @BindView(R.id.body_temp_chart)
    LineChart body_temp_chart;
    @BindView(R.id.user_current_selected_temp)
    TextView user_current_selected_temp;
    @BindView(R.id.store_data_temp)
    Button store_data_temp;
    @BindView(R.id.take_data_temp)
    Button take_data_temp;

    @OnClick(R.id.back_temperature)
    void back_temperature(){
        finish();
    }

    private List<String> data = new ArrayList<>();
    private FirebaseFirestore db;

    private static final int REQUEST_ENABLE_BT = 1002;
    private BluetoothAdapter bluetoothAdapter;
    //For BT Connection
    private String macAddress;
    Handler handlerBluetoothIn;
    final int handlerState = 0;
    private BluetoothSocket bluetoothSocket = null;
    private StringBuilder dataStringInput = new StringBuilder();
    //private ConnectedThread connectedThread;
    private BluetoothThreadConnection connectedThread;
    private static final UUID UUIDBT = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private int timestamp = 0;
    private int mFillColor = Color.argb(150, 51, 101, 229);

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_body_temperature);
        ButterKnife.bind(this);
        body_temp_chart.setGridBackgroundColor(Color.GREEN);
        body_temp_chart.setDrawGridBackground(true);
        List<Entry> entries = new ArrayList<>();
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
                        if (splitData.length > 4) {
                            if (Common.currentSelectedUser != null) {
                                String temperature = String.valueOf(splitData[0]);
                                Log.d(this.getClass().getName(), "Temperature: " + temperature);
                                float temp = Float.parseFloat(temperature);
                                if (temp > 0) {
                                    temperature_rate.setText(String.format("%s°", temp));
                                    data.add(String.valueOf(temp));
                                    entries.add(new Entry(timestamp, temp));
                                    LineDataSet dataSet = new LineDataSet(entries, "Temperatura corporal");
                                    dataSet.setColor(Color.RED);
                                    LineData lineData = new LineData(dataSet);
                                    body_temp_chart.setData(lineData);
                                    body_temp_chart.invalidate();
                                    timestamp++;
                                }
                            }
                        }
                        Log.d("Data", inputData);
                        dataStringInput.delete(0, dataStringInput.length());
                    }
                } else {
                    Toast.makeText(BodyTemperatureActivity.this, "Fallo al conectar.", Toast.LENGTH_SHORT).show();
                }
            }
        };

        take_data_temp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Common.currentSelectedUser != null) {
                    connectedThread.write("0");
                } else {
                    Snackbar.make(findViewById(R.id.body_temp_layout), "Debe seleccionar un usuario para poder registrar los datos.", Snackbar.LENGTH_LONG).show();
                }
            }
        });

        store_data_temp.setOnClickListener(new View.OnClickListener() {
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
                    Snackbar.make(findViewById(R.id.respiratory_rate_layout), "Debe seleccionar un usuario para poder registrar los datos.", Snackbar.LENGTH_LONG).show();
                }
            }
        });

        user_current_selected_temp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(BodyTemperatureActivity.this, UsersActivity.class));
            }
        });

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        checkState();
    }

    private void storeData() {
        Common.currentSelectedUser.setBodyTemperature(data);
        db.collection(Common.KEY_USER_PATIENT)
                .document(String.valueOf(Common.currentSelectedUser.getIdentify()))
                .update("bodyTemperature", Common.currentSelectedUser.getHeartbeat())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        new AlertDialog.Builder(BodyTemperatureActivity.this)
                                .setTitle("Datos registrados")
                                .setMessage("Se agregaron los datos al registro.")
                                .setIcon(R.drawable.ic_info)
                                .setPositiveButton("Aceptar", (dialog, which) -> {
                                    dialog.cancel();
                                    finish();
                                })
                                .setCancelable(true)
                                .create().show();
                        //Snackbar.make(findViewById(R.id.body_temp_layout), "Se agregaron los datos al registro.", Snackbar.LENGTH_LONG).show();
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                showAlertForSelectedBt(e.getMessage());
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Common.currentSelectedUser != null) {
            user_current_selected_temp.setText(String.format("Paciente: %s", Common.currentSelectedUser.getName()));
            Log.d(this.getClass().getName(), "onResume: " + macAddress);
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(macAddress);
            try {
                bluetoothSocket = createConnectionSecure(device);
                bluetoothAdapter.cancelDiscovery();
                bluetoothSocket.connect();
                connectedThread = new BluetoothThreadConnection(bluetoothSocket, handlerBluetoothIn, BodyTemperatureActivity.this, handlerState);
                connectedThread.start();
            } catch (IOException e) {
                Log.e("IOException onResume", e.toString());
                Toast.makeText(BodyTemperatureActivity.this, "Se presento un error al conectar.", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(BodyTemperatureActivity.this, "Error: " + e.toString(), Toast.LENGTH_SHORT).show();
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
            Toast.makeText(BodyTemperatureActivity.this, "Error: " + e.toString(), Toast.LENGTH_SHORT).show();
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
