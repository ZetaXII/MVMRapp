package com.example.mvmrapp;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HttpGetXmlFiles
{
    // --- VARIABILI E COSTANTI DELLA CLASSE ---

    // Definisco l'Endpoint per la richiesta al servizio getXMLFiles fornito dal Server MVMR
    private static final String URL = "http://10.0.2.2:8080/loadXML/getXMLFiles";                                                                                                                       // Endpoint alternativo nel caso il Server MVMR fosse in esecuzione su una macchina esterna al dispositivo (l'IP indica quello del server) //private static final String URL = "http://192.168.1.56:8080/loadXML/getXMLFiles";

    // Definisco il tipo di media per i file XML
    private static final MediaType XML_MEDIA_TYPE = MediaType.parse("application/xml");

    // TAG per il logging
    private static final String TAG = "HttpGetXmlFiles";

    // Client OkHttp per eseguire le richieste HTTP
    private OkHttpClient client;

    // Context dell'Activity corrente
    private Context context;

    // Costruttore della classe
    public HttpGetXmlFiles(Context context)
    {
        // Inizializzazione del client OkHttp
        this.client = new OkHttpClient();
        // Inizializzazione del Context
        this.context = context;
    }

    /**
     * Metodo per eseguire la richiesta POST al server con i file XML.
     *
     * @param flagNmap     Flag per indicare se è stato selezionato un file NMAP
     * @param flagNessus   Flag per indicare se è stato selezionato un file NESSUS
     * @param flagOpenvas  Flag per indicare se è stato selezionato un file OPENVAS
     * @param flagOwaspzap Flag per indicare se è stato selezionato un file OWASPZAP
     * @param fileNmap     Oggetto File per il file NMAP
     * @param fileNessus   Oggetto File per il file NESSUS
     * @param fileOpenvas  Oggetto File per il file OPENVAS
     * @param fileOwaspzap Oggetto File per il file OWASPZAP
     */

    public void postRequest(boolean flagNmap, boolean flagNessus, boolean flagOpenvas, boolean flagOwaspzap,
                            File fileNmap, File fileNessus, File fileOpenvas, File fileOwaspzap)
    {
        // Se il flag di un certo tool di report è false, il File XML di quel report non è stato selezionato
        new PostRequestTask(flagNmap, flagNessus, flagOpenvas, flagOwaspzap,
                fileNmap, fileNessus, fileOpenvas, fileOwaspzap).execute();
    }

    // Viene usato AsyncTask per eseguire la richiesta POST in background.
    private class PostRequestTask extends AsyncTask<Void, Void, String>
    {
        private boolean flagNmap;
        private boolean flagNessus;
        private boolean flagOpenvas;
        private boolean flagOwaspzap;
        private File fileNmap;
        private File fileNessus;
        private File fileOpenvas;
        private File fileOwaspzap;

        public PostRequestTask(boolean flagNmap, boolean flagNessus, boolean flagOpenvas, boolean flagOwaspzap,
                               File fileNmap, File fileNessus, File fileOpenvas, File fileOwaspzap)
        {
            this.flagNmap = flagNmap;
            this.flagNessus = flagNessus;
            this.flagOpenvas = flagOpenvas;
            this.flagOwaspzap = flagOwaspzap;
            this.fileNmap = fileNmap;
            this.fileNessus = fileNessus;
            this.fileOpenvas = fileOpenvas;
            this.fileOwaspzap = fileOwaspzap;
        }

        @Override
        protected String doInBackground(Void... voids)
        {
            try
            {
                // Costruzione del corpo della richiesta multipart
                MultipartBody.Builder requestBodyBuilder = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("flag_nmap", String.valueOf(flagNmap))
                        .addFormDataPart("flag_nessus", String.valueOf(flagNessus))
                        .addFormDataPart("flag_openvas", String.valueOf(flagOpenvas))
                        .addFormDataPart("flag_owaspzap", String.valueOf(flagOwaspzap));

                // Aggiunta dei file XML se presenti

                //Per NMAP
                if (fileNmap != null && fileNmap.exists())
                {
                    requestBodyBuilder.addFormDataPart("nmap", "nmap.xml", RequestBody.create(XML_MEDIA_TYPE, fileNmap));
                }
                else
                {
                    Log.d(TAG, "File NMAP non trovato o non valido.");
                }

                //Per NESSUS
                if (fileNessus != null && fileNessus.exists())
                {
                    requestBodyBuilder.addFormDataPart("nessus", "nessus.xml", RequestBody.create(XML_MEDIA_TYPE, fileNessus));
                }
                else
                {
                    Log.d(TAG, "File NESSUS non trovato o non valido.");
                }

                //Per OPENVAS
                if (fileOpenvas != null && fileOpenvas.exists())
                {
                    requestBodyBuilder.addFormDataPart("openvas", "openvas.xml", RequestBody.create(XML_MEDIA_TYPE, fileOpenvas));
                } else
                {
                    Log.d(TAG, "File OPENVAS non trovato o non valido.");
                }

                //Per OWASPZAP
                if (fileOwaspzap != null && fileOwaspzap.exists())
                {
                    requestBodyBuilder.addFormDataPart("owaspzap", "owaspzap.xml", RequestBody.create(XML_MEDIA_TYPE, fileOwaspzap));
                } else
                {
                    Log.d(TAG, "File OWASPAZ non trovato o non valido.");
                }

                // Costruzione della richiesta HTTP POST prendendo i file XML aggiunti al requestBodyBuilder
                RequestBody requestBody = requestBodyBuilder.build();
                Request request = new Request.Builder().url(URL).post(requestBody).build();

                // Invio dei dati al Server MVMR lanciando execute()
                try (Response response = client.newCall(request).execute())
                {
                    // Se la Risposta è negativa allora stampa l'errore relativo alla risposta
                    if (!response.isSuccessful())
                    {
                        return "Errore durante la richiesta: " + response.message();
                    }

                    // Altrimenti ritorna la Risposta affermativa
                    String responseBody = response.body().string();
                    return "Successo: " + responseBody;
                }
            }
            catch (IOException e)
            {
                return "Errore durante la richiesta: " + e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(String result)
        {
            // Gestione del risultato della richiesta
            if (result.startsWith("Errore") || result.isEmpty())
            {
                // In caso di Errore, viene Lanciato l'ErrorActivity
                Intent intent = new Intent(context, ErrorActivity.class);
                context.startActivity(intent);
            }
            else
            {
                /*  In caso di Successo, si passa alla classe successiva HttpWebScraping
                    che ha il compito di richiedere al Server MVMR l'attività di WebScraping
                    passando come parametro il Merge dei file di Report XML passati in precedenza
                */
                HttpWebScraping httpWebScraping = new HttpWebScraping(context);
                String body = "x_";
                httpWebScraping.postRequest(body);
            }
        }
    }
}