package edu.nwu.sakaistudentlink.services;

import java.io.Serializable;

/**
 *
 * @author Jaco Gillman
 */
public class IntegrationError implements Serializable {

	private static final long serialVersionUID = -5978279698369273070L;
	private String errorCategory;
    private String errorKey;
    private String errorMessage;
    private String[] parameters;

    /**
     * @return the errorCategory
     */
    public String getErrorCategory() {
        return errorCategory;
    }

    /**
     * @param errorCategory the errorCategory to set
     */
    public void setErrorCategory(String errorCategory) {
        this.errorCategory = errorCategory;
    }

    /**
     * @return the errorKey
     */
    public String getErrorKey() {
        return errorKey;
    }

    /**
     * @param errorKey the errorKey to set
     */
    public void setErrorKey(String errorKey) {
        this.errorKey = errorKey;
    }

    /**
     * @return the errorMessage
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * @param errorMessage the errorMessage to set
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * @return the parameters
     */
    public String[] getParameters() {
        return parameters;
    }

    /**
     * @param parameters the parameters to set
     */
    public void setParameters(String[] parameters) {
        this.parameters = parameters;
    }
}
