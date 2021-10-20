package com.vmware.tanzu.luna;

import java.security.Provider;
import java.security.Security;
import java.util.Arrays;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;

@RestController
final class SecurityProvidersController {

    @GetMapping(value = "/security-providers")
    Flux<ProviderProjection> securityProviders() {
        return Flux.fromStream(Arrays.stream(Security.getProviders()).map(ProviderProjection::new));
    }

    static final class ProviderProjection {

        private final Provider provider;

        private ProviderProjection(Provider provider) {
            this.provider = provider;
        }

        public String getInfo() {
            return this.provider.getInfo();
        }

        public String getName() {
            return this.provider.getName();
        }

        public String getVersion() {
            return this.provider.getVersionStr();
        }

    }
}