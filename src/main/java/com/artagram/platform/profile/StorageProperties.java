package com.artagram.platform.profile;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {

    private String root;
    private String publicBaseUrl;
    private String signingSecret;
    private int signedUrlTtlMinutes = 60;
    private Azure azure = new Azure();

    public String getRoot() {
        return root;
    }

    public void setRoot(String root) {
        this.root = root;
    }

    public String getPublicBaseUrl() {
        return publicBaseUrl;
    }

    public void setPublicBaseUrl(String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl;
    }

    public String getSigningSecret() {
        return signingSecret;
    }

    public void setSigningSecret(String signingSecret) {
        this.signingSecret = signingSecret;
    }

    public int getSignedUrlTtlMinutes() {
        return signedUrlTtlMinutes;
    }

    public void setSignedUrlTtlMinutes(int signedUrlTtlMinutes) {
        this.signedUrlTtlMinutes = signedUrlTtlMinutes;
    }

    public Azure getAzure() {
        return azure;
    }

    public void setAzure(Azure azure) {
        this.azure = azure;
    }

    /** Solo se usa si {@code app.storage.provider=blob} (ver {@link AzureBlobStorageProvider}). */
    public static class Azure {
        private String connectionString;
        private String container = "artagram-uploads";

        public String getConnectionString() {
            return connectionString;
        }

        public void setConnectionString(String connectionString) {
            this.connectionString = connectionString;
        }

        public String getContainer() {
            return container;
        }

        public void setContainer(String container) {
            this.container = container;
        }
    }
}
