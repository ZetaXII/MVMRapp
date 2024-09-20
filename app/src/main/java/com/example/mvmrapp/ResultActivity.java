package com.example.mvmrapp;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.Environment;
import android.text.Html;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.Spanned;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;

public class ResultActivity extends AppCompatActivity
{
    // Variabili e costanti
    private static final String TAG = "ResultActivity";
    private static final int PERMISSION_REQUEST_CODE = 100;
    private String formattedResult;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        // Recupero l'Intent che ha avviato l'Activity
        Intent intent = getIntent();
        String httpResult = intent.getStringExtra("HTTP_RESULT");                               /* Log del risultato HTTP per il debug */ Log.d(TAG, "HTTP Result: " + httpResult);

        // Formatto il risultato JSON
        formattedResult = formatJsonResult(httpResult);                                                /* Log del risultato HTTP formattato */ Log.d(TAG, "HTTP Result leggibile: " + formattedResult);

        // Utilizzo il risultato (ad esempio, impostare un TextView)
        TextView textView = findViewById(R.id.text_view_result);

        // Uso Html.fromHtml() per interpretare il testo HTML
        textView.setText(Html.fromHtml(formattedResult, Html.FROM_HTML_MODE_LEGACY));
    }

    // Metodo per gestire il click sul bottone newScan
    public void newScan(View view)
    {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    public void exportReportAsPdf(View view)
    {
        if (ContextCompat.checkSelfPermission
                (this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE);
        }
        else
        {
            exportToPdf();
        }
    }

    /*@Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE)
        {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                exportToPdf();
            }
            else
            {
                Log.e(TAG, "Permesso di scrittura non concesso.");
            }
        }
    }*/

    public void exportToPdf()
    {
        if (formattedResult == null || formattedResult.trim().isEmpty())
        {
            Log.e(TAG, "Il testo formattato è vuoto.");
            Toast.makeText(this, "Nessun testo da esportare.", Toast.LENGTH_LONG).show();
            return;
        }
        Toast.makeText(this, "Esportazione in corso...", Toast.LENGTH_LONG).show();
        PdfDocument pdfDocument = new PdfDocument();
        int pageWidth = 595; // Larghezza della pagina A4 in punti
        int pageHeight = 842; // Altezza della pagina A4 in punti
        int margin = 40; // Margini della pagina
        int textSize = 12; // Dimensione del testo
        TextPaint textPaint = new TextPaint();
        textPaint.setTextSize(textSize);
        textPaint.setColor(android.graphics.Color.BLACK);
        // Converto l'HTML in uno Spanned
        Spanned spannedResult = Html.fromHtml(formattedResult, Html.FROM_HTML_MODE_LEGACY);
        // Creo un StaticLayout per gestire il testo
        StaticLayout staticLayout = StaticLayout.Builder.obtain(spannedResult, 0,
                                   spannedResult.length(), textPaint, pageWidth - 2 * margin)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(1.0f, 1.0f)
                .setIncludePad(false)
                .build();

        int lineHeight = staticLayout.getHeight() / staticLayout.getLineCount(); // Altezza riga
        int linesPerPage = (pageHeight - 2 * margin) / lineHeight; // Calcola le righe per pagina
        int totalLines = staticLayout.getLineCount();
        int startLine = 0;
        int pageNumber = 1;
        while (startLine < totalLines)
        {
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight,
                                                                             pageNumber).create();
            PdfDocument.Page page = pdfDocument.startPage(pageInfo);
            android.graphics.Canvas canvas = page.getCanvas();
            canvas.save();
            canvas.translate(margin, margin);
            // Disegna il testo per la pagina corrente
            int endLine = Math.min(startLine + linesPerPage, totalLines);
            for (int line = startLine; line < endLine; line++)
            {
                int lineStart = staticLayout.getLineStart(line);
                int lineEnd = staticLayout.getLineEnd(line);
                float lineTop = staticLayout.getLineTop(line) - staticLayout.getLineTop(startLine);
                canvas.drawText(spannedResult, lineStart, lineEnd, 0, lineTop, textPaint);
            }
            canvas.restore();
            pdfDocument.finishPage(page);
            startLine = endLine;
            pageNumber++;
        }

        // Salvataggio del PDF
        File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "PDFs");
        if (!directory.exists())
        {
            directory.mkdirs();
        }
        String timestamp = String.valueOf(System.currentTimeMillis());
        File file = new File(directory, "result_" + timestamp + ".pdf");

        try (OutputStream outputStream = new FileOutputStream(file))
        {
            pdfDocument.writeTo(outputStream);
            Log.d(TAG, "PDF salvato in: " + file.getAbsolutePath());

            // Mostra un Toast di conferma
            runOnUiThread(() -> Toast.makeText(ResultActivity.this, "PDF salvato in: "
                                               + file.getAbsolutePath(), Toast.LENGTH_LONG).show());
        }
        catch (IOException e)
        {
            // Mostra un Toast di errore
            runOnUiThread(() -> Toast.makeText(ResultActivity.this,
                    "Errore durante il salvataggio del PDF", Toast.LENGTH_LONG).show());                                    //Log.e(TAG, "Errore durante il salvataggio del PDF", e);
        }
        finally
        {
            pdfDocument.close();
        }
    }

    private String formatJsonResult(String jsonString)
    {
        StringBuilder formattedString = new StringBuilder();
        try
        {
            JSONObject json = new JSONObject(jsonString);
            JSONObject zSummary = json.optJSONObject("z_summary");
            if (zSummary != null) {
                JSONObject vulnerabilities = zSummary.optJSONObject("vulnerabilities");

                if (vulnerabilities != null) {
                    Iterator<String> domainKeys = vulnerabilities.keys();
                    while (domainKeys.hasNext()) {
                        String domainKey = domainKeys.next();
                        JSONObject domainVulnerabilities = vulnerabilities.optJSONObject(domainKey);

                        if (domainVulnerabilities != null) {
                            Iterator<String> portKeys = domainVulnerabilities.keys();
                            while (portKeys.hasNext()) {
                                String portKey = portKeys.next();
                                formattedString.append(processPortVulnerabilities(domainVulnerabilities,
                                                                                  portKey));
                            }
                        } else {
                            formattedString.append("Chiave dominio '").append(domainKey)
                                                                      .append("' mancante.\n");
                        }
                    }
                } else {
                    formattedString.append("Chiave 'vulnerabilities' mancante.\n");
                }
            } else {
                formattedString.append("Chiave 'z_summary' mancante.\n");
            }
        } catch (JSONException e) {
            Log.e(TAG, "Errore nel parsing del JSON", e);
            formattedString.append("Errore nel parsing del JSON: ").append(e.getMessage()).append("\n");
        }

        return formattedString.toString();
    }

    private String processPortVulnerabilities(JSONObject domainVulnerabilities, String port)
    {
        StringBuilder details = new StringBuilder();
        JSONObject vulnerabilitiesObject = domainVulnerabilities.optJSONObject(port);
        if (vulnerabilitiesObject != null) {
            details.append("<b>Porta ").append(port).append(":</b><br>");
            Iterator<String> keys = vulnerabilitiesObject.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                JSONArray vulnerabilitiesArray = vulnerabilitiesObject.optJSONArray(key);
                if (vulnerabilitiesArray != null) {
                    for (int i = 0; i < vulnerabilitiesArray.length(); i++) {
                        JSONObject vulnerability = vulnerabilitiesArray.optJSONObject(i);
                        if (vulnerability != null) {
                            details.append(formatVulnerabilityDetails(vulnerability)).append("<br>");
                        }
                    }
                }
            }
        } else {
            details.append("<b>Porta ").append(port).append(":</b><br>Nessuna informazione disponibile.<br>");
        }
        return details.toString();
    }

    private String formatVulnerabilityDetails(JSONObject vulnerability)
    {
        StringBuilder details = new StringBuilder();
        try {
            String description = vulnerability.optString("description", "N/A");
            String risk = vulnerability.optString("risk", "N/A");
            String type = vulnerability.optString("type", "N/A");
            String solution = vulnerability.optString("solution", "N/A");
            JSONArray refs = vulnerability.optJSONArray("refs");

            String color = getColorForRisk(risk);

            details.append("Descrizione: ").append(description).append("<br>")
                    .append("Rischio: <font color='").append(color).append("'>").append(risk)
                    .append("</font><br>")
                    .append("Tipo: ").append(type).append("<br>");
            if (!"n/a".equalsIgnoreCase(solution)) {
                details.append("Soluzione: ").append(solution).append("<br>");
            }
            if (refs != null && refs.length() > 0) {
                details.append("Riferimenti: ");
                for (int i = 0; i < refs.length(); i++) {
                    details.append(refs.getString(i));
                    if (i < refs.length() - 1) {
                        details.append(", ");
                    }
                }
                details.append("<br>");
            }
        } catch (JSONException e) {
            Log.e(TAG, "Errore nel parsing dei dettagli della vulnerabilità", e);
            details.append("Errore nel parsing dei dettagli.<br>");
        }

        return details.toString();
    }

    private String getColorForRisk(String risk)
    {
        switch (risk.toLowerCase()) {
            case "critical":
                return "#ad0000"; // Rosso
            case "high":
                return "#ff6a00"; // Arancione
            case "medium":
                return "#ffae00"; // Giallo
            case "low":
                return "#30b504"; // Verde
            case "unknown":
                return "#8f1a89"; // Viola
            case "info":
                return "#008cff"; // Azzurro
            default:
                return "#8f1a88"; // Viola
        }
    }
}
