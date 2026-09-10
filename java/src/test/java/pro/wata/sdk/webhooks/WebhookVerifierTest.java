package pro.wata.sdk.webhooks;

import org.junit.jupiter.api.Test;
import pro.wata.sdk.WataEnvironment;
import pro.wata.sdk.model.WebhookEvent;
import pro.wata.sdk.support.MockHttpServer;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Обязательное покрытие по п.9.3 SPEC.md: проверка подписи вебхука, успех и
 * подделка. Алгоритм — SHA512withRSA, как того требует п.8 SPEC.md.
 */
class WebhookVerifierTest {

    private static final String RAW_BODY = "{\"transactionType\":\"CardCrypto\",\"kind\":\"Payment\","
            + "\"id\":\"evt_1\",\"transactionId\":\"tx_1\",\"transactionStatus\":\"Paid\","
            + "\"amount\":1500.0,\"currency\":\"RUB\",\"orderId\":\"order-1\",\"commission\":15.0}";

    @Test
    void verifiesGenuineSignatureAgainstAnExplicitKey() throws Exception {
        KeyPair keyPair = generateRsaKeyPair();
        String signature = sign(keyPair.getPrivate(), RAW_BODY);
        String pem = toPem(keyPair.getPublic().getEncoded());

        WebhookVerifier verifier = WebhookVerifier.forEnvironment(WataEnvironment.PRODUCTION);

        assertTrue(verifier.verify(RAW_BODY, signature, pem), "Genuine signature must verify");
        assertTrue(verifier.verify(RAW_BODY.getBytes(StandardCharsets.UTF_8), signature, pem),
                "Byte[] overload must accept the same raw body");
    }

    @Test
    void rejectsATamperedBody() throws Exception {
        KeyPair keyPair = generateRsaKeyPair();
        String signature = sign(keyPair.getPrivate(), RAW_BODY);
        String pem = toPem(keyPair.getPublic().getEncoded());

        WebhookVerifier verifier = WebhookVerifier.forEnvironment(WataEnvironment.PRODUCTION);

        String tamperedBody = RAW_BODY.replace("1500.0", "9999.0");
        assertFalse(verifier.verify(tamperedBody, signature, pem), "A tampered body must fail verification");
    }

    @Test
    void rejectsASignatureFromADifferentKey() throws Exception {
        KeyPair genuineKeyPair = generateRsaKeyPair();
        KeyPair attackerKeyPair = generateRsaKeyPair();

        String signatureFromAttacker = sign(attackerKeyPair.getPrivate(), RAW_BODY);
        String genuinePem = toPem(genuineKeyPair.getPublic().getEncoded());

        WebhookVerifier verifier = WebhookVerifier.forEnvironment(WataEnvironment.PRODUCTION);

        assertFalse(verifier.verify(RAW_BODY, signatureFromAttacker, genuinePem),
                "A signature made with a different private key must fail verification");
    }

    @Test
    void fetchesAndCachesThePublicKeyPerEnvironment() throws Exception {
        KeyPair keyPair = generateRsaKeyPair();
        String signature = sign(keyPair.getPrivate(), RAW_BODY);
        String pem = toPem(keyPair.getPublic().getEncoded());

        try (MockHttpServer server = new MockHttpServer()
                .json("/api/h2h/public-key", 200, "{\"value\":" + toJsonString(pem) + "}")
                .start()) {

            // Package-private constructor: same package as this test, points the
            // verifier's public-key fetch at the mock server instead of the real host.
            WebhookVerifier verifier = new WebhookVerifier(server.baseUrl(), java.time.Duration.ofSeconds(5));

            assertTrue(verifier.verify(RAW_BODY, signature));
            assertTrue(verifier.verify(RAW_BODY, signature), "Second call should be served from cache");
            assertEquals(1, server.requests().size(), "The public key must be fetched once and cached");

            // The public-key endpoint must be called without an Authorization header.
            assertFalse(server.requests().get(0).headers().containsKey("Authorization"));
        }
    }

    @Test
    void parsesAWebhookEventWithoutRequiringSignatureVerification() {
        WebhookVerifier verifier = WebhookVerifier.forEnvironment(WataEnvironment.PRODUCTION);
        WebhookEvent event = verifier.parse(RAW_BODY);

        assertEquals("evt_1", event.id());
        assertEquals("tx_1", event.transactionId());
        assertEquals(15.0, event.commission());
    }

    private static KeyPair generateRsaKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private static String sign(PrivateKey privateKey, String body) throws Exception {
        Signature signature = Signature.getInstance("SHA512withRSA");
        signature.initSign(privateKey);
        signature.update(body.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signature.sign());
    }

    private static String toPem(byte[] der) {
        String base64 = Base64.getEncoder().encodeToString(der);
        StringBuilder sb = new StringBuilder("-----BEGIN PUBLIC KEY-----\n");
        for (int i = 0; i < base64.length(); i += 64) {
            sb.append(base64, i, Math.min(i + 64, base64.length())).append('\n');
        }
        sb.append("-----END PUBLIC KEY-----\n");
        return sb.toString();
    }

    private static String toJsonString(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }
}
