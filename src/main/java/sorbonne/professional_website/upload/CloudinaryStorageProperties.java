package sorbonne.professional_website.upload;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("storage.cloudinary")
public class CloudinaryStorageProperties {

    /**
     * Cloudinary cloud name.
     */
    private String cloudName;

    /**
     * Cloudinary API key.
     */
    private String apiKey;

    /**
     * Cloudinary API secret. Never expose it in the frontend.
     */
    private String apiSecret;

    /**
     * Folder used to isolate portfolio uploads inside Cloudinary.
     */
    private String folder = "portfolio";

    public String getCloudName() {
        return cloudName;
    }

    public void setCloudName(String cloudName) {
        this.cloudName = cloudName;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiSecret() {
        return apiSecret;
    }

    public void setApiSecret(String apiSecret) {
        this.apiSecret = apiSecret;
    }

    public String getFolder() {
        return folder;
    }

    public void setFolder(String folder) {
        this.folder = folder;
    }
}
