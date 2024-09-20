package com.example.mvmrapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class ErrorActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_error);
    }

    // Metodo per gestire il click sul bottone riprova
    public void tryAgain(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        // Imposta i flag per evitare la duplicazione della MainActivity nella pila di attività.
        // FLAG_ACTIVITY_CLEAR_TOP: Se la MainActivity è già in esecuzione, tutte le attività sopra di essa verranno chiuse.
        // FLAG_ACTIVITY_NEW_TASK: Se la MainActivity esiste già in un'altra task, verrà spostata in cima a quella task.
        // Questo assicura che la MainActivity venga riportata in primo piano e tutte le attività intermedie vengano chiuse.
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish(); // Opzionale, chiude l'activity corrente
    }
}
