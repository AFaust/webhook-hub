package org.orderofthebee.tools.webhook.hub;

/**
 * @author Axel Faust
 */
public class InboundConfig
{

    private String requestMethod;

    private String urlPattern;

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
     * @return the urlPattern
     */
    public String getUrlPattern()
    {
        return this.urlPattern;
    }

    /**
     * @param urlPattern
     *            the urlPattern to set
     */
    public void setUrlPattern(final String urlPattern)
    {
        this.urlPattern = urlPattern;
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
        complete = complete && this.urlPattern != null && !this.urlPattern.trim().isEmpty();
        return complete;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        final StringBuilder builder = new StringBuilder();
        builder.append("InbounConfig [");
        if (this.requestMethod != null)
        {
            builder.append("requestMethod=");
            builder.append(this.requestMethod);
            builder.append(", ");
        }

        builder.append("urlPattern=");
        builder.append(this.urlPattern);

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
