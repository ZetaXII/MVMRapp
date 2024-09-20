package com.example.mvmrapp;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity
{
    // Definizione delle variabili
    private static final int PICK_XML_FILE = 1;
    private String uriNmap;
    private String uriNessus;
    private String uriOpenvas;
    private String uriOwaspzap;
    private boolean flag_nmap = false;
    private boolean flag_nessus = false;
    private boolean flag_openvas = false;
    private boolean flag_owaspzap = false;
    private CardView cardNmap = null;
    private CardView cardNessus = null;
    private CardView cardOpenvas = null;
    private CardView cardOwaspzap = null;
    private TableLayout fileTable = null;
    private CardView lastClickedCardView = null;
    private TextView tvNoFileUpload = null;
    private Button searchVulnerabilitiesButton = null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Disattivo la tabella dei report selezionati
        fileTable = findViewById(R.id.fileTable);
        deActivateView(fileTable);

        // Disattivo il bottone dell'inoltro dei report
        searchVulnerabilitiesButton = findViewById(R.id.searchVulnerabilitiesButton);
        deActivateView(searchVulnerabilitiesButton);

        // Recupero la view del no file upload
        tvNoFileUpload = findViewById(R.id.tvNoFileUpload);
        cardNmap = findViewById(R.id.cardNmap);
        cardNessus = findViewById(R.id.cardNessus);
        cardOpenvas = findViewById(R.id.cardOpenvas);
        cardOwaspzap = findViewById(R.id.cardOwaspzap);

        // Imposto la funzione che dovranno eseguire se cliccate
        cardNmap.setOnClickListener(v -> handleClickForFlag(cardNmap));
        cardNessus.setOnClickListener(v -> handleClickForFlag(cardNessus));
        cardOpenvas.setOnClickListener(v -> handleClickForFlag(cardOpenvas));
        cardOwaspzap.setOnClickListener(v -> handleClickForFlag(cardOwaspzap));
    }

    /*-----[INIZIO FUNZIONI PER LA TABELLA]-----*/

    // Metodo per aggiungere una riga alla tabella dei file
    private void addRow(String toolName, String fileName)
    {
        TableRow tableRow = new TableRow(this);
        TableRow.LayoutParams layoutParams = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT,
                                                                       TableRow.LayoutParams.WRAP_CONTENT);

        // Crea la cella per il nome del tool
        TextView toolNameCell = new TextView(this);
        toolNameCell.setLayoutParams(layoutParams);
        toolNameCell.setText(toolName);

        // Crea la cella per il nome del file
        TextView fileNameCell = new TextView(this);
        fileNameCell.setLayoutParams(layoutParams);

        // Stampa solo il percorso del file dopo lo slash e i primi numChar caratteri:

        // Creo una variabile che imposta il limite di n caratteri
        int numChar = 16;
        String primiCharFileName = fileName.substring(fileName.lastIndexOf("/") + 1);
        if(primiCharFileName.length() > numChar)
        {
            // Ottieni la sottostringa che contiene i primi numChar caratteri
            primiCharFileName = primiCharFileName.substring(0, numChar)+"...";
        }
        fileNameCell.setText(primiCharFileName);

        // Crea la cella per il pulsante che rimuove il file selezionato
        Button removeButton = new Button(this);
        removeButton.setLayoutParams(layoutParams);
        removeButton.setText("X");

        // Imposta il funzionamento del pulsante
        removeButton.setOnClickListener(v ->
        {
            // Quando si cancella un file dalla lista, automaticamente si riattiva la card e se la
            // tabella non contiene più nulla si disattiva anch'essa insieme al suo bottone
            // "cerca vulnerabilità" e si riattiva la scritta "Nessun file caricato."
            if (toolName.equalsIgnoreCase("NMAP"))
            {
                activateCard(cardNmap);
                uriNmap = null;
                flag_nmap = false;
            }
            if (toolName.equalsIgnoreCase("NESSUS"))
            {
                activateCard(cardNessus);
                uriNessus = null;
                flag_nessus = false;
            }
            if (toolName.equalsIgnoreCase("OPENVAS"))
            {
                activateCard(cardOpenvas);
                uriOpenvas = null;
                flag_openvas = false;
            }
            if (toolName.equalsIgnoreCase("OWASPZAP"))
            {
                activateCard(cardOwaspzap);
                uriOwaspzap = null;
                flag_owaspzap = false;
            }

            // Rimuovi la riga dalla tabella
            fileTable.removeView(tableRow);
            if (checkIfTableLayoutIsEmpty(fileTable))
            {
                deActivateView(searchVulnerabilitiesButton);
                activateView(tvNoFileUpload);
            }
        });

        // Aggiungi celle alla riga
        tableRow.addView(toolNameCell);
        tableRow.addView(fileNameCell);
        tableRow.addView(removeButton);

        // Aggiungi riga alla tabella
        fileTable.addView(tableRow);
    }

    // Metodo per verificare se la TableLayout è vuota
    private boolean checkIfTableLayoutIsEmpty(TableLayout tableLayout)
    {
        int childCount = tableLayout.getChildCount();
        boolean isEmpty = true;
        for (int i = 0; i < childCount; i++)
        {
            View child = tableLayout.getChildAt(i);
            if (child instanceof TableRow)
            {
                TableRow row = (TableRow) child;
                if (row.getChildCount() > 0)
                {
                    isEmpty = false;
                    break;
                }
            }
        }
        return isEmpty;
    }
    /*-----[FINE FUNZIONI PER LA TABELLA]-----*/

    // Metodo per gestire il click sulle CardView
    private void handleClickForFlag(CardView cardView)
    {
        openFilePicker();
        setFlagForCard(cardView);
        lastClickedCardView = cardView;
    }

    // Metodo per aprire il picker di file
    private void openFilePicker()
    {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("text/xml");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Select XML File"), PICK_XML_FILE);
    }

    // Metodo per impostare i flag basati sulla CardView
    private void setFlagForCard(CardView cardView)
    {
        if (cardView.getId() == R.id.cardNmap)
        {
            flag_nmap = true;
        }
        else if (cardView.getId() == R.id.cardNessus)
        {
            flag_nessus = true;
        }
        else if (cardView.getId() == R.id.cardOpenvas)
        {
            flag_openvas = true;
        }
        else if (cardView.getId() == R.id.cardOwaspzap)
        {
            flag_owaspzap = true;
        }
    }

    // Metodo per attivare una vista
    private void activateView(View view)
    {
        if (view.getVisibility() == View.GONE)
        {
            view.setVisibility(View.VISIBLE);
        }
    }

    // Metodo per disattivare una vista
    private void deActivateView(View view)
    {
        if (view.getVisibility() == View.VISIBLE)
        {
            view.setVisibility(View.GONE);
        }
    }

    // Metodo per disattivare una CardView
    private void deActivateCard(CardView cardView)
    {
        cardView.setAlpha(0.3f);
        cardView.setEnabled(false);
    }

    // Metodo per attivare una CardView
    private void activateCard(CardView cardView)
    {
        cardView.setAlpha(1f);
        cardView.setEnabled(true);
    }

    // Metodo per impostare il percorso del file per una CardView
    private void setPathForCard(CardView cardView, Intent data)
    {
        Uri uri = data.getData();
        if (uri != null)
        {
            String toolName = null;
            if (cardView.getId() == R.id.cardNmap)
            {
                toolName = "NMAP";
            }
            else if (cardView.getId() == R.id.cardNessus)
            {
                toolName = "NESSUS";
            }
            else if (cardView.getId() == R.id.cardOpenvas)
            {
                toolName = "OPENVAS";
            }
            else if (cardView.getId() == R.id.cardOwaspzap)
            {
                toolName = "OWASPZAP";
            }
            if (toolName != null)
            {
                try
                {
                    String fileName = getFileNameFromUri(this, uri);
                    addRow(toolName, fileName);
                    // Disattiva la card del tool selezionato, mostra tabella e bottone di ricerca
                    deActivateCard(cardView);
                    deActivateView(tvNoFileUpload);
                    activateView(fileTable);
                    activateView(searchVulnerabilitiesButton);
                    // Salva l'URI e lo associa ad uno dei 4 TOOL selezionato
                    saveUriForTool(toolName, uri);
                    Toast.makeText(getApplicationContext(), "File " + toolName +
                                   " Selezionato", Toast.LENGTH_LONG).show();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    Toast.makeText(this, "Failed to get file name",
                                   Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    // Metodo per ottenere il nome del file dall'URI
    private String getFileNameFromUri(Context context, Uri uri)
    {
        String fileName = null;
        Cursor cursor = null;
        try
        {
            ContentResolver contentResolver = context.getContentResolver();
            cursor = contentResolver.query(uri, null, null,
                       null, null);
            if (cursor != null && cursor.moveToFirst())
            {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (nameIndex != -1)
                {
                    fileName = cursor.getString(nameIndex);
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            if (cursor != null)
            {
                cursor.close();
            }
        }
        return fileName != null ? fileName : "tempfile_" + System.currentTimeMillis();
    }

    private File getFileFromContentUri(Context context, Uri uri)
    {
        if (uri == null)
        {
            return null;
        }

        // Solo per Uri di tipo content
        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme()))
        {
            return copyUriToFile(context, uri);
        }

        return null;
    }

    private File copyUriToFile(Context context, Uri uri)
    {
        File tempFile = null;
        InputStream inputStream = null;
        OutputStream outputStream = null;

        try
        {
            ContentResolver contentResolver = context.getContentResolver();
            inputStream = contentResolver.openInputStream(uri);
            if (inputStream != null)
            {
                // Crea un file temporaneo nella cache dell'app
                String fileName = getFileNameFromUri(context, uri);
                tempFile = new File(context.getCacheDir(), fileName != null ?
                           fileName : "tempfile_" + System.currentTimeMillis());
                outputStream = new FileOutputStream(tempFile);

                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) > 0)
                {
                    outputStream.write(buffer, 0, length);
                }
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
            tempFile = null;
        }
        finally
        {
            // Chiudi gli stream
            try
            {
                if (inputStream != null)
                {
                    inputStream.close();
                }
                if (outputStream != null)
                {
                    outputStream.close();
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        return tempFile;
    }

    // Metodo per salvare l'URI per uno strumento
    private void saveUriForTool(String toolName, Uri uri)
    {
        if (toolName.equalsIgnoreCase("NMAP"))
        {
            uriNmap = uri.toString();
        }
        else if (toolName.equalsIgnoreCase("NESSUS"))
        {
            uriNessus = uri.toString();
        }
        else if (toolName.equalsIgnoreCase("OPENVAS"))
        {
            uriOpenvas = uri.toString();
        }
        else if (toolName.equalsIgnoreCase("OWASPZAP"))
        {
            uriOwaspzap = uri.toString();
        }
    }

    // Metodo per la ricerca delle vulnerabilità
    // Specifico il comportamento dopo il click del tasto "Cerca Vulnerabilità"
    public void searchVulnerabilitiesOnClick(View view)
    {
        // Converti i path dei file selezionati in oggetti File XML
        File fileNmap = null;
        File fileNessus = null;
        File fileOpenvas = null;
        File fileOwaspzap = null;

        if (uriNmap != null)
        {
            fileNmap = getFileFromContentUri(this, Uri.parse(uriNmap));                                                                             Log.d("MainActivity", "FFFFFFFFFFFFFFFFFFFFFFFFFF fileNmapASSSSSOLUTOOO: " + fileNmap.getAbsolutePath());
        }

        if (uriNessus != null)
        {
            fileNessus = getFileFromContentUri(this, Uri.parse(uriNessus));                                                                         Log.d("MainActivity", "FFFFFFFFFFFFFFFFFFFFFFFFFF fileNessusASSSSSOLUTOOO: " + fileNessus.getAbsolutePath());
        }

        if (uriOpenvas != null)
        {
            fileOpenvas = getFileFromContentUri(this, Uri.parse(uriOpenvas));                                                                       Log.d("MainActivity", "FFFFFFFFFFFFFFFFFFFFFFFFFF fileOpenvasASSSSSOLUTOOO: " + fileOpenvas.getAbsolutePath());
        }

        if (uriOwaspzap != null)
        {
            fileOwaspzap = getFileFromContentUri(this, Uri.parse(uriOwaspzap));                                                                     Log.d("MainActivity", "FFFFFFFFFFFFFFFFFFFFFFFFFF fileOwaspzapASSSSSOLUTOOO: " + fileOwaspzap.getAbsolutePath());
        }

        // Esegui la richiesta HTTP utilizzando il middleware
        HttpGetXmlFiles httpGetXmlFiles = new HttpGetXmlFiles(this);
        httpGetXmlFiles.postRequest(flag_nmap, flag_nessus, flag_openvas, flag_owaspzap,
                                    fileNmap, fileNessus, fileOpenvas, fileOwaspzap);

        // Mostra un toast per confermare il click sul pulsante
        Toast.makeText(this, "Avvio ricerca vulnerabilità", Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_XML_FILE && resultCode == RESULT_OK)
        {
            if (data != null)
            {
                setPathForCard(lastClickedCardView, data);
            }
        }
    }
}
