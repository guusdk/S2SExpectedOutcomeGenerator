package nl.goodbytes.xmpp.tools.output;

import nl.goodbytes.xmpp.tools.ExpectedOutcome;
import nl.goodbytes.xmpp.tools.ServerSettings;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

public class HtmlTableOutputter implements Outputter
{
    private ServerSettings previousLocalServerSettings;

    private int entryCount = 0;
    private Path htmlFile;
    @Override
    public void init(final Collection<ServerSettings> localServerSettings, final Collection<ServerSettings> remoteServerSettings)
    {
        try {
            htmlFile = Files.createTempFile("s2s-outcomes", ".html");

            Files.writeString(htmlFile, "<html><head><style>th {background: ghostwhite} td {text-align: center}</style></head><body>\n", CREATE, APPEND);
            Files.writeString(htmlFile, "<table border='1'>\n", APPEND);
            final StringBuilder line1 = new StringBuilder("<tr><th></th><th>RECEIVING</th><th>Encryption</th>");
            final StringBuilder line2 = new StringBuilder("<tr><th>INITIATING</th><th></th><th>Certificate</th>");
            final StringBuilder line3 = new StringBuilder("<tr><th>Encryption</th><th>Certificate</th><th>Dialback</th>");
            for (final ServerSettings remote : remoteServerSettings) {
                line1.append("<th>").append(remote.encryptionPolicy).append("</th>");
                line2.append("<th>").append(remote.certificateState).append("</th>");
                line3.append("<th>").append(remote.dialbackSupported).append("</th>");
            }
            Files.writeString(htmlFile, line1.append("</tr>\n").toString(), APPEND);
            Files.writeString(htmlFile, line2.append("</tr>\n").toString(), APPEND);
            Files.writeString(htmlFile, line3.append("</tr>\n").toString(), APPEND);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void add(ExpectedOutcome expectedOutcome, ServerSettings localServerSettings, ServerSettings remoteServerSettings)
    {
        String matrixLine = "";
        if (localServerSettings != previousLocalServerSettings) {
            // New line!
            if (previousLocalServerSettings != null) {
                // End previous line.
                matrixLine += "</tr>\n";
            }
            previousLocalServerSettings = localServerSettings;
            matrixLine += "<tr><th>"+localServerSettings.encryptionPolicy+"</th><th>"+localServerSettings.certificateState+"</th><th>"+localServerSettings.dialbackSupported+"</th>";

        }
        entryCount++;
        matrixLine += "<td>"+entryCount+". "+expectedOutcome.getConnectionState().getShortCode()+"</td>";
        try {
            Files.writeString(htmlFile, matrixLine, APPEND);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void complete()
    {
        try {
            Files.writeString(htmlFile, "</tr>\n", APPEND);
            Files.writeString(htmlFile, "</table></body></html>", APPEND);
            System.out.println("Written HTML file to " + htmlFile);
            Desktop.getDesktop().browse(htmlFile.toUri());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
