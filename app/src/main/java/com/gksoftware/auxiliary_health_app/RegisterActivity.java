package com.gksoftware.auxiliary_health_app;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.gksoftware.auxiliary_health_app.model.User;
import com.gksoftware.auxiliary_health_app.utils.Common;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.paperdb.Paper;

public class RegisterActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private ProgressDialog pdialog;
    private FirebaseFirestore db;
    private User user;
    private final String TAG = this.getClass().getName();

    @BindView(R.id.name_input)
    EditText name_input;
    @BindView(R.id.address_input)
    EditText address_input;
    @BindView(R.id.phone_input)
    EditText phone_input;
    @BindView(R.id.email_input)
    EditText email_input;
    @BindView(R.id.email_input_sign_up)
    EditText email_input_sign_up;
    @BindView(R.id.password_input)
    EditText password_input;
    //TIL
    @BindView(R.id.input_layout_name)
    TextInputLayout name_input_layout;
    @BindView(R.id.input_layout_address)
    TextInputLayout address_input_layout;
    @BindView(R.id.input_layout_phone)
    TextInputLayout phone_input_layout;
    @BindView(R.id.input_layout_email)
    TextInputLayout email_input_layout;
    @BindView(R.id.input_layout_email_sign_up)
    TextInputLayout email_input_sign_up_layout;
    @BindView(R.id.input_layout_password_sign_up)
    TextInputLayout password_input_layout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        ButterKnife.bind(this);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        Paper.init(this);
    }

    @OnClick(R.id.sign_up_button)
    void signUpButton() {
        pdialog = new ProgressDialog(this);
        pdialog.setMessage("Registrando...");
        pdialog.setCanceledOnTouchOutside(false);
        pdialog.show();
        String name = name_input.getText().toString();
        String address = address_input.getText().toString();
        String phone = phone_input.getText().toString();
        String email = email_input.getText().toString();
        String email_login = email_input_sign_up.getText().toString();
        String password = password_input.getText().toString();
        validateInputRegister(name, address, phone, email, email_login, password);
    }

    @OnClick(R.id.back_button)
    void backButton() {
        finish();
    }

    private void validateInputRegister(String name, String address, String phone, String email, String email_login, String password) {
        if (name.isEmpty()) {
            name_input_layout.setError("El campo esta vacio.");
        } else {
            name_input_layout.setError(null);
        }
        if (address.isEmpty()) {
            address_input_layout.setError("El campo esta vacio.");
        } else {
            address_input_layout.setError(null);
        }
        if (phone.isEmpty()) {
            phone_input_layout.setError("El campo esta vacio.");
        } else {
            phone_input_layout.setError(null);
        }
        if (email.isEmpty()) {
            email_input_layout.setError("El campo esta vacio.");
        } else {
            email_input_layout.setError(null);
        }
        if (email_login.isEmpty()) {
            email_input_sign_up_layout.setError("El campo esta vacio.");
        } else {
            email_input_sign_up_layout.setError(null);
        }
        if (password.isEmpty()) {
            password_input_layout.setError("El campo esta vacio.");
        } else {
            password_input_layout.setError(null);
        }
        if (!name.isEmpty() && !address.isEmpty() && !phone.isEmpty() && !email.isEmpty() && !email_login.isEmpty() && !password.isEmpty()) {
            user = new User(null, name, address, phone, email);
            mAuth.createUserWithEmailAndPassword(email_login, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            user.setKeyuser(mAuth.getCurrentUser().getUid());
                            db.collection(Common.KEY_USERS)
                                    .add(user.store())
                                    .addOnSuccessListener(documentReference -> {
                                        pdialog.dismiss();
                                        Common.createAlert(
                                                RegisterActivity.this,
                                                "Registro exitoso",
                                                "¿Desea iniciar sesión inmediatamente?",
                                                "Aceptar",
                                                "Cancelar",
                                                0,
                                                (dialog, which) -> {
                                                    startActivity(new Intent(RegisterActivity.this, HomeActivity.class));
                                                },
                                                (dialog, which) -> {
                                                    dialog.cancel();
                                                    finish();
                                                }).show();
                                    })
                                    .addOnFailureListener(e -> {
                                        pdialog.dismiss();
                                        Log.e(TAG, e.getMessage(), e);
                                        Common.createAlert(
                                                RegisterActivity.this,
                                                "Falló al registrar",
                                                e.getMessage(),
                                                "Aceptar",
                                                "Cancelar",
                                                0,
                                                (dialog, which) -> dialog.cancel(),
                                                null
                                        ).show();
                                    });
                        } else {
                            pdialog.dismiss();
                            Log.e(TAG, task.getException().getMessage(), task.getException());
                            Common.createAlert(
                                    RegisterActivity.this,
                                    "Error al registrar",
                                    task.getException().getMessage(),
                                    null,
                                    null,
                                    R.drawable.ic_error,
                                    (dialog, which) -> dialog.cancel(),
                                    null)
                                    .show();
                        }
                    });
        }
    }
}
