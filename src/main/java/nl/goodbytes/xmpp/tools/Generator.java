/*
 * Copyright (C) 2023 Guus der Kinderen. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.goodbytes.xmpp.tools;

import nl.goodbytes.xmpp.tools.output.HtmlTableOutputter;
import nl.goodbytes.xmpp.tools.output.Outputter;
import nl.goodbytes.xmpp.tools.output.StdOutOutPutter;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import static nl.goodbytes.xmpp.tools.ExpectedOutcome.ConnectionState.*;

public class Generator
{
    public static void main(String[] args)
    {
        final Instant start = Instant.now();
        final Set<ServerSettings> localServerSettings = new HashSet<>();
        final Set<ServerSettings> remoteServerSettings = new HashSet<>();

        for (final ServerSettings.CertificateState certificateState : ServerSettings.CertificateState.values()) {
            for (final boolean dialbackSupported : Set.of(true, false)) {
                for (final ServerSettings.EncryptionPolicy tlsPolicy : ServerSettings.EncryptionPolicy.values()) {
                    final ServerSettings serverSettings = new ServerSettings(certificateState, dialbackSupported, tlsPolicy);
                    localServerSettings.add(serverSettings);
                    remoteServerSettings.add(serverSettings);
                }
            }
        }

        final Set<Outputter> outputters = Set.of(new StdOutOutPutter(), new HtmlTableOutputter());
        outputters.forEach(outputter -> outputter.init(localServerSettings, remoteServerSettings));
        for (final ServerSettings local : localServerSettings) {
            for (final ServerSettings remote : remoteServerSettings) {
                final ExpectedOutcome expectedOutcome = generateExpectedOutcome(local, remote);
                outputters.forEach(outputter -> outputter.add(expectedOutcome, local, remote));
            }
        }
        outputters.forEach(Outputter::complete);
        System.out.println();
        System.out.println("Finished execution in " + Duration.between(start, Instant.now()));
    }

    public static ExpectedOutcome generateExpectedOutcome(final ServerSettings initiatingServer, final ServerSettings receivingServer) {
        final ExpectedOutcome expectedOutcome = new ExpectedOutcome();

        switch (initiatingServer.encryptionPolicy) {
            case DISABLED: // <-- Initiating server's encryption policy.
                switch (receivingServer.encryptionPolicy) {
                    case DISABLED: // Intended fall-through: if one peer disables TLS, it won't be used in any circumstances.
                    case OPTIONAL:
                        // The certificate status of both peers is irrelevant, as TLS won't happen.
                        if (initiatingServer.dialbackSupported && receivingServer.dialbackSupported) {
                            expectedOutcome.set(NON_ENCRYPTED_WITH_DIALBACK_AUTH, "although TLS is not available (so it cannot be used for encryption or authentication), Dialback is available, which allows the Initiating Entity to be authenticated by the Receiving Entity.");
                        } else {
                            expectedOutcome.set(NO_CONNECTION, "TLS and Dialback aren't available, making it impossible for the Initiating Entity to be authenticated by the Receiving Entity.");
                        }
                        break;
                    case REQUIRED:
                        expectedOutcome.set(NO_CONNECTION, "one peer requires encryption while the other disables encryption. This cannot work.");
                        break;
                }
                break;

            case OPTIONAL: // <-- Initiating server's encryption policy.
                switch (receivingServer.encryptionPolicy) {
                    case DISABLED:
                        // The certificate status of both peers is irrelevant, as TLS won't happen.
                        if (initiatingServer.dialbackSupported && receivingServer.dialbackSupported) {
                            expectedOutcome.set(NON_ENCRYPTED_WITH_DIALBACK_AUTH, "TLS is not available, so it cannot be used for encryption or authentication. Dialback is available, which allows the Initiating Entity to be authenticated by the Receiving Entity.");
                        } else {
                            expectedOutcome.set(NO_CONNECTION, "TLS and Dialback aren't available, making it impossible for the Initiating Entity to be authenticated by the Receiving Entity.");
                        }
                        break;
                    case OPTIONAL:
                        switch (receivingServer.certificateState) {
                            case MISSING:
                                if (initiatingServer.dialbackSupported && receivingServer.dialbackSupported) {
                                    // TODO: should we take into account a manual configuration of an ANON cypher suite, so that encryption-without-authentication can occur via TLS, followed by a Dialback-based authentication?
                                    expectedOutcome.set(NON_ENCRYPTED_WITH_DIALBACK_AUTH, "Receiving Entity does not provides a TLS certificate. As ANON cypher suites are expected to be unavailable, Initiating Entity cannot negotiate TLS, so that cannot be used for encryption or authentication. Dialback is available, so authentication can occur.");
                                } else {
                                    expectedOutcome.set(NO_CONNECTION, "Receiving Entity does not provides a TLS certificate, Initiating Entity cannot negotiate TLS. With TLS and Dialback unavailable, authentication cannot occur (even if usage of an ANON cypher suite would make TLS-for-encryption possible)");
                                }
                                break;
                            case INVALID:
                                // TODO Is this possibly an allowable OF-2591 edge-case? Worry about DOWNGRADE ATTACK VECTOR?
                                // possibly: expectedOutcome.set(NON_ENCRYPTED_WITH_DIALBACK), // Encryption is configured to be OPTIONAL, so maybe allowable?
                                expectedOutcome.set(NO_CONNECTION, "the Initiating Entity will fail to negotiate TLS, as the Receiving Entity's certificate is not valid.");
                                break;
                            case VALID:
                                switch (initiatingServer.certificateState) {
                                    case MISSING:
                                        if (initiatingServer.dialbackSupported && receivingServer.dialbackSupported) {
                                            expectedOutcome.set(ENCRYPTED_WITH_DIALBACK_AUTH, "Receiving Entity provides a valid TLS certificate allowing Initiating Entity to be able to establish TLS for encryption. Initiating Entity but does not provide a certificate itself, so SASL EXTERNAL is not available for Receiving Entity to authenticate Initiating Entity, but Dialback is available for that purpose.");
                                        } else {
                                            expectedOutcome.set(NO_CONNECTION, "Receiving Entity provides a valid TLS certificate allowing Initiating Entity to be able to establish TLS for encryption. Initiating Entity but does not provide a certificate itself, so SASL EXTERNAL is not available for Receiving Entity to authenticate Initiating Entity. As Dialback is also not available, authentication cannot occur.");
                                        }
                                        break;
                                    case INVALID:
                                        if (initiatingServer.dialbackSupported && receivingServer.dialbackSupported) {
                                            // TODO: should the Initiating Entity be allowed to authenticate using Dialback over an encrypted TLS connection - or should TLS fail hard when invalid certs are used (the client _could_ opt to not send those...)? Is this possibly an allowable OF-2591 edge-case, or is this a DOWNGRADE ATTACK vector?
                                            expectedOutcome.set(NO_CONNECTION, "Initiating Entity provides an invalid TLS certificate, which should cause Receiving Entity to abort TLS.");
                                            // possibly: expectedOutcome.set(NON_ENCRYPTED_WITH_DIALBACK), // fail TLS but allow unencrypted. Perhaps better to fail connecting, to not give false sense of encryption security? DOWNGRADE ATTACK VECTOR? Encryption is configured to be OPTIONAL, so maybe allowable?
                                            // possibly: expectedOutcome.set(ENCRYPTED_WITH_DIALBACK), // do not fail TLS with invalid client cert, as it's usable for encryption even if it's not used for authentication. DOWNGRADE ATTACK VECTOR?
                                        } else {
                                            expectedOutcome.set(NO_CONNECTION, "Initiating Entity provides an invalid TLS certificate, which should cause Receiving Entity to abort TLS. Even if Receiving Entity would negotiate TLS for encryption, it can't use Initiating Entity's invalid cert for Authentication. As Dialback is also not available, authentication cannot occur.");
                                        }
                                        break;
                                    case VALID:
                                        // valid certs all around. Dialback is not needed.
                                        expectedOutcome.set(ENCRYPTED_WITH_SASLEXTERNAL_AUTH, "TLS is usable for both encryption and authentication.");
                                        break;
                                }
                                break;
                        }
                        break;
                    case REQUIRED:
                        switch (receivingServer.certificateState) {
                            case MISSING:
                                expectedOutcome.set(NO_CONNECTION, "Receiving Entity requires encryption, but it does not provide a TLS certificate. As ANON cypher suites are expected to be unavailable, the Initiating Entity cannot negotiate TLS and therefor the required encrypted connection cannot be established.");
                                // TODO: should we take into account a manual configuration of an ANON cypher suite, so that encryption-without-authentication can occur via TLS, followed by a Dialback-based authentication?
                                break;
                            case INVALID:
                                expectedOutcome.set(NO_CONNECTION, "Receiving Entity requires encryption, but it does provides an invalid TLS certificate. The Initiating Entity cannot negotiate TLS and therefor the required encrypted connection cannot be established.");
                                // TODO: should we allow TLS to be used anyway, so that encryption-without-authentication can occur via TLS, followed by a Dialback-based authentication? THIS INTRODUCES DOWNGRADE ATTACK VECTOR.
                                break;
                            case VALID:
                                // TODO - AG We need to check the initiating servers certificate
                                switch (initiatingServer.certificateState) {
                                    case MISSING:
                                        if (initiatingServer.dialbackSupported && receivingServer.dialbackSupported) {
                                            expectedOutcome.set(ENCRYPTED_WITH_DIALBACK_AUTH, "Receiving Entity provides a valid TLS certificate allowing Initiating Entity to be able to establish TLS for encryption. Initiating Entity but does not provide a certificate itself, so SASL EXTERNAL is not available for Receiving Entity to authenticate Initiating Entity, but Dialback is available for that purpose.");
                                        } else {
                                            expectedOutcome.set(NO_CONNECTION, "Receiving Entity provides a valid TLS certificate allowing Initiating Entity to be able to establish TLS for encryption. Initiating Entity but does not provide a certificate itself, so SASL EXTERNAL is not available for Receiving Entity to authenticate Initiating Entity. As Dialback is also not available, authentication cannot occur.");
                                        }
                                        break;
                                    case INVALID:
                                        if (initiatingServer.dialbackSupported && receivingServer.dialbackSupported) {
                                            // TODO: should the Initiating Entity be allowed to authenticate using Dialback over an encrypted TLS connection - or should TLS fail hard when invalid certs are used (the client _could_ opt to not send those...)? Is this possibly an allowable OF-2591 edge-case, or is this a DOWNGRADE ATTACK vector?
                                            expectedOutcome.set(NO_CONNECTION, "Initiating Entity provides an invalid TLS certificate, which should cause Receiving Entity to abort TLS.");
                                            // possibly: expectedOutcome.set(NON_ENCRYPTED_WITH_DIALBACK), // fail TLS but allow unencrypted. Perhaps better to fail connecting, to not give false sense of encryption security? DOWNGRADE ATTACK VECTOR? Encryption is configured to be OPTIONAL, so maybe allowable?
                                            // possibly: expectedOutcome.set(ENCRYPTED_WITH_DIALBACK), // do not fail TLS with invalid client cert, as it's usable for encryption even if it's not used for authentication. DOWNGRADE ATTACK VECTOR?
                                        } else {
                                            expectedOutcome.set(NO_CONNECTION, "Initiating Entity provides an invalid TLS certificate, which should cause Receiving Entity to abort TLS. Even if Receiving Entity would negotiate TLS for encryption, it can't use Initiating Entity's invalid cert for Authentication. As Dialback is also not available, authentication cannot occur.");
                                        }
                                        break;
                                    case VALID:
                                        // valid certs all around. Dialback is not needed.
                                        expectedOutcome.set(ENCRYPTED_WITH_SASLEXTERNAL_AUTH, "TLS is usable for both encryption and authentication.");
                                        break;
                                }
                                break;
                        }
                        break;
                }
                break;

            case REQUIRED: // <-- Initiating server's encryption policy.
                switch (receivingServer.encryptionPolicy) {
                    case DISABLED:
                        expectedOutcome.set(NO_CONNECTION, "one peer requires encryption, the other disables encryption. This cannot work.");
                        break;
                    case OPTIONAL:
                        switch (receivingServer.certificateState) {
                            case MISSING:
                                expectedOutcome.set(NO_CONNECTION, "Receiving Entity does not provide a TLS certificate. As ANON cypher suites are expected to be unavailable, the Initiating Entity cannot negotiate TLS and therefor the required encrypted connection cannot be established.");
                                // TODO: should we take into account a manual configuration of an ANON cypher suite, so that encryption-without-authentication can occur via TLS, followed by a Dialback-based authentication?
                                break;
                            case INVALID:
                                expectedOutcome.set(NO_CONNECTION, "Receiving Entity provides an invalid TLS certificate. The Initiating Entity cannot negotiate TLS and therefor the required encrypted connection cannot be established.");
                                // TODO: should we allow TLS to be used anyway, so that encryption-without-authentication can occur via TLS, followed by a Dialback-based authentication? THIS INTRODUCES DOWNGRADE ATTACK VECTOR.
                                break;
                            case VALID:
                                switch (initiatingServer.certificateState) {
                                    case MISSING:
                                        if (initiatingServer.dialbackSupported && receivingServer.dialbackSupported) {
                                            expectedOutcome.set(ENCRYPTED_WITH_DIALBACK_AUTH, "Receiving Entity provides a valid TLS certificate allowing Initiating Entity to be able to establish TLS for encryption. Initiating Entity but does not provide a certificate itself, so SASL EXTERNAL is not available for Receiving Entity to authenticate Initiating Entity, but Dialback is available for that purpose.");
                                        } else {
                                            expectedOutcome.set(NO_CONNECTION, "Receiving Entity provides a valid TLS certificate allowing Initiating Entity to be able to establish TLS for encryption. Initiating Entity but does not provide a certificate itself, so SASL EXTERNAL is not available for Receiving Entity to authenticate Initiating Entity. As Dialback is also not available, authentication cannot occur.");
                                        }
                                        break;
                                    case INVALID:
                                        if (initiatingServer.dialbackSupported && receivingServer.dialbackSupported) {
                                            // TODO: should the Initiating Entity be allowed to authenticate using Dialback over an encrypted TLS connection - or should TLS fail hard when invalid certs are used (the client _could_ opt to not send those...)? Is this possibly an allowable OF-2591 edge-case, or is this a DOWNGRADE ATTACK vector?
                                            expectedOutcome.set(NO_CONNECTION, "Initiating Entity provides an invalid TLS certificate, which should cause Receiving Entity to abort TLS.");
                                            // AG: this is correct I think. The language in 13.7.2 states in block capitals if a certificate is present it MUST attempt validation; if the validation fails, the connection terminates. See RFC2119 to confirm this is an absolute requirement.
                                            // possibly: expectedOutcome.set(NON_ENCRYPTED_WITH_DIALBACK), // fail TLS but allow unencrypted. Perhaps better to fail connecting, to not give false sense of encryption security? DOWNGRADE ATTACK VECTOR? Encryption is configured to be OPTIONAL, so maybe allowable?
                                            // possibly: expectedOutcome.set(ENCRYPTED_WITH_DIALBACK), // do not fail TLS with invalid client cert, as it's usable for encryption even if it's not used for authentication. DOWNGRADE ATTACK VECTOR?
                                        } else {
                                            expectedOutcome.set(NO_CONNECTION, "Initiating Entity provides an invalid TLS certificate, which should cause Receiving Entity to abort TLS. Even if Receiving Entity would negotiate TLS for encryption, it can't use Initiating Entity's invalid cert for Authentication. As Dialback is also not available, authentication cannot occur.");
                                        }
                                        break;
                                    case VALID:
                                        // valid certs all around. Dialback is not needed.
                                        expectedOutcome.set(ENCRYPTED_WITH_SASLEXTERNAL_AUTH, "TLS is usable for both encryption and authentication.");
                                        break;
                                }
                                break;
                        }
                        break;
                    case REQUIRED:
                        switch (receivingServer.certificateState) {
                            case MISSING:
                                expectedOutcome.set(NO_CONNECTION, "Receiving Entity does not provide a TLS certificate. As ANON cypher suites are expected to be unavailable, the Initiating Entity cannot negotiate TLS and therefor the required encrypted connection cannot be established.");
                                // TODO: should we take into account a manual configuration of an ANON cypher suite, so that encryption-without-authentication can occur via TLS, followed by a Dialback-based authentication?
                                break;
                            case INVALID:
                                expectedOutcome.set(NO_CONNECTION, "Receiving Entity provides an invalid TLS certificate. The Initiating Entity cannot negotiate TLS and therefor the required encrypted connection cannot be established.");
                                // TODO: should we allow TLS to be used anyway, so that encryption-without-authentication can occur via TLS, followed by a Dialback-based authentication? THIS INTRODUCES DOWNGRADE ATTACK VECTOR.
                                // AG: This is the expected behaviour, OF-2555 suggests this is not the current behaviour of Openfire as "if validation fails but Dialback is available", the connection is made.
                                break;
                            case VALID:
                                switch (initiatingServer.certificateState) {
                                    case MISSING:
                                        if (initiatingServer.dialbackSupported && receivingServer.dialbackSupported) {
                                            expectedOutcome.set(ENCRYPTED_WITH_DIALBACK_AUTH, "Initiating Entity can negotiate encryption, but does not provide a certificate. SASL EXTERNAL cannot be used, but Dialback is available, so authentication can occur.");
                                        } else {
                                            expectedOutcome.set(NO_CONNECTION, "Initiating Entity can negotiate encryption, but does not provide a certificate. As Dialback is not available, authentication cannot occur. Connection cannot be established.");
                                        }
                                        break;
                                    case INVALID:
                                        // TODO: should the Receiving Entity be allowed to authenticate using Dialback? Is this possibly an allowable OF-2591 edge-case?
                                        if (initiatingServer.dialbackSupported && receivingServer.dialbackSupported) {
                                            // TODO: should the Initiating Entity be allowed to authenticate using Dialback over an encrypted TLS connection - or should TLS fail hard when invalid certs are used (the client _could_ opt to not send those...)? Is this possibly an allowable OF-2591 edge-case, or is this a DOWNGRADE ATTACK vector?
                                            expectedOutcome.set(NO_CONNECTION, "Initiating Entity provides an invalid TLS certificate, which should cause Receiving Entity to abort TLS.");
                                            // AG: this is correct I think. The language in 13.7.2 states in block capitals if a certificate is present it MUST attempt validation; if the validation fails, the connection terminates. See RFC2119 to confirm this is an absolute requirement.
                                            // possibly: expectedOutcome.set(NON_ENCRYPTED_WITH_DIALBACK), // fail TLS but allow unencrypted. Perhaps better to fail connecting, to not give false sense of encryption security? DOWNGRADE ATTACK VECTOR? Encryption is configured to be OPTIONAL, so maybe allowable?
                                            // possibly: expectedOutcome.set(ENCRYPTED_WITH_DIALBACK), // do not fail TLS with invalid client cert, as it's usable for encryption even if it's not used for authentication. DOWNGRADE ATTACK VECTOR?
                                        } else {
                                            expectedOutcome.set(NO_CONNECTION, "Initiating Entity provides an invalid TLS certificate, which should cause Receiving Entity to abort TLS. Even if Receiving Entity would negotiate TLS for encryption, it can't use Initiating Entity's invalid cert for Authentication. As Dialback is also not available, authentication cannot occur.");
                                        }
                                        break;
                                    case VALID:
                                        expectedOutcome.set(ENCRYPTED_WITH_SASLEXTERNAL_AUTH, "Initiating Entity can establish encryption and authenticate using TLS.");
                                        break;
                                }
                                break;
                        }
                        break;
                }
                break;
        }

        return expectedOutcome;
    }
}
