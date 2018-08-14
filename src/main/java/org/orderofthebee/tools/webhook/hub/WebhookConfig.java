package org.orderofthebee.tools.webhook.hub;

/**
 * @author Axel Faust
 */
public class WebhookConfig
{

    private InboundConfig inbound;

    private String handlerScript;

    private OutboundConfig outbound;

    /**
     * @return the inbound
     */
    public InboundConfig getInbound()
    {
        return this.inbound;
    }

    /**
     * @param inbound
     *            the inbound to set
     */
    public void setInbound(final InboundConfig inbound)
    {
        this.inbound = inbound;
    }

    /**
     * @return the mapperScript
     */
    public String getMapperScript()
    {
        return this.handlerScript;
    }

    /**
     * @param handlerScript
     *            the handlerScript to set
     */
    public void setHandlerScript(final String handlerScript)
    {
        this.handlerScript = handlerScript;
    }

    /**
     * @return the outbound
     */
    public OutboundConfig getOutbound()
    {
        return this.outbound;
    }

    /**
     * @param outbound
     *            the outbound to set
     */
    public void setOutbound(final OutboundConfig outbound)
    {
        this.outbound = outbound;
    }

    public boolean isMandatoryConfigComplete()
    {
        boolean complete = true;
        complete = complete && this.inbound != null && this.inbound.isMandatoryConfigComplete();
        complete = complete && this.outbound != null && this.outbound.isMandatoryConfigComplete();
        complete = complete && this.handlerScript != null && !this.handlerScript.trim().isEmpty();
        return complete;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        final StringBuilder builder = new StringBuilder();
        builder.append("WebhookConfig [");
        builder.append("inbound=");
        builder.append(this.inbound);
        builder.append(", ");
        builder.append("handlerScript=");
        builder.append(this.handlerScript);
        builder.append(", ");
        builder.append("outbound=");
        builder.append(this.outbound);
        builder.append("]");
        return builder.toString();
    }

}
