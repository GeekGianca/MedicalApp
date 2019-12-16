package com.gksoftware.auxiliary_health_app.ui.notifications;

import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.gksoftware.auxiliary_health_app.R;
import com.gksoftware.auxiliary_health_app.UsersActivity;
import com.gksoftware.auxiliary_health_app.components.adapters.AtentionAdapter;
import com.gksoftware.auxiliary_health_app.interfaces.IProfile;
import com.gksoftware.auxiliary_health_app.model.AtentionUser;
import com.gksoftware.auxiliary_health_app.utils.Common;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class NotificationsFragment extends Fragment implements IProfile {

    @BindView(R.id.swipe_update)
    SwipeRefreshLayout swipeUpdate;
    @BindView(R.id.users_re)
    RecyclerView users_re;
    @BindView(R.id.profile_toolbar)
    Toolbar profile_toolbar;
    @BindView(R.id.name_user)
    TextView nameUseR;
    @BindView(R.id.email_user)
    TextView email_user;
    @BindView(R.id.phone_user)
    TextView phone_user;

    private List<AtentionUser> atentionUsers = new ArrayList<>();
    private AtentionAdapter adapter;
    private FirebaseFirestore db;

    private FirebaseAuth mAuth;
    private FirebaseUser user;

    private NotificationsViewModel notificationsViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        notificationsViewModel =
                ViewModelProviders.of(this).get(NotificationsViewModel.class);
        View root = inflater.inflate(R.layout.fragment_notifications, container, false);

        notificationsViewModel.getText().observe(this, new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {

            }
        });
        ButterKnife.bind(this, root);
        profile_toolbar.setTitle("Perfil");
        ((AppCompatActivity) getContext()).setSupportActionBar(profile_toolbar);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        swipeUpdate.setRefreshing(true);
        swipeUpdate.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                swipeUpdate.setRefreshing(true);
                loadUserList();
            }
        });
        user = mAuth.getCurrentUser();
        nameUseR.setText(user.getDisplayName());
        email_user.setText(user.getEmail());
        phone_user.setText(user.getPhoneNumber());
        loadUserList();
        return root;
    }

    private void loadUserList() {
        db.collection(Common.KEY_USER_PATIENT)
                .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    atentionUsers = new ArrayList<>();
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        AtentionUser userAt = document.toObject(AtentionUser.class);
                        if (user.getUid().equals(userAt.getUserDocRef())) {
                            atentionUsers.add(userAt);
                        }
                        Log.d(this.getClass().getName(), document.getId() + " => " + document.getData());
                    }
                    loadToRecycler();
                } else {
                    Toast.makeText(getContext(), "Error al intentar obtener los registros.", Toast.LENGTH_LONG).show();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                errorAlert(e.getMessage());
            }
        });
    }

    private void loadToRecycler() {
        users_re.setHasFixedSize(true);
        users_re.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new AtentionAdapter(atentionUsers, getContext());
        adapter.setiProfile(this);
        adapter.notifyDataSetChanged();
        users_re.setAdapter(adapter);
        swipeUpdate.setRefreshing(false);
    }

    private void errorAlert(String o) {
        Common.createAlert(
                getContext(),
                "Fallo al registrar",
                (o != null) ? o : "Se presento un error al intentar registrar.",
                "Aceptar",
                "Cancelar",
                0,
                (dialog, which) -> dialog.dismiss(),
                (dialog, which) -> dialog.cancel()).show();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        MenuInflater inflate = ((AppCompatActivity) getContext()).getMenuInflater();
        inflate.inflate(R.menu.profile_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_exit:
                Log.d(this.getClass().getName(), "Exit to app");
                return true;
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    @Override
    public void showProfile(AtentionUser au) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Historial de registro por persona");
        builder.setIcon(R.drawable.ic_info);
        LayoutInflater inflater = getLayoutInflater();
        View component = inflater.inflate(R.layout.show_profile_layout, null);
        TextView name = component.findViewById(R.id.name_user_profile);
        TextView phone = component.findViewById(R.id.phone_profile);
        TextView age = component.findViewById(R.id.age_profile);
        TextView heartbeat = component.findViewById(R.id.heartbeat_profile);
        TextView respiratory = component.findViewById(R.id.respiratory_profile);
        TextView body = component.findViewById(R.id.body_profile);
        TextView oxygen = component.findViewById(R.id.blood_profile);
        name.setText(au.getName());
        phone.setText(au.getPhone());
        age.setText(String.format("Edad: %s", au.getAge()));
        heartbeat.setText(String.format("Promedio ritmo cardiaco: %s", (au.getHeartbeat().isEmpty()) ? "0" : au.getHeartbeat().get(0)));
        respiratory.setText(String.format("Promedio ritmo respiratorio: %s", ((au.getRespiratoryRate() != null) ? (au.getRespiratoryRate().isEmpty()) ? "0" : au.getRespiratoryRate().get(0) : "0")));
        body.setText(String.format("Promedio temperatura corporal: %s", ((au.getBodyTemperature() != null) ? (au.getBodyTemperature().isEmpty()) ? "0" : au.getBodyTemperature().get(0) : "0")));
        oxygen.setText(String.format("Promedio oxigeno en la sangre: %s", ((au.getBloodOxygen() != null) ? ((au.getBloodOxygen().isEmpty()) ? "0" : au.getBloodOxygen().get(0)) : "0")));
        builder.setView(component);
        builder.setPositiveButton("Aceptar", (dialog, which) -> dialog.cancel());
        builder.create().show();
    }
}