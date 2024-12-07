package com.hrs.model; // Package for model classes

import javafx.beans.property.SimpleStringProperty; // Importing SimpleStringProperty for JavaFX property support
import javafx.beans.property.StringProperty; // Importing StringProperty for JavaFX property support

/**
 * Model class for scan results displayed in the table.
 */
public class ScanResult {
    // Property for the URL
    private final StringProperty url;
    // Property for the HTTP method
    private final StringProperty method;
    // Property for the Transfer-Encoding header
    private final StringProperty teHeader;
    // Property for the payload
    private final StringProperty payload;

    /**
     * Constructor to initialize a ScanResult.
     * @param url The URL of the scan result
     * @param method The HTTP method used
     * @param teHeader The Transfer-Encoding header
     * @param payload The payload of the scan result
     */
    public ScanResult(String url, String method, String teHeader, String payload) {
        // Initializes the URL property
        this.url = new SimpleStringProperty(url); 
        // Initializes the method property
        this.method = new SimpleStringProperty(method); 
        // Initializes the Transfer-Encoding header property
        this.teHeader = new SimpleStringProperty(teHeader); 
        // Initializes the payload property
        this.payload = new SimpleStringProperty(payload); 
    }

    /**
     * Gets the URL.
     * @return The URL
     */
    public String getUrl() { 
        // Returns the URL value
        return url.get(); 
    }
    /**
     * Gets the URL property.
     * @return The URL property
     */
    public StringProperty urlProperty() { 
        // Returns the URL property for JavaFX binding
        return url; 
    }
    /**
     * Sets the URL.
     * @param url The URL to set
     */
    public void setUrl(String url) { 
        // Sets the URL value
        this.url.set(url); 
    }

    /**
     * Gets the HTTP method.
     * @return The HTTP method
     */
    public String getMethod() { 
        // Returns the HTTP method value
        return method.get(); 
    }
    /**
     * Gets the method property.
     * @return The method property
     */
    public StringProperty methodProperty() { 
        // Returns the method property for JavaFX binding
        return method; 
    }
    /**
     * Sets the HTTP method.
     * @param method The method to set
     */
    public void setMethod(String method) { 
        // Sets the HTTP method value
        this.method.set(method); 
    }

    /**
     * Gets the Transfer-Encoding header.
     * @return The Transfer-Encoding header
     */
    public String getTeHeader() { 
        // Returns the Transfer-Encoding header value
        return teHeader.get(); 
    }
    /**
     * Gets the Transfer-Encoding header property.
     * @return The Transfer-Encoding header property
     */
    public StringProperty teHeaderProperty() { 
        // Returns the Transfer-Encoding header property for JavaFX binding
        return teHeader; 
    }
    /**
     * Sets the Transfer-Encoding header.
     * @param teHeader The header to set
     */
    public void setTeHeader(String teHeader) { 
        // Sets the Transfer-Encoding header value
        this.teHeader.set(teHeader); 
    }

    /**
     * Gets the payload.
     * @return The payload
     */
    public String getPayload() { 
        // Returns the payload value
        return payload.get(); 
    }
    /**
     * Gets the payload property.
     * @return The payload property
     */
    public StringProperty payloadProperty() { 
        // Returns the payload property for JavaFX binding
        return payload; 
    }
    /**
     * Sets the payload.
     * @param payload The payload to set
     */
    public void setPayload(String payload) { 
        // Sets the payload value
        this.payload.set(payload); 
    }
}
