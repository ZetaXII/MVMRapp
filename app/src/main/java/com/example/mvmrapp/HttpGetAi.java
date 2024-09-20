package com.example.mvmrapp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HttpGetAi
{
    // --- VARIABILI E COSTANTI DELLA CLASSE ---

    // Definisco l'Endpoint per la richiesta al servizio getAi fornito dal Server MVMR
    private static final String URL = "http://10.0.2.2:8080/ai/";                                                                                                               //private static final String URL = "http://192.168.1.56:8080/ai/"; // URL per la richiesta al server

    // Definisco il tipo di media per i file JSON
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json");

    // TAG per il logging
    private static final String TAG = "HttpGetAi";

    // Client OkHttp per eseguire le richieste HTTP
    private OkHttpClient client;

    // Context dell'Activity corrente
    private Context context;

    // Timeout impostato a 3 minuti (180 secondi) per effettuare la richiesta al Server
    private static final int TIMEOUT_SECONDS = 180;

    private AlertDialog progressDialog;

    // Costruttore della classe
    public HttpGetAi(Context context)
    {
        // Inizializzazione del client OkHttp
        this.client = new OkHttpClient.Builder()
                .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build();

        // Inizializzazione del Context
        this.context = context;
        initProgressDialog();
    }

    // Inizializzazione della Dialog del Caricamento
    private void initProgressDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.progress_dialog, null);
        builder.setView(dialogView);
        progressDialog = builder.create();
        progressDialog.setCancelable(false);
    }

    public void postRequest(String body)
    {
        new PostRequestTask(body).execute();
    }

    // Viene usato AsyncTask per eseguire la richiesta POST in background
    private class PostRequestTask extends AsyncTask<Void, Void, String>
    {
        private String body;

        public PostRequestTask(String body)
        {
            this.body = body;
        }

        // Viene chiamato prima di iniziare l'operazione
        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();
            // Mostra il ProgressDialog prima di iniziare l'operazione
            progressDialog.show();
        }

        @Override
        protected String doInBackground(Void... voids)
        {
            try
            {
                // Costruzione del corpo della richiesta
                String filename = "z_summary_0.json";
                String json = "{\"z_summary_filename\":\"" + filename + "\"}";                                                                  //Log.d(TAG, "JSON z_summary_0: " + json);

                RequestBody requestBody = RequestBody.create(json, JSON_MEDIA_TYPE);

                // Costruzione della richiesta HTTP POST
                Request request = new Request.Builder()
                        .url(URL)
                        .post(requestBody)
                        .build();

                // Invio dei dati al Server MVMR lanciando execute()
                try (Response response = client.newCall(request).execute())
                {
                    // Se la Risposta è positiva allora ritorna la risposta affermativa
                    if (response.isSuccessful())
                    {
                        Log.d(TAG, "Richiesta POST riuscita");
                        // Gestione della risposta
                        String responseBody = response.body().string();
                        return responseBody;
                    }
                    // Altrimenti stampa l'errore relativo alla risposta
                    else
                    {
                        Log.e(TAG, "Errore durante la richiesta POST: " + response.code() +
                                " - " + response.message());
                    }
                }
            }
            catch (IOException e)
            {
                return "Errore durante la richiesta: " + e.getMessage();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result)
        {
            // Nasconde la ProgressDialog quando l'operazione è completata
            if (progressDialog.isShowing()) {
                progressDialog.dismiss();
            }

            // Gestione del risultato della richiesta
            Log.d(TAG, "[GET_AI] Risultato della richiesta: " + result);

            if (result.startsWith("Errore") || result.isEmpty())
            {
                // In caso di Errore, viene Lanciato l'ErrorActivity
                Intent intent = new Intent(context, ErrorActivity.class);
                context.startActivity(intent);
            }
            else
            {
                /*  In caso di Successo, il Middleware termina la sua esecuzione e
                    restituisce al client il risultato della classificazione generato
                    dal Server MVMR tramite la ResultActivity
                */
                Intent intent = new Intent(context, ResultActivity.class);
                intent.putExtra("HTTP_RESULT", result);
                context.startActivity(intent);
            }
        }
    }
}