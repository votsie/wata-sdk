package pro.wata.sdk;

import org.junit.jupiter.api.Test;
import pro.wata.sdk.errors.WataConfigException;
import pro.wata.sdk.support.MockHttpServer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Обязательное покрытие по п.9.3 SPEC.md: обращение к продукту без токена
 * бросает {@link WataConfigException} до какого-либо сетевого вызова.
 */
class WataClientConfigTest {

    @Test
    void steamWithoutTokenFailsBeforeAnyNetworkCall() {
        try (MockHttpServer server = new MockHttpServer().start()) {
            WataClient client = WataClient.builder()
                    .acquiringToken("acquiring-token") // some other product IS configured
                    .build();

            WataConfigException exception = assertThrows(WataConfigException.class, client::steam);
            assertTrue(exception.getMessage().contains("steam"), "Error message must name the missing token");
            assertEquals(0, server.requests().size(), "No network call should happen for a missing-token product");
        }
    }

    @Test
    void starsWithoutTokenFailsBeforeAnyNetworkCall() {
        WataClient client = WataClient.builder().build();
        WataConfigException exception = assertThrows(WataConfigException.class, client::stars);
        assertTrue(exception.getMessage().contains("stars"));
    }

    @Test
    void acquiringWithoutTokenFailsBeforeAnyNetworkCall() {
        WataClient client = WataClient.builder().steamToken("steam-token").build();
        WataConfigException exception = assertThrows(WataConfigException.class, client::acquiring);
        assertTrue(exception.getMessage().contains("acquiring"));
    }

    @Test
    void digitalGoodsSandboxIsRejectedExplicitlyInsteadOfFallingBackToProduction() {
        WataClient client = WataClient.builder()
                .environment(WataEnvironment.SANDBOX)
                .steamToken("steam-token")
                .build();

        WataConfigException exception = assertThrows(WataConfigException.class, client::steam);
        assertTrue(exception.getMessage().toLowerCase().contains("sandbox"));
    }
}
