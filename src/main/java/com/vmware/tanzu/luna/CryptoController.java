package com.vmware.tanzu.luna;

import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.Signature;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

import javax.crypto.Cipher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

@RestController
public class CryptoController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final Base64.Decoder decoder = Base64.getDecoder();

    private final Cipher decryptionCipher;

    private final Base64.Encoder encoder = Base64.getEncoder();

    private final Cipher encryptionCipher;

    private final Signature signingSignature;

    private final Signature verificationSignature;

    @Autowired
    CryptoController(@Qualifier("decryptionCipher") Cipher decryptionCipher,
            @Qualifier("encryptionCipher") Cipher encryptionCipher,
            @Qualifier("signingSignature") Signature signingSignature,
            @Qualifier("verificationSignature") Signature verificationSignature) {
        this.decryptionCipher = decryptionCipher;
        this.encryptionCipher = encryptionCipher;
        this.signingSignature = signingSignature;
        this.verificationSignature = verificationSignature;
    }

    @PostMapping(value = "/decrypt", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    Mono<Map<String, String>> decrypt(@RequestBody Map<String, String> payload) throws GeneralSecurityException {
        String cipherText = Optional.of(payload.get("cipher-text"))
                .orElseThrow(() -> new IllegalArgumentException("Payload must contain 'cipher-text'"));

        this.logger.info("Decrypting Cipher Text '{}'", cipherText);

        this.decryptionCipher.update(this.decoder.decode(cipherText));
        String message = new String(this.decryptionCipher.doFinal(), Charset.defaultCharset()).trim();

        return Mono.just(Map.of("cipher-text", cipherText, "message", message));
    }

    @PostMapping(value = "/encrypt", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    Mono<Map<String, String>> encrypt(@RequestBody Map<String, String> payload) throws GeneralSecurityException {
        String message = Optional.of(payload.get("message"))
                .orElseThrow(() -> new IllegalArgumentException("Payload must contain 'message'"));

        this.logger.info("Encrypting Message '{}'", message);

        this.encryptionCipher.update(message.getBytes(Charset.defaultCharset()));
        String cipherText = this.encoder.encodeToString(this.encryptionCipher.doFinal());

        return Mono.just(Map.of("message", message, "cipher-text", cipherText));
    }

    @PostMapping(value = "/sign", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    Mono<Map<String, String>> sign(@RequestBody Map<String, String> payload) throws GeneralSecurityException {
        String message = Optional.of(payload.get("message"))
                .orElseThrow(() -> new IllegalArgumentException("Payload must contain 'message'"));

        this.logger.info("Signing Message '{}'", message);

        this.signingSignature.update(message.getBytes(Charset.defaultCharset()));
        String signature = this.encoder.encodeToString(this.signingSignature.sign());

        return Mono.just(Map.of("message", message, "signature", signature));
    }

    @PostMapping(value = "/verify", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    Mono<Map<String, Object>> verify(@RequestBody Map<String, String> payload) throws GeneralSecurityException {
        String message = Optional.of(payload.get("message"))
                .orElseThrow(() -> new IllegalArgumentException("Payload must contain 'message'"));
        String signature = Optional.of(payload.get("signature"))
                .orElseThrow(() -> new IllegalArgumentException("Payload must contain 'signature'"));

        this.logger.info("Verifying Message '{}' and Signature '{}'", message, signature);

        this.verificationSignature.update(message.getBytes(Charset.defaultCharset()));
        boolean verified = this.verificationSignature.verify(this.decoder.decode(signature));

        return Mono.just(Map.of("message", message, "signature", signature, "verified", verified));
    }
}
