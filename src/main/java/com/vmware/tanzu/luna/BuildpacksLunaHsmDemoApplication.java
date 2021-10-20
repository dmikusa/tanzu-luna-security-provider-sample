package com.vmware.tanzu.luna;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;

import javax.crypto.Cipher;

import com.safenetinc.luna.LunaSlotManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;

@SpringBootApplication
public class BuildpacksLunaHsmDemoApplication {
	private static final Logger log = LoggerFactory.getLogger(BuildpacksLunaHsmDemoApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(BuildpacksLunaHsmDemoApplication.class, args);
	}

	@Bean
	Cipher decryptionCipher(KeyPair keyPair) throws GeneralSecurityException {
		Cipher cipher = Cipher.getInstance("RSA/NONE/NoPadding", "LunaProvider");
		cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
		return cipher;
	}

	@Bean
	Cipher encryptionCipher(KeyPair keyPair) throws GeneralSecurityException {
		Cipher cipher = Cipher.getInstance("RSA/NONE/NoPadding", "LunaProvider");
		cipher.init(Cipher.ENCRYPT_MODE, keyPair.getPublic());
		return cipher;
	}

	@Bean
	@DependsOn("slotManager")
	KeyPair keyPair() throws GeneralSecurityException {
		KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA", "LunaProvider");
		keyPairGenerator.initialize(1024);
		return keyPairGenerator.generateKeyPair();
	}

	@Bean
	Signature signingSignature(KeyPair keyPair) throws GeneralSecurityException {
		Signature signature = Signature.getInstance("RSA");
		signature.initSign(keyPair.getPrivate());
		return signature;
	}

	@Bean(destroyMethod = "logout")
	LunaSlotManager slotManager(@Value("${k8s.bindings.luna-security-provider.crypto_service_id}") String tokenLabel,
			@Value("${k8s.bindings.luna-security-provider.crypto_service_password}") String password) {
		LunaSlotManager slotManager = LunaSlotManager.getInstance();

		int[] activeSlots = slotManager.getSlotList();
		log.info("Number of slots:" + activeSlots.length);

		for (int slot : activeSlots) {
			try {
				if (slotManager.isTokenPresent(slot)) {
					tokenLabel = slotManager.getTokenLabel(slot);
					log.info("Slot: " + slot + " token label: " + tokenLabel);
				}
			} catch (Exception ex) {
				log.warn("Could not access slot", ex);
			}
		}

		slotManager.login(tokenLabel, password);
		return slotManager;
	}

	@Bean
	Signature verificationSignature(KeyPair keyPair) throws GeneralSecurityException {
		Signature signature = Signature.getInstance("RSA");
		signature.initVerify(keyPair.getPublic());
		return signature;
	}

}
