package org.orderofthebee.tools.webhook.hub;

/**
 * @author Axel Faust
 */
public class SecretConfig
{

    private String name;

    private String value;

    private boolean useHeader;

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
     * @return the value
     */
    public String getValue()
    {
        return this.value;
    }

    /**
     * @param value
     *            the value to set
     */
    public void setValue(final String value)
    {
        this.value = value;
    }

    /**
     * @return the useHeader
     */
    public boolean isUseHeader()
    {
        return this.useHeader;
    }

    /**
     * @param useHeader
     *            the useHeader to set
     */
    public void setUseHeader(final boolean useHeader)
    {
        this.useHeader = useHeader;
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
        builder.append(this.value);
        builder.append(", ");
        builder.append("useHeader=");
        builder.append(this.useHeader);
        builder.append("]");
        return builder.toString();
    }

}
