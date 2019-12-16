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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class RespiratoryRateActivity extends AppCompatActivity {

    @OnClick(R.id.back_rrate)
    void back_temperature() {
        finish();
    }

    @BindView(R.id.back_rrate)
    ImageView back_rrate;
    @BindView(R.id.bt_state_img_rrate)
    ImageView bt_state_img_rrate;
    @BindView(R.id.state_bt_rrate)
    TextView state_bt_rrate;
    @BindView(R.id.respiratory_rate)
    TextView respiratory_rate;
    @BindView(R.id.rate_respiratory_chart)
    LineChart rate_respiratory_chart;
    @BindView(R.id.user_current_selected)
    TextView user_current_selected;
    @BindView(R.id.store_data_rrate)
    Button store_data_rrate;
    @BindView(R.id.take_data_rrate)
    Button take_data_rrate;

    private Map<String, String> data = new HashMap<>();
    private List<String> respiratoryRate = new ArrayList<>();

    private static final int REQUEST_ENABLE_BT = 1002;
    private BluetoothAdapter bluetoothAdapter;
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
    private int rate;
    private boolean state = true;

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_respiratory_rate);
        ButterKnife.bind(this);
        List<Entry> entries = new ArrayList<>();
        rate = 0;
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
                                String vRate = String.valueOf(splitData[2]);
                                float rater = Float.parseFloat(vRate);
                                String rs_air = String.valueOf(splitData[3]);
                                String r0 = String.valueOf(splitData[4]);
                                Log.d(this.getClass().getName(), "Respiratory Rate: " + rs_air + " - " + r0);
                                respiratoryRate.add(String.format("%s-%s-%sV", rs_air, r0, vRate));
                                double valR = rater / 60;
                                respiratory_rate.setText(String.format("%sp/minuto", valR + 10));
                                float val = Float.parseFloat(r0) * -1;
                                float valY = Float.parseFloat(rs_air) * -1;
                                Log.d(this.getClass().getName(), "Respiratory Rate Axis: " + val + " - " + valY);
                                if (val > 0 && valY > 0) {
                                    Common.PROM_RATE_HEART += valY / (rate);
                                    if (state) {
                                        state = false;
                                        entries.add(new Entry(rate, valY));
                                    } else {
                                        entries.add(new Entry(rate, val));
                                        state = true;
                                    }
                                    LineDataSet dataSet = new LineDataSet(entries, "Ritmo respiratorio");
                                    dataSet.setColor(Color.GREEN);
                                    LineData lineData = new LineData(dataSet);
                                    rate_respiratory_chart.setData(lineData);
                                    rate_respiratory_chart.invalidate();
                                    rate++;
                                }
                            }
                        }
                        Log.d("Data", inputData);
                        dataStringInput.delete(0, dataStringInput.length());
                    }
                } else {
                    Toast.makeText(RespiratoryRateActivity.this, "Fallo al conectar.", Toast.LENGTH_SHORT).show();
                }
            }
        };

        take_data_rrate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Common.currentSelectedUser != null) {
                    connectedThread.write("0");
                } else {
                    Snackbar.make(findViewById(R.id.respiratory_rate_layout), "Debe seleccionar un usuario para poder registrar los datos.", Snackbar.LENGTH_LONG).show();
                }
            }
        });

        store_data_rrate.setOnClickListener(new View.OnClickListener() {
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

        user_current_selected.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(RespiratoryRateActivity.this, UsersActivity.class));
            }
        });

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        checkState();
    }

    private void storeData() {
        Common.currentSelectedUser.setRespiratoryRate(respiratoryRate);
        db.collection(Common.KEY_USER_PATIENT)
                .document(String.valueOf(Common.currentSelectedUser.getIdentify()))
                .update("respiratoryRate", Common.currentSelectedUser.getHeartbeat())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        new AlertDialog.Builder(RespiratoryRateActivity.this)
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

    @Override
    protected void onResume() {
        super.onResume();
        if (Common.currentSelectedUser != null) {
            user_current_selected.setText(String.format("Paciente: %s", Common.currentSelectedUser.getName()));
            Log.d(this.getClass().getName(), "onResume: " + macAddress);
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(macAddress);
            try {
                bluetoothSocket = createConnectionSecure(device);
                bluetoothAdapter.cancelDiscovery();
                bluetoothSocket.connect();
                connectedThread = new BluetoothThreadConnection(bluetoothSocket, handlerBluetoothIn, RespiratoryRateActivity.this, handlerState);
                connectedThread.start();
            } catch (IOException e) {
                Log.e("IOException onResume", e.toString());
                Toast.makeText(RespiratoryRateActivity.this, "Se presento un error al conectar.", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(RespiratoryRateActivity.this, "Error: " + e.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (Common.currentSelectedUser != null) {
                bluetoothSocket.close();
            }
            Log.d("BTSocket", "Connection Close");
        } catch (IOException e) {
            Log.d("IOException on Destroy", e.toString());
            Toast.makeText(RespiratoryRateActivity.this, "Error: " + e.toString(), Toast.LENGTH_SHORT).show();
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
