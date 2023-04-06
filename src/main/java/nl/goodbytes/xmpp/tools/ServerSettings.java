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

/**
 * Representation of a particular state of server configuration.
 */
public class ServerSettings
{
    public enum EncryptionPolicy
    {
        /**
         * Encryption cannot be used.
         */
        DISABLED,

        /**
         * Encryption may be used, but is not mandatory.
         */
        OPTIONAL,

        /**
         * Encryption must be used.
         */
        REQUIRED
    }

    public enum CertificateState
    {
        /**
         * Server does not offer a TLS certificate.
         */
        MISSING,

        /**
         * Server offers a TLS certificate that is somehow not valid (it expired, is self-signed, uses a root CA that's not recognized by the peer, uses an incorrect identity, etc).
         */
        INVALID,

        /**
         * Server offers a TLS certificate that is valid.
         */
        VALID
    }

//        public enum TlsMutualAuthenticationPolicy {
//            /** Local Server will not attempt to identify peer, using its TLS certificate. */
//            DISABLED,
//
//            /** Local Server will attempt to identify peer, but only if it provides its TLS certificate. */
//            WANTED,
//
//            /** Local Server will fail to establish a connection if it cannot verify peer's TLS certificate. */
//            NEEDED
//        }

    /**
     * Describes the certificate that's offered by this entity.
     */
    public final CertificateState certificateState;

    /**
     * Defines if this entity will support the Dialback authentication mechanism.
     */
    public final boolean dialbackSupported;

    /**
     * Defines if this entity requires/disables or can use TLS for encryption (this does not mandate TLS-based authentication).
     */
    public final EncryptionPolicy encryptionPolicy;

//        /**
//         * Defines if this entity will attempt/require/ignore to validate the peer's certificate
//         */
//        public final TlsMutualAuthenticationPolicy tlsMutualAuthenticationPolicy;

    public ServerSettings(final CertificateState certificateState, final boolean dialbackSupported, final EncryptionPolicy encryptionPolicy)
    {
        this.certificateState = certificateState;
        this.dialbackSupported = dialbackSupported;
        this.encryptionPolicy = encryptionPolicy;
    }

    @Override
    public String toString()
    {
        return toString(5);
    }

    public String toString(int length)
    {
        if (length > 0) {
            return "encryption=" + encryptionPolicy.toString().substring(0, length) + ", certificate=" + certificateState.toString().substring(0, length) + ", dialback=" + (dialbackSupported ? "SUPPORTED" : "DISABLED").substring(0, length);
        } else {
            return "encryption=" + encryptionPolicy.toString() + ", certificate=" + certificateState.toString() + ", dialback=" + (dialbackSupported ? "SUPPORTED" : "DISABLED");
        }
    }
}
