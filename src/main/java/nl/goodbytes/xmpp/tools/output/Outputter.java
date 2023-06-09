package nl.goodbytes.xmpp.tools.output;

import nl.goodbytes.xmpp.tools.ExpectedOutcome;
import nl.goodbytes.xmpp.tools.ServerSettings;

import java.io.IOException;
import java.util.Collection;

public interface Outputter
{
    void init(final Collection<ServerSettings> localServerSettings, final Collection<ServerSettings> remoteServerSettings);

    void add(final ExpectedOutcome expectedOutcome, final ServerSettings localServerSettings, final ServerSettings remoteServerSettings);

    void complete();
}
