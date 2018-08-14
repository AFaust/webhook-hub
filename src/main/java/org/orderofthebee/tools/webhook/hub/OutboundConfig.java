package org.orderofthebee.tools.webhook.hub;

/**
 * @author Axel Faust
 */
public class OutboundConfig
{

    private String requestMethod;

    private String requestUrl;

    private String requestContentType;

    private SecretConfig secret;

    /**
     * @return the requestMethod
     */
    public String getRequestMethod()
    {
        return this.requestMethod;
    }

    /**
     * @param requestMethod
     *            the requestMethod to set
     */
    public void setRequestMethod(final String requestMethod)
    {
        this.requestMethod = requestMethod;
    }

    /**
     * @return the requestUrl
     */
    public String getRequestUrl()
    {
        return this.requestUrl;
    }

    /**
     * @param requestUrl
     *            the requestUrl to set
     */
    public void setRequestUrl(final String requestUrl)
    {
        this.requestUrl = requestUrl;
    }

    /**
     * @return the requestContentType
     */
    public String getRequestContentType()
    {
        return this.requestContentType;
    }

    /**
     * @param requestContentType
     *            the requestContentType to set
     */
    public void setRequestContentType(final String requestContentType)
    {
        this.requestContentType = requestContentType;
    }

    /**
     * @return the secret
     */
    public SecretConfig getSecret()
    {
        return this.secret;
    }

    /**
     * @param secret
     *            the secret to set
     */
    public void setSecret(final SecretConfig secret)
    {
        this.secret = secret;
    }

    public boolean isMandatoryConfigComplete()
    {
        boolean complete = true;
        complete = complete && this.requestMethod != null && !this.requestMethod.trim().isEmpty();
        complete = complete && this.requestUrl != null && !this.requestUrl.trim().isEmpty();
        return complete;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        final StringBuilder builder = new StringBuilder();
        builder.append("OutboundConfig [");
        builder.append("requestMethod=");
        builder.append(this.requestMethod);
        builder.append(", ");
        builder.append("requestUrl=");
        builder.append(this.requestUrl);

        if (this.requestContentType != null)
        {
            builder.append(", ");
            builder.append("requestContentType=");
            builder.append(this.requestContentType);
        }
        if (this.secret != null)
        {
            builder.append(", ");
            builder.append("secret=");
            builder.append(this.secret);
        }
        builder.append("]");
        return builder.toString();
    }

}
