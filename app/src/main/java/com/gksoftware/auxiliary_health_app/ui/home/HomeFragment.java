package com.gksoftware.auxiliary_health_app.ui.home;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.gksoftware.auxiliary_health_app.BloodOxygenActivity;
import com.gksoftware.auxiliary_health_app.BodyTemperatureActivity;
import com.gksoftware.auxiliary_health_app.HeartbeatActivity;
import com.gksoftware.auxiliary_health_app.MainActivity;
import com.gksoftware.auxiliary_health_app.R;
import com.gksoftware.auxiliary_health_app.RespiratoryRateActivity;
import com.gksoftware.auxiliary_health_app.model.Device;
import com.gksoftware.auxiliary_health_app.utils.Common;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class HomeFragment extends Fragment {

    @BindView(R.id.state_device_image)
    ImageView stateDeviceImage;
    @BindView(R.id.state_device)
    TextView state_device;
    @BindView(R.id.average_heart_rate)
    TextView average_heart_rate;

    private HomeViewModel homeViewModel;
    private static final int REQUEST_ENABLE_BT = 1002;
    private BluetoothAdapter bluetoothAdapter;
    private List<Device> devices = new ArrayList<>();
    private String[] devicesUID;

    private FirebaseAuth mAuth;

    private void updateListDevices() {
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            devicesUID = new String[pairedDevices.size()];
            int current = 0;
            for (BluetoothDevice device : pairedDevices) {
                Device dev = new Device(device.getAddress(), device.getName(), 0);
                devices.add(dev);
                devicesUID[current] = dev.getDeviceUid() + "-(" + dev.getName() + ")";
            }
        }
    }

    private void checkState() {
        if (bluetoothAdapter == null) {
            Toast.makeText(getContext(), "El dispositivo no soporta el Bluetooth", Toast.LENGTH_SHORT).show();
        } else {
            if (!bluetoothAdapter.isEnabled()) {
                Intent enablebt = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enablebt, REQUEST_ENABLE_BT);
            }
        }
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        homeViewModel =
                ViewModelProviders.of(this).get(HomeViewModel.class);
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        homeViewModel.getText().observe(this, new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {

            }
        });
        ButterKnife.bind(this, root);
        average_heart_rate.setText(String.valueOf(Common.PROM_RATE_HEART));
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        checkState();
        mAuth = FirebaseAuth.getInstance();
        return root;
    }

    @OnClick(R.id.blood_oxygen_btn)
    void bloodOxygen() {
        if (Common.currentBluetoothSelected != null) {
            getContext().startActivity(new Intent(getContext(), BloodOxygenActivity.class));
        } else {
            showAlertForSelectedBt();
        }
    }

    @OnClick(R.id.body_temp_btn)
    void bodyTemp() {
        if (Common.currentBluetoothSelected != null) {
            getContext().startActivity(new Intent(getContext(), BodyTemperatureActivity.class));
        } else {
            showAlertForSelectedBt();
        }
    }

    @OnClick(R.id.respiratory_rate_btn)
    void respiratoryRate() {
        if (Common.currentBluetoothSelected != null) {
            getContext().startActivity(new Intent(getContext(), RespiratoryRateActivity.class));
        } else {
            showAlertForSelectedBt();
        }
    }

    @OnClick(R.id.heartbeat_btn)
    void heartbeat() {
        if (Common.currentBluetoothSelected != null) {
            getContext().startActivity(new Intent(getContext(), HeartbeatActivity.class));
        } else {
            showAlertForSelectedBt();
        }
    }

    @OnClick(R.id.exit_app)
    void exit_app() {
        new AlertDialog.Builder(getContext())
                .setTitle("¿Desea salir?")
                .setMessage("¿Desea cerrar sesion?")
                .setIcon(R.drawable.ic_error)
                .setPositiveButton("Aceptar", (dialog, which) -> {
                    dialog.cancel();
                    mAuth.signOut();
                    getContext().startActivity(new Intent(getContext(), MainActivity.class));
                    ((AppCompatActivity) getContext()).finishAffinity();
                })
                .setCancelable(true)
                .create().show();
    }

    @OnClick(R.id.state_device)
    void stateDevice() {
        updateListDevices();
        try {
            new AlertDialog.Builder(getContext())
                    .setTitle(String.valueOf("Bluetooth disponibles"))
                    .setItems(devicesUID, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String selectedBt = devicesUID[which];
                            int endOfLineUID = selectedBt.indexOf("-");
                            if (endOfLineUID > 0) {
                                String uid = selectedBt.substring(0, endOfLineUID);
                                for (Device device : devices) {
                                    if (device.getDeviceUid().equals(uid)) {
                                        Common.currentBluetoothSelected = device;
                                    }
                                }
                                Log.d(this.getClass().getName(), Common.currentBluetoothSelected.toString());
                                if (Common.currentBluetoothSelected != null) {
                                    stateDeviceImage.setImageResource(R.drawable.ic_bluetooth_connected);
                                    state_device.setText(String.valueOf("Dispositivo conectado"));
                                }
                            }
                        }
                    }).create().show();
        } catch (Exception e) {
            Log.e(this.getClass().getName(), e.getMessage(), e);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT && bluetoothAdapter.isEnabled()) {
            Toast.makeText(getContext(), "Bluetooth Encendido", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "No se Activo el Bluetooth", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        average_heart_rate.setText(String.valueOf((Common.PROM_RATE_HEART)));
    }

    private void showAlertForSelectedBt() {
        new AlertDialog.Builder(getContext())
                .setTitle("Seleccione un dispositivo")
                .setMessage("Para acceder a las opciones debe conectarse a un dispositivo bluetooth. Seleccionelo en el icono.")
                .setIcon(R.drawable.ic_error)
                .setPositiveButton("Aceptar", (dialog, which) -> dialog.cancel())
                .setCancelable(true)
                .create().show();
    }
}