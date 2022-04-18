package net.moises_soft.m_sms;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class ConfigCuentaActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config_cuenta);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    public void guardarCuenta (View view){
        SharedPreferences preferences   = getSharedPreferences("DatosDeLaCuenta", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        EditText etContacto = (EditText) findViewById(R.id.etUsuario);
        EditText etClave    = (EditText) findViewById(R.id.etClave);

        String contacto = etContacto.getText().toString();
        String clave    = etClave.getText().toString();

        if (contacto.isEmpty() || clave.isEmpty()){
            Toast.makeText(ConfigCuentaActivity.this, "Lleno todos los campos.", Toast.LENGTH_SHORT).show();
        }else{
            editor.putString("Contacto",contacto);
            editor.putString("Clave",clave);
            Toast.makeText(ConfigCuentaActivity.this, "Se ha guardado su cuenta correctamente.", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(ConfigCuentaActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
            editor.commit();
        }
    }
}
