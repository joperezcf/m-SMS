package net.moises_soft.m_sms;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.MultiAutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {
    private static final int INTERVALO = 2000; //2 segundos para salir
    private long tiempoPrimerClick;
    private MultiAutoCompleteTextView completeTextView;
    private static final int ABRIRFICHERO_RESULT_CODE = 1;
    private String dirFicheroTxt = "";

    private final ArrayList<String> contactNameList = new ArrayList<>();
    private final ArrayList<String> contactNumberList = new ArrayList<>();
    private final HashMap<String, String> nameToNumberMap = new HashMap<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // .:: Crear el autocompletamiento de los contactos ::.
        completeTextView = (MultiAutoCompleteTextView) findViewById(R.id.mactvContact);
        final ContentResolver contentResolver = getContentResolver();
        Cursor managedCursor = contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);

        assert managedCursor != null;
        if (managedCursor.moveToFirst()) {
            String contactName, contactNumber;

            int nameColumn = managedCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
            int phoneColumn = managedCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
            do {
                //Get the field values
                contactName = managedCursor.getString(nameColumn);
                contactNumber = managedCursor.getString(phoneColumn);
                //noinspection StringEquality
                if ((contactName != " " || contactName != null) && (contactNumber != " " || contactNumber != null)) {
                    contactNameList.add(contactName);
                    contactNumberList.add(contactNumber);
                    nameToNumberMap.put(contactName, contactNumber);
                }
            } while (managedCursor.moveToNext());

            managedCursor.close();

            String[] contacts = new String[contactNameList.size()];

            for (int i = 0; i < contacts.length; i++) {
                String value = contactNameList.get(i) + " (" + contactNumberList.get(i) + ")";
                contacts[i] = value;
            }

            ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, contacts);
            completeTextView.setAdapter(adapter);
            completeTextView.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());
        }

        //.:: Contar la cantidad de caracteres del Msg en tiempo real::.
        EditText etTextMsg = (EditText) findViewById(R.id.etMsg);
        etTextMsg.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                TextView contador = (TextView) findViewById(R.id.tvTextContador);
                String tamanoString = String.valueOf(s.length());
                contador.setText(tamanoString);
            }
        });

    }

    // .:: Salir con dos toques atras ::.
    @Override
    public void onBackPressed() {
        if (tiempoPrimerClick + INTERVALO > System.currentTimeMillis()) {
            super.onBackPressed();
            return;
        } else {
            Toast.makeText(this, "Pulse otra vez para salir", Toast.LENGTH_SHORT).show();
        }
        tiempoPrimerClick = System.currentTimeMillis();
    }

    // .:: Agreagr el menu ::.
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    // .:: Usar los botones del menu ::.
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case R.id.mnConfigCuenta:
                intent = new Intent(this, ConfigCuentaActivity.class);
                startActivity(intent);
                break;
            case R.id.mnAddTxt:
                new FileChooser(this).setFileListener(new FileChooser.FileSelectedListener() {
                    @Override
                    public void fileSelected(File file) {
                        dirFicheroTxt                           = file.getPath();
                        MultiAutoCompleteTextView telefono      = (MultiAutoCompleteTextView) findViewById(R.id.mactvContact);
                        String[] nombreFichero                  = dirFicheroTxt.split("/");
                        telefono.setText(nombreFichero[nombreFichero.length-1].toString());
                        Toast.makeText(MainActivity.this, dirFicheroTxt, Toast.LENGTH_SHORT).show();
                    }
                }).showDialog();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    // .:: Enviar el msg ::.
    public void enviarMsgSimple(View view) {
        String valor = "";

        MultiAutoCompleteTextView telefono  = (MultiAutoCompleteTextView) findViewById(R.id.mactvContact);
        EditText texto                      = (EditText) findViewById(R.id.etMsg);

        SharedPreferences preferences = getSharedPreferences("DatosDeLaCuenta", Context.MODE_PRIVATE);
        String usuario = preferences.getString("Contacto", "ninguno");
        String key = preferences.getString("Clave", "ninguno");

        if (usuario.equals("ninguno") || key.equals("ninguno")) {
            Toast.makeText(MainActivity.this, "No tiene ninguna cuenta registrada.", Toast.LENGTH_SHORT).show();
        } else if (telefono.length() == 0 || texto.length() == 0) {
            Toast.makeText(MainActivity.this, "Asegurase de llenar todos los formularios.", Toast.LENGTH_SHORT).show();
        } else if (texto.length() < 6) {
            Toast.makeText(MainActivity.this, "El mensaje tiene que ser de más de 5 caracteres.", Toast.LENGTH_SHORT).show();
        } else {
            String numListSend;
            if (dirFicheroTxt.equals("")){
                numListSend = numerosEnviar(separarNumeros(telefono.getText().toString()));
            }else {
                numListSend = numerosEnviar(numeroFichero(dirFicheroTxt));
            }

            ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = manager.getActiveNetworkInfo();

            if (networkInfo != null && networkInfo.isConnected()) {
                try {
                    URL url = new
                            URL("http://tem.moises-sms.com/api/sendsms?" +
                            "user=" + usuario +
                            "&key=" + key +
                            "&phone=" + numListSend +
                            "&msg=" + quitaEspacio(texto.getText().toString())
                    );

                    Toast.makeText(MainActivity.this, "Enviando mensaje...", Toast.LENGTH_SHORT).show();

                    ConnectionTask connectionTask = new ConnectionTask();
                    valor = connectionTask.execute(url).get();
                    respuestaServido(valor);

                    telefono.setText("");
                    texto.setText("");

                } catch (Exception e) {
                    Log.e("ERROR-enviarMsgSimple", e.toString());
                    Toast.makeText(MainActivity.this, "Mensaje no enviado.", Toast.LENGTH_SHORT).show();
                }

            } else {
                Toast.makeText(MainActivity.this, "Verifique su conexion a Internet", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // .:: Metodo para leer todos los numeros del fichero ::.
    public ArrayList<String> numeroFichero (String dirFicheroTxt){
        ArrayList<String> lista = new ArrayList<>();
        String numContact;
        try
        {
            BufferedReader bReader = new BufferedReader(new FileReader(dirFicheroTxt));
            while ((numContact = bReader.readLine()) != null){
                lista.add(numContact);
            }
            bReader.close();
        }
        catch (Exception ex)
        {
            Log.e("Ficheros", "Error al leer fichero de TXT");
        }
        return lista;
    }

    // .:: Metodo para separar los numeros de los contactos ::.
    public ArrayList<String> separarNumeros(String text){
        ArrayList<String> result = new ArrayList<>();
        String[] nomb_Num = text.split(", ");
        for (int i = 0; i < nomb_Num.length; i++) {
            String numero = nomb_Num[i].substring(nomb_Num[i].length()-9,nomb_Num[i].length()-1);
            result.add(numero);
        }
        return result;
    }

    // .:: Metodos para preparar los numeros para el envio del msg ::.
    public String numerosEnviar (ArrayList<String> numeros){
        String result = "";
        if (numeros.size() == 1){
            result = numeros.get(0);
        }else{
            for (int i = 0; i < numeros.size(); i++) {
                if(i != numeros.size()-1){
                    result = result + numeros.get(i) + ",";
                }else {
                    result = result + numeros.get(i);
                }
            }
        }
        return result;
    }

    // .:: Metodo para el envio de la url ::.
    public class ConnectionTask extends AsyncTask<URL, Void, String>{
        HttpURLConnection urlConnection;
        BufferedReader reader;
        String respuesta;

        @Override
        protected String doInBackground(URL... url) {
            try {
                urlConnection   = (HttpURLConnection) url[0].openConnection();
                urlConnection.setRequestMethod("POST");
                reader          = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                respuesta       = reader.readLine();
                Log.e("READER", respuesta);
            } catch (IOException e) {
                Log.e("ERROR-ConnectionTask", e.toString());
            }
            return respuesta;
        }
    }

    // .:: Metodo para quitar e espacio en el msg de la url ::.
    public String quitaEspacio (String text){
        return text.replaceAll(" ","%20");
    }

    // .:: Metodo para el trabajo con las respuestas del servidor ::.
    public void respuestaServido(String valor){
        if (valor.length() > 4){
            Toast.makeText(MainActivity.this, "Mensaje enviado correctamente.", Toast.LENGTH_SHORT).show();
        }
        switch (valor){
            case "666":
                Toast.makeText(MainActivity.this, "Error desconocido", Toast.LENGTH_SHORT).show();
                break;
            case "6":
                Toast.makeText(MainActivity.this, "Error: El mensaje debe tener al menos 5 caracteres.", Toast.LENGTH_SHORT).show();
                break;
            case "7":
                Toast.makeText(MainActivity.this, "Error: El mensaje debe tener máximo 155 caracteres.", Toast.LENGTH_SHORT).show();
                break;
            case "8":
                Toast.makeText(MainActivity.this, "Error: Usuario no registrado.", Toast.LENGTH_SHORT).show();
                break;
            case "9":
                Toast.makeText(MainActivity.this, "Error: Su cuenta no está activa.", Toast.LENGTH_SHORT).show();
                break;
            case "12":
                Toast.makeText(MainActivity.this, "Error: Los números celulares en Cuba tienen 8 dígitos.", Toast.LENGTH_SHORT).show();
                break;
            case "13":
                Toast.makeText(MainActivity.this, "Error en la base de datos, contacte al administrador.", Toast.LENGTH_SHORT).show();
                break;
            case "81":
                Toast.makeText(MainActivity.this, "Error: Su cuenta no tiene el servicio de API activado.", Toast.LENGTH_SHORT).show();
                break;
            case "83":
                Toast.makeText(MainActivity.this, "Error: Clave Incorrecta.", Toast.LENGTH_SHORT).show();
                break;
            case "111":
                Toast.makeText(MainActivity.this, "Error: No tiene saldo suficiente. Debe recargar su cuenta.", Toast.LENGTH_SHORT).show();
                break;
            case "112":
                Toast.makeText(MainActivity.this, "Error: Su cuenta no tiene saldo.", Toast.LENGTH_SHORT).show();
                break;
        }
    }

}