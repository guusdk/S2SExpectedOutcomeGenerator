A generator of expected outcomes of an XMPP server-to-server unidirectional connection from one server (the Initiating Entity) to another server (the Receiving Entity).

This code defines a server configuration as a collection of configuration options. The code generates scenarios based on  each possible server configuration on both ends of the to-be-established connection. For each scenario, the 'expected outcome' is calculated.

It is explicitly *not advisable* to use the output of this project as an authoritative definition! This project was created to help develop understanding of relevant XMPP specifications, generate data for discussing the desirability of certain outcomes, and to 
facilitate the creation of (unit) tests in a specific XMPP project. Many of these outcomes are open to discussion.

### Server Settings
Server Settings, as represented by the `ServerSettings` class, include (but may in future versions not be limited to):
- `EncryptionPolicy` defining if a server requires, allows or prohibits encryption
- `CertificateState` defining if the server offers a TLS certificate, and if that's valid (isn't expired, isn't self-signed, uses a root CA that's recognized by the peer, uses a correct identity, etc).
- `DialbackSupported` defining if the server allows the Dialback protocol (which is an authentication protocol) to be used.

It is assumed that a server _always_ allows authentication to occur using SASL EXTERNAL, when possible.

### Expected Outcomes
Four distinct outcomes are defined, as represented by the `ExpectedOutcome` class:
- `NO_CONNECTION` - Connection cannot be established. In the current implementation, this includes scenarios in which connections could not be authenticated (some form of authentication is assumed to be mandatory).
- `NON_ENCRYPTED_WITH_DIALBACK_AUTH` - Connection without encryption, Initiating Entity is authenticated by the Receiving Entity using the Dialback protocol.
- `ENCRYPTED_WITH_DIALBACK_AUTH` - Connection that is encrypted, Initiating Entity is authenticated by the Receiving Entity using the Dialback protocol.
- `ENCRYPTED_WITH_SASLEXTERNAL_AUTH` - Connection that is encrypted, Initiating Entity is authenticated by the Receiving Entity using the SASL EXTERNAL mechanism.

# Compiling and running the code
This implementation requires Java 11 or later to compile and run. Apache Maven is used for the project framework (although no dependencies are defined. It should be possible to compile this using the basic `javac` compiler).

To compile the project:
```bash
mvn clean package
```

This will generate an executable JAR file in the `target` directory.

Executing this JAR file (eg: `java -jar target/S2SExpectedOutcomeGenerator-1.0-SNAPSHOT.jar`) will cause all scenarios and their calculated expected outcome to be printed to the standard output stream.
