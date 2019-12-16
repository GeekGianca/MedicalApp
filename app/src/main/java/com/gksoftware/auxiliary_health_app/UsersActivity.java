package com.gksoftware.auxiliary_health_app;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.gksoftware.auxiliary_health_app.components.adapters.AtentionAdapter;
import com.gksoftware.auxiliary_health_app.model.AtentionUser;
import com.gksoftware.auxiliary_health_app.utils.Common;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class UsersActivity extends AppCompatActivity {

    @BindView(R.id.empty_recycler)
    TextView emptyRecycler;
    @BindView(R.id.users_recycler)
    RecyclerView userRecycler;
    @BindView(R.id.swipe_refresh)
    SwipeRefreshLayout swipeRefresh;
    @BindView(R.id.add_more_users)
    ImageView addMoreUsers;
    @BindView(R.id.back_button_user)
    ImageView backButtonUser;

    private AlertDialog.Builder dBuilder;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private List<AtentionUser> atentionUsers = new ArrayList<>();
    private AtentionUser atentionUser;
    private ProgressDialog pDialog;
    private final String TAG = this.getClass().getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_users);
        ButterKnife.bind(this);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        swipeRefresh.setRefreshing(true);
        loadUserList();
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                swipeRefresh.setRefreshing(true);
                loadUserList();
            }
        });
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
                        if (mAuth.getCurrentUser().getUid().equals(userAt.getUserDocRef())){
                            atentionUsers.add(userAt);
                        }
                        Log.d(TAG, document.getId() + " => " + document.getData());
                    }
                    loadToRecycler();
                } else {
                    Toast.makeText(UsersActivity.this, "Error al intentar obtener los registros.", Toast.LENGTH_LONG).show();
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
        if (atentionUsers.isEmpty()){
            emptyRecycler.setVisibility(View.VISIBLE);
            userRecycler.setVisibility(View.GONE);
        }else{
            emptyRecycler.setVisibility(View.GONE);
            userRecycler.setVisibility(View.VISIBLE);
        }
        userRecycler.setHasFixedSize(true);
        userRecycler.setLayoutManager(new LinearLayoutManager(this));
        AtentionAdapter adapter = new AtentionAdapter(atentionUsers, this);
        adapter.notifyDataSetChanged();
        userRecycler.setAdapter(adapter);
        swipeRefresh.setRefreshing(false);
    }

    @OnClick(R.id.add_more_users)
    void addMoreUsers() {
        dBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View component = inflater.inflate(R.layout.item_patient_layout, null);
        EditText identify = component.findViewById(R.id.identify_input);
        EditText name = component.findViewById(R.id.name_input);
        EditText age = component.findViewById(R.id.age_input);
        EditText birthdate = component.findViewById(R.id.birthdate_input);
        EditText phone = component.findViewById(R.id.phone_input);
        TextView text_error = component.findViewById(R.id.text_error);
        dBuilder.setView(component);
        dBuilder.setCancelable(false);
        dBuilder.setPositiveButton("Registrar", null);
        dBuilder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        AlertDialog dialog = dBuilder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button register = ((AlertDialog) dialog).getButton(dialog.BUTTON_POSITIVE);
                register.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (identify.getText().toString().isEmpty() || name.getText().toString().isEmpty() || age.getText().toString().isEmpty() || birthdate.getText().toString().isEmpty() || phone.getText().toString().isEmpty()) {
                            text_error.setVisibility(View.VISIBLE);
                            text_error.setText("Hay campos vacios.");
                        } else {
                            text_error.setVisibility(View.GONE);
                            dialog.dismiss();
                            registerAtention(identify.getText().toString(), name.getText().toString(), age.getText().toString(), birthdate.getText().toString(), phone.getText().toString());
                        }
                    }
                });
            }
        });
        dialog.show();
    }

    private void registerAtention(String identify, String name, String age, String birthdate, String phone) {
        pDialog = new ProgressDialog(this);
        pDialog.setMessage("Registrando paciente.");
        pDialog.setCanceledOnTouchOutside(false);
        pDialog.show();
        atentionUser = new AtentionUser();
        atentionUser.setName(name);
        atentionUser.setAge(Integer.parseInt(age));
        atentionUser.setIdentify(Integer.parseInt(identify));
        atentionUser.setBirthdate(birthdate);
        atentionUser.setPhone(phone);
        atentionUser.setUserDocRef(mAuth.getCurrentUser().getUid());
        db.collection(Common.KEY_USER_PATIENT)
                .document(atentionUser.getIdentify()+"")
                .set(atentionUser.store())
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        pDialog.dismiss();
                        if (task.isSuccessful()) {
                            Snackbar.make(findViewById(R.id.user_layout), "Registro exitoso", Snackbar.LENGTH_LONG).show();
                        } else {
                            errorAlert(null);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        errorAlert(e.getMessage());
                    }
                });
    }

    private void errorAlert(String o) {
        Common.createAlert(
                this,
                "Fallo al registrar",
                (o != null) ? o : "Se presento un error al intentar registrar.",
                "Aceptar",
                "Cancelar",
                0,
                (dialog, which) -> dialog.dismiss(),
                (dialog, which) -> dialog.cancel()).show();
    }
}
