package com.gksoftware.auxiliary_health_app.components.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.gksoftware.auxiliary_health_app.R;
import com.gksoftware.auxiliary_health_app.UsersActivity;
import com.gksoftware.auxiliary_health_app.interfaces.IProfile;
import com.gksoftware.auxiliary_health_app.model.AtentionUser;
import com.gksoftware.auxiliary_health_app.utils.Common;

import java.util.List;

public class AtentionAdapter extends RecyclerView.Adapter<AtentionViewHolder> {

    private List<AtentionUser> clientList;
    private Context context;
    private IProfile iProfile;

    public AtentionAdapter(List<AtentionUser> clientList, Context context) {
        this.clientList = clientList;
        this.context = context;
    }

    public void setiProfile(IProfile iProfile) {
        this.iProfile = iProfile;
    }

    @NonNull
    @Override
    public AtentionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View itemView = inflater.inflate(R.layout.item_object_layout, parent, false);
        return new AtentionViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull AtentionViewHolder holder, int position) {
        holder.name.setText(clientList.get(position).getName());
        holder.idClient.setText(String.valueOf(clientList.get(position).getIdentify()));
        holder.phone.setText(String.valueOf(clientList.get(position).getPhone()));
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (context instanceof UsersActivity) {
                    Log.d(this.getClass().getName(), "ActivityUser");
                    Common.currentSelectedUser = clientList.get(position);
                    ((AppCompatActivity) context).finish();
                }else{
                    iProfile.showProfile(clientList.get(position));
                    Log.d(this.getClass().getName(), "Activity profile");
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return clientList.size();
    }
}

class AtentionViewHolder extends RecyclerView.ViewHolder {

    TextView name;
    TextView idClient;
    TextView phone;

    AtentionViewHolder(View itemView) {
        super(itemView);
        name = itemView.findViewById(R.id.name_client);
        idClient = itemView.findViewById(R.id.id_client);
        phone = itemView.findViewById(R.id.phone_client);
    }

}