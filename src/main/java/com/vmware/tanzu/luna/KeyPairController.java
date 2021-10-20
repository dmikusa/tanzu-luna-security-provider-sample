package com.vmware.tanzu.luna;

import java.security.KeyPair;
import java.util.Base64;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

@RestController
public class KeyPairController {
    private final KeyPair keyPair;

    @Autowired
    KeyPairController(KeyPair keyPair) {
        this.keyPair = keyPair;
    }

    @RequestMapping(method = RequestMethod.GET, value = "/key-pair")
    Mono<Map<String, String>> keyPair() {
        String privateKey = Base64.getEncoder().encodeToString(this.keyPair.getPrivate().getEncoded());
        String publicKey = Base64.getEncoder().encodeToString(this.keyPair.getPublic().getEncoded());

        return Mono.just(Map.of("private", privateKey, "public", publicKey));
    }
}
