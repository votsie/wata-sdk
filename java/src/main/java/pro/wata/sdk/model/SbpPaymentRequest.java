package pro.wata.sdk.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Оплата СБП ({@code POST /api/h2h/payments/sbp}). Валюты нет — только рубли.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class SbpPaymentRequest {

    private final double amount;
    private final String ip;
    private final String returnUrl;
    private final DeviceData deviceData;
    private final String orderId;
    private final String description;

    private SbpPaymentRequest(Builder b) {
        this.amount = b.amount;
        this.ip = b.ip;
        this.returnUrl = b.returnUrl;
        this.deviceData = b.deviceData;
        this.orderId = b.orderId;
        this.description = b.description;
    }

    public double getAmount() {
        return amount;
    }

    public String getIp() {
        return ip;
    }

    public String getReturnUrl() {
        return returnUrl;
    }

    public DeviceData getDeviceData() {
        return deviceData;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getDescription() {
        return description;
    }

    public static Builder builder(double amount, String ip, String returnUrl, DeviceData deviceData) {
        return new Builder(amount, ip, returnUrl, deviceData);
    }

    public static final class Builder {
        private final double amount;
        private final String ip;
        private final String returnUrl;
        private final DeviceData deviceData;
        private String orderId;
        private String description;

        public Builder(double amount, String ip, String returnUrl, DeviceData deviceData) {
            this.amount = amount;
            this.ip = ip;
            this.returnUrl = returnUrl;
            this.deviceData = deviceData;
        }

        public Builder orderId(String orderId) {
            this.orderId = orderId;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public SbpPaymentRequest build() {
            return new SbpPaymentRequest(this);
        }
    }
}
