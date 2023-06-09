package nl.goodbytes.xmpp.tools.output;

import nl.goodbytes.xmpp.tools.ExpectedOutcome;
import nl.goodbytes.xmpp.tools.ServerSettings;

import java.util.Collection;

public class StdOutOutPutter implements Outputter
{
    private long totalOutcomes = 0;
    private long inconclusiveOutcomes = 0;
    private long noConnectOutcomes = 0;
    private long noEncryptionOutcomes = 0;
    private long encryptionOutcomes = 0;
    private long dialbackOutcomes = 0;
    private long saslExternalOutcomes = 0;

    @Override
    public void init(final Collection<ServerSettings> localServerSettings, final Collection<ServerSettings> remoteServerSettings)
    {
        System.out.println("Generated " + localServerSettings.size() + " distinct server configurations:");
        for (final ServerSettings serverSettings : localServerSettings) {
            System.out.println("- " + serverSettings.toString(0));
        }

        System.out.println();
        System.out.println("These are all scenarios in which a an outgoing, unidirectional server-to-server connection is to be established, given all possible combinations for Initiating and Receiving Entity server configuration.");
        System.out.println("It is assumed that the Receiving Entity MUST be able to authenticate the Initiating Entity. If this is not possible, the outcome is 'NO_CONNECTION' (there is no valid connection configuration that would lead to an unauthenticated connection.");
        System.out.println();
    }

    @Override
    public void add(ExpectedOutcome expectedOutcome, ServerSettings localServerSettings, ServerSettings remoteServerSettings)
    {
        final String explanation;
        if (expectedOutcome.isInconclusive()) {
            explanation = "has an inconclusive outcome";
        } else {
            explanation = "should result in " + expectedOutcome.getConnectionState() + " because " + String.join(" ", expectedOutcome.getRationales());
        }
        System.out.println( "S2S from Initiating Entity [" + localServerSettings.toString() + "] to Receiving Entity [" + remoteServerSettings.toString() + "] " + explanation );

        totalOutcomes++;
        if (expectedOutcome.isInconclusive()) {
            inconclusiveOutcomes++;
        }
        switch (expectedOutcome.getConnectionState()) {
            case NO_CONNECTION:
                noConnectOutcomes++;
                break;
            case NON_ENCRYPTED_WITH_DIALBACK_AUTH:
                noEncryptionOutcomes++;
                dialbackOutcomes++;
                break;
            case ENCRYPTED_WITH_DIALBACK_AUTH:
                encryptionOutcomes++;
                dialbackOutcomes++;
                break;
            case ENCRYPTED_WITH_SASLEXTERNAL_AUTH:
                encryptionOutcomes++;
                saslExternalOutcomes++;
                break;
        }
    }

    @Override
    public void complete()
    {
        System.out.println("Found " + totalOutcomes + " possible outcomes of which " + inconclusiveOutcomes + " (~" + Math.round(inconclusiveOutcomes * 100.0 / totalOutcomes) +"%) were inconclusive.");
        System.out.println(noConnectOutcomes + " (~" + Math.round(noConnectOutcomes * 100.0 / totalOutcomes) + "%) of the outcomes define a 'no connection possible' scenario.");
        System.out.println(noEncryptionOutcomes + " (~" + Math.round(noEncryptionOutcomes * 100.0 / totalOutcomes) + "%) of the outcomes define a successful connection that does not use encryption.");
        System.out.println(encryptionOutcomes + " (~" + Math.round(encryptionOutcomes * 100.0 / totalOutcomes) + "%) of the outcomes define a successful connection that do use encryption.");
        System.out.println(dialbackOutcomes + " (~" + Math.round(dialbackOutcomes * 100.0 / totalOutcomes) + "%) of the outcomes define a successful connection in which Dialback is used to authorize the Initiating Entity.");
        System.out.println(saslExternalOutcomes + " (~" + Math.round(saslExternalOutcomes * 100.0 / totalOutcomes) + "%) of the outcomes define a successful connection in which SASL EXTERNAL is used to authorize the Initiating Entity.");
    }
}
