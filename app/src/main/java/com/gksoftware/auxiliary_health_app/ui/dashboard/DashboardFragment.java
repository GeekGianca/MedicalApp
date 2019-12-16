package com.gksoftware.auxiliary_health_app.ui.dashboard;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.airbnb.lottie.LottieAnimationView;
import com.gksoftware.auxiliary_health_app.R;
import com.gksoftware.auxiliary_health_app.components.adapters.DevicesAdapter;
import com.gksoftware.auxiliary_health_app.interfaces.IConnected;
import com.gksoftware.auxiliary_health_app.model.Device;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;

public class DashboardFragment extends Fragment implements IConnected {

    @BindView(R.id.swipeUpdate)
    SwipeRefreshLayout swipeUpdate;
    @BindView(R.id.devices_paired)
    RecyclerView devices_paired;
    @BindView(R.id.toolbarDash)
    Toolbar toolbarDash;
    @BindView(R.id.bt_state)
    LottieAnimationView bt_state;

    private DashboardViewModel dashboardViewModel;
    private BluetoothAdapter bluetoothAdapter;
    private DevicesAdapter devicesAdapter;
    private List<Device> devices = new ArrayList<>();
    private static final int REQUEST_ENABLE_BT = 1000;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        dashboardViewModel =
                ViewModelProviders.of(this).get(DashboardViewModel.class);
        View root = inflater.inflate(R.layout.fragment_dashboard, container, false);
        ButterKnife.bind(this, root);
        ((AppCompatActivity)getActivity()).setSupportActionBar(toolbarDash);
        toolbarDash.setTitle("Dispositivos");
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        dashboardViewModel.getText().observe(this, s -> {

        });
        swipeUpdate.setColorSchemeColors(Color.BLUE, Color.CYAN, Color.YELLOW);
        swipeUpdate.setOnRefreshListener(this::updateListDevices);
        checkBluetoothState();
        updateListDevices();
        return root;
    }

    private void checkBluetoothState() {
        if (bluetoothAdapter == null){
            Toast.makeText(getContext(), "El dispositivo no soporta el Bluetooth", Toast.LENGTH_SHORT).show();
        }else{
            if (!bluetoothAdapter.isEnabled()){
                Intent enablebt = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enablebt, REQUEST_ENABLE_BT);
            }
        }
    }

    private void updateListDevices(){
        devices = new ArrayList<>();
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0){
            for (BluetoothDevice device : pairedDevices){
                Device dev = new Device(device.getAddress(), device.getName(), 0);
                devices.add(dev);
                Log.v("Devices: ", device.getName());
            }
            swipeUpdate.setRefreshing(false);
        }
        if (devices.size() == 0){
            devices_paired.setVisibility(View.GONE);
            bt_state.setVisibility(View.VISIBLE);
        }else{
            devices_paired.setVisibility(View.VISIBLE);
            bt_state.setVisibility(View.GONE);
        }
        devices_paired.setHasFixedSize(true);
        devices_paired.setLayoutManager(new LinearLayoutManager(getContext()));
        devicesAdapter = new DevicesAdapter(devices, getContext(), this);
        devicesAdapter.notifyDataSetChanged();
        devices_paired.setAdapter(devicesAdapter);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode==REQUEST_ENABLE_BT && bluetoothAdapter.isEnabled()){
            Toast.makeText(getContext(), "Bluetooth Encendido", Toast.LENGTH_SHORT).show();
            updateListDevices();
            bt_state.setVisibility(View.INVISIBLE);
        }else{
            bt_state.setVisibility(View.VISIBLE);
            Toast.makeText(getContext(), "No se Activo el Bluetooth", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onConnectedDevice(String uid) {

    }
}