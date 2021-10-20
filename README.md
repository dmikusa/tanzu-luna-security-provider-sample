# A REST-ful sample application that exercises the Luna HSM.

This application exposes four cryptography REST endpoints that expect to recieve a JSON payload. Each endpoint returns values that can be used as arguments to the other endpoints.

* `POST /encrypt` - `message`, a message to encrypt.
* `POST /decrypt` - `cipher-text`, a base64 encoded encrypted message to decrypt.
* `POST /sign` - `message`, a message to sign.
* `POST /verify` - `message` and `signature`, a signature to verify against a message.

In addition to these, two information endpoints are exposed.

* `GET /key-pair` - Returns the base64 encoded private and public keys.
* `GET /security-providers` - Lists all of the installed Java security providers.

## Building

### Prerequisites

The required Luna JAR files are not in Maven central, so you need to add them to your local machine. Download the latest Luna Client, it was 10.3.0, at the time of writing and take these steps.

1. Update `pom.xml` with the version you downloaded.
2. Execute this command: `./mvnw install:install-file -Dfile=/path/to/the/LunaProvider.jar -DgroupId=luna-jsp -DartifactId=luna-provider -Dversion=<version> -Dpackaging=jar -DgeneratePom=true`. 

Make sure to add the actual path to the LunaProvider.jar and your version number to the command. You may obtain this from the Luna Minimal client, under the `jsp/` directory.

### Update your Binding Info

There is an example binding in the `bindings/luna-security-provider` directory. Populate this with the required info.

1. `client-certificate.pem` and `client-private-key.pem` are the client cert and key which is connecting to the Luna HSM.
2. `server-certificates.pem` is zero or more certificates which may be required to trust the Luna HSM's certificates.
3. `template-metadata.toml` is the base config which the buildpack will use to expand into `Chrystoki.conf`. See the buildpack documentation for details.
4. `crypto_service_id` and `crypto_service_password` are the values used by the application to authenticate with the `LunaSlotManager`. They are passed to the `slotManager.login(tokenLabel, password)` method. The id/label is likely your partition name and the password is likely your crypto officer's password, but these may vary depending on your setup. Contact your Luna HSM administrator if in doubt about what values to use.

### Steps to Build

1. Run `./mvnw package`. This will build the JAR file.
2. Execute `pack build -b tanzu-buildpacks/java --volume $PWD/bindings:/platform/bindings -p target/luna-hsm-0.0.1-SNAPSHOT.jar -B paketobuildpacks/builder:full`.
3. Run the app with `docker run -it -p 8080:8080 -v $PWD/bindings:/bindings -e SERVICE_BINDING_ROOT=/bindings apps/luna-hsm`.

The `paketobuildpacks/builder:full` builder is presently required because of library requirements for the Luna Client. It's not possible to run on tiny or base.

## Usage

Once you've completed the steps to build & run, you may now access the application. The endpoints are documented above, but here are some examples.

```
# Encrypt a value using the LunaProvider
> curl -H 'Content-type: application/json' -X POST http://localhost:8080/encrypt -d '{"message": "hello world!"}'
{"message":"hello world!","cipher-text":"axOe8ivD8dQ4LxTnmMuvlItY6M1E32hSqUydI/jOYW19BsAO8ltYKkcZeo5Atk2W5d33dM/Qtzb3Zwf0pY/WuyPbad7RhFRdy3UIViR8nOV+tZdsZ6zFRkyyKngyMEtlbMfCgV3k8PghR3wRMJNXxY9Dhk9L+3sMtXxjQCiH0sg="}⏎

# Decrypt an encrypted value using the LunaProvider
> curl -H 'Content-type: application/json' -X POST http://localhost:8080/decrypt -d '{"cipher-text": "axOe8ivD8dQ4LxTnmMuvlItY6M1E32hSqUydI/jOYW19BsAO8ltYKkcZeo5Atk2W5d33dM/Qtzb3Zwf0pY/WuyPbad7RhFRdy3UIViR8nOV+tZdsZ6zFRkyyKngyMEtlbMfCgV3k8PghR3wRMJNXxY9Dhk9L+3sMtXxjQCiH0sg="}'
{"cipher-text":"axOe8ivD8dQ4LxTnmMuvlItY6M1E32hSqUydI/jOYW19BsAO8ltYKkcZeo5Atk2W5d33dM/Qtzb3Zwf0pY/WuyPbad7RhFRdy3UIViR8nOV+tZdsZ6zFRkyyKngyMEtlbMfCgV3k8PghR3wRMJNXxY9Dhk9L+3sMtXxjQCiH0sg=","message":"hello world!"}⏎

# Sign a value using the LunaProvider
> curl -H 'Content-type: application/json' -X POST http://localhost:8080/sign -d '{"message": "hello world!"}'
{"message":"hello world!","signature":"LOpnJV0cWSsh7PQoex5Gse5Np5kFjRcvUbxO9+Zqtp9pC1FTGHDPWJlx7WN33RLs2atJj+xF+Je21uu6OvNC3FQXzyF33k12vxl3Ut0G2KiUcHDB1Pt4zfqNalxi8QY0F0j6G/wwJarLYEplEroDlAWwa9evYzZzDrKi3Neec/8="}⏎

# Verify a signed value using the LunaProvider
> curl -H 'Content-type: application/json' -X POST http://localhost:8080/verify -d '{"message":"hello world!","signature":"LOpnJV0cWSsh7PQoex5Gse5Np5kFjRcvUbxO9+Zqtp9pC1FTGHDPWJlx7WN33RLs2atJj+xF+Je21uu6OvNC3FQXzyF33k12vxl3Ut0G2KiUcHDB1Pt4zfqNalxi8QY0F0j6G/wwJarLYEplEroDlAWwa9evYzZzDrKi3Neec/8="}'
{"signature":"LOpnJV0cWSsh7PQoex5Gse5Np5kFjRcvUbxO9+Zqtp9pC1FTGHDPWJlx7WN33RLs2atJj+xF+Je21uu6OvNC3FQXzyF33k12vxl3Ut0G2KiUcHDB1Pt4zfqNalxi8QY0F0j6G/wwJarLYEplEroDlAWwa9evYzZzDrKi3Neec/8=","verified":true,"message":"hello world!"}⏎

# List the active key/pair
> curl http://localhost:8080/key-pair | jq .
{
  "private": "<private>",
  "public": "<public>"
}

# List Security Providers - note LunaProvider is last in the list
> curl http://localhost:8080/security-providers | jq .
[
  {
    "info": "SUN (DSA key/parameter generation; DSA signing; SHA-1, MD5 digests; SecureRandom; X.509 certificates; PKCS12, JKS & DKS keystores; PKIX CertPathValidator; PKIX CertPathBuilder; LDAP, Collection CertStores, JavaPolicy Policy; JavaLoginConfig Configuration)",
    "version": "11",
    "name": "SUN"
  },
  {
    "info": "Sun RSA signature provider",
    "version": "11",
    "name": "SunRsaSign"
  },
  {
    "info": "Sun Elliptic Curve provider (EC, ECDSA, ECDH)",
    "version": "11",
    "name": "SunEC"
  },
  {
    "info": "Sun JSSE provider(PKCS12, SunX509/PKIX key/trust factories, SSLv3/TLSv1/TLSv1.1/TLSv1.2/TLSv1.3/DTLSv1.0/DTLSv1.2)",
    "version": "11",
    "name": "SunJSSE"
  },
  {
    "info": "SunJCE Provider (implements RSA, DES, Triple DES, AES, Blowfish, ARCFOUR, RC2, PBE, Diffie-Hellman, HMAC, ChaCha20)",
    "version": "11",
    "name": "SunJCE"
  },
  {
    "info": "Sun (Kerberos v5, SPNEGO)",
    "version": "11",
    "name": "SunJGSS"
  },
  {
    "info": "Sun SASL provider(implements client mechanisms for: DIGEST-MD5, EXTERNAL, PLAIN, CRAM-MD5, NTLM; server mechanisms for: DIGEST-MD5, CRAM-MD5, NTLM)",
    "version": "11",
    "name": "SunSASL"
  },
  {
    "info": "XMLDSig (DOM XMLSignatureFactory; DOM KeyInfoFactory; C14N 1.0, C14N 1.1, Exclusive C14N, Base64, Enveloped, XPath, XPath2, XSLT TransformServices)",
    "version": "11",
    "name": "XMLDSig"
  },
  {
    "info": "Sun PC/SC provider",
    "version": "11",
    "name": "SunPCSC"
  },
  {
    "info": "JdkLDAP Provider (implements LDAP CertStore)",
    "version": "11",
    "name": "JdkLDAP"
  },
  {
    "info": "JDK SASL provider(implements client and server mechanisms for GSSAPI)",
    "version": "11",
    "name": "JdkSASL"
  },
  {
    "info": "Unconfigured and unusable PKCS11 provider",
    "version": "11",
    "name": "SunPKCS11"
  },
  {
    "info": "Java Security Provider for SafeNet Luna hardware",
    "version": "7.3",
    "name": "LunaProvider"
  }
]
```