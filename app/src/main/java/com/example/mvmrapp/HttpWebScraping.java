package com.example.mvmrapp;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import java.io.IOException;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HttpWebScraping
{
    // --- VARIABILI E COSTANTI DELLA CLASSE ---

    // Definisco l'Endpoint per la richiesta al servizio webScraping fornito dal Server MVMR
    private static final String URL = "http://10.0.2.2:8080/webScraping";                                                                                          //private static final String URL = "http://192.168.1.56:8080/webScraping"; // URL per la richiesta al server

    // Definisco il tipo di media per i file JSON
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json");

    // TAG per il logging
    private static final String TAG = "HttpWebScraping";

    // Client OkHttp per eseguire le richieste HTTP
    private OkHttpClient client;

    // Context dell'Activity corrente
    private Context context;

    // Costruttore della classe
    public HttpWebScraping(Context context)
    {
        // Inizializzazione del client OkHttp
        this.client = new OkHttpClient();
        // Inizializzazione del Context
        this.context = context;
    }

    public void postRequest(String body)
    {
        new PostRequestTask(body).execute();
    }

    // Viene usato AsyncTask per eseguire la richiesta POST in background.
    private class PostRequestTask extends AsyncTask<Void, Void, String>
    {
        private String body;

        public PostRequestTask(String body)
        {
            this.body = body;
        }

        @Override
        protected String doInBackground(Void... voids)
        {
            try
            {
                // Costruzione del corpo della richiesta
                String filename = "x_summary_0.json";
                String json = "{\"x_summary_filename\":\"" + filename + "\"}";
                                                                                                                                            Log.d(TAG, "JSON x_summary_0: " + json);
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
        protected void onPostExecute(String result) {

            // Gestione del risultato della richiesta
            Log.d(TAG, "[GET_WEB_SCRAPING] Risultato della richiesta: " + result);

            if (result.startsWith("Errore") || result.isEmpty())
            {
                // In caso di Errore, viene Lanciato l'ErrorActivity
                Intent intent = new Intent(context, ErrorActivity.class);
                context.startActivity(intent);
            }
            else
            {
                /*  In caso di Successo, si passa alla classe successiva HttpGetAi
                    che ha il compito di richiedere al Server MVMR l'attività del BayesianCLassifier
                    indicando al Server  MVMR di utilizzare il file "z_summary_0,json" prodotto qui
                */
                HttpGetAi httpGetAi = new HttpGetAi(context);
                String body = "z_";
                httpGetAi.postRequest(body);
            }
        }
    }
}