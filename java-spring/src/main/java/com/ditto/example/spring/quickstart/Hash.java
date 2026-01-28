package com.ditto.example.spring.quickstart;

import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class Hash {

	private static final int ITERATIONS = 100_000;
	private static final int KEY_LENGTH = 256;
	private static final String APP_SECRET = "zaza"; // FIXME:

	public static String hash(String password, String user_id) throws Exception {
		String saltString = user_id + APP_SECRET;
		byte[] salt = saltString.getBytes();

		PBEKeySpec spec = new PBEKeySpec(
			password.toCharArray(),
			salt,
			ITERATIONS,
			KEY_LENGTH
		);

		SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
		byte[] hash = skf.generateSecret(spec).getEncoded();

		return Base64.getEncoder().encodeToString(hash);
	}
}
