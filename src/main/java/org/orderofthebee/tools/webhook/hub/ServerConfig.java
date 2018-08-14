package org.orderofthebee.tools.webhook.hub;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Axel Faust
 */
public class ServerConfig
{

    private List<WebhookConfig> webhooks;

    /**
     * @return the webhooks
     */
    public List<WebhookConfig> getWebhooks()
    {
        return this.webhooks != null ? new ArrayList<>(this.webhooks) : null;
    }

    /**
     * @param webhooks
     *            the webhooks to set
     */
    public void setWebhooks(final List<WebhookConfig> webhooks)
    {
        this.webhooks = webhooks != null ? new ArrayList<>(webhooks) : null;
    }

}
