package org.orderofthebee.tools.webhook.hub;

import java.util.Locale;

/**
 * @author Axel Faust
 */
public class SecretConfig
{

    /**
     *
     * @author Axel Faust
     */
    public static enum Mode
    {
        PLAIN_HEADER,
        PLAIN_PARAMETER,
        BODY_DIGEST_HEADER,
        BODY_DIGEST_WITH_SHARED_SECRET_HEADER;
    }

    private String name;

    private String secretValue;

    private Mode mode = Mode.PLAIN_PARAMETER;

    private String digestAlgorithm;

    /**
     * @return the name
     */
    public String getName()
    {
        return this.name;
    }

    /**
     * @param name
     *            the name to set
     */
    public void setName(final String name)
    {
        this.name = name;
    }

    /**
     * @return the secretValue
     */
    public String getSecretValue()
    {
        return this.secretValue;
    }

    /**
     * @param secretValue
     *            the secretValue to set
     */
    public void setSecretValue(final String secretValue)
    {
        this.secretValue = secretValue;
    }

    /**
     * @return the mode
     */
    public Mode getMode()
    {
        return this.mode;
    }

    /**
     * @param mode
     *            the mode to set
     */
    public void setMode(final String mode)
    {
        // TODO add support for camelCase, i.e. plainHeader in addition to plain_header
        this.mode = Mode.valueOf(mode.toUpperCase(Locale.ENGLISH));
    }

    /**
     * @return the digestAlgorithm
     */
    public String getDigestAlgorithm()
    {
        return this.digestAlgorithm;
    }

    /**
     * @param digestAlgorithm
     *            the digestAlgorithm to set
     */
    public void setDigestAlgorithm(final String digestAlgorithm)
    {
        this.digestAlgorithm = digestAlgorithm;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        final StringBuilder builder = new StringBuilder();
        builder.append("SecretConfig [");
        builder.append("name=");
        builder.append(this.name);
        builder.append(", ");
        builder.append("value=");
        builder.append(this.secretValue);
        builder.append(", ");
        builder.append("mode=");
        builder.append(this.mode);
        builder.append(", ");
        builder.append("digestAlgorithm=");
        builder.append(this.digestAlgorithm);
        builder.append("]");
        return builder.toString();
    }

}
