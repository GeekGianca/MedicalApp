package com.gksoftware.auxiliary_health_app.components.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.gksoftware.auxiliary_health_app.R;
import com.gksoftware.auxiliary_health_app.interfaces.IConnected;
import com.gksoftware.auxiliary_health_app.model.Device;

import java.util.List;

public class DevicesAdapter extends RecyclerView.Adapter<DevicesViewHolder> {
    private List<Device> listDevices;
    private Context context;
    private IConnected iConnected;

    public DevicesAdapter(List<Device> listDevices, Context context, IConnected iConnected) {
        this.listDevices = listDevices;
        this.context = context;
        this.iConnected = iConnected;
    }

    @NonNull
    @Override
    public DevicesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View itemView = inflater.inflate(R.layout.device_item_layout, parent, false);
        return new DevicesViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull DevicesViewHolder holder, int position) {
        holder.deviceUid.setText(listDevices.get(position).getDeviceUid());
        holder.deviceName.setText(listDevices.get(position).getName());
        holder.itemView.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Permitir conexion");
            builder.setMessage("Estas realizando una conexion a un dispositivo Bluetooth. ¿Deseas permitirla?");
            builder.setPositiveButton("Permitir", (dialog, which) -> {
                dialog.cancel();
                iConnected.onConnectedDevice(listDevices.get(position).getDeviceUid());
            });
            builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());
            builder.show();
        });
    }

    @Override
    public int getItemCount() {
        return listDevices.size();
    }
}

class DevicesViewHolder extends RecyclerView.ViewHolder {

    TextView deviceName;
    TextView deviceUid;

    DevicesViewHolder(View itemView) {
        super(itemView);
        deviceName = itemView.findViewById(R.id.device_name);
        deviceUid = itemView.findViewById(R.id.uuid_device);
    }

}

