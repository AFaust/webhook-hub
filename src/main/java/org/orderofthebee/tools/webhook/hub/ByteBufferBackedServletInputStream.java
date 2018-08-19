package org.orderofthebee.tools.webhook.hub;

import java.io.IOException;
import java.nio.ByteBuffer;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;

/**
 * @author Axel Faust
 */
public class ByteBufferBackedServletInputStream extends ServletInputStream
{

    private final ByteBuffer buffer;

    public ByteBufferBackedServletInputStream(final ByteBuffer buffer)
    {
        this.buffer = buffer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isFinished()
    {
        return !this.buffer.hasRemaining();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isReady()
    {
        return this.buffer.hasRemaining();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setReadListener(final ReadListener readListener)
    {
        // NO-OP (not supported)
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int read() throws IOException
    {
        return this.buffer.hasRemaining() ? this.buffer.get() : -1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException
    {
        final int remaining = this.buffer.remaining();
        final int bytesToRead = remaining != 0 ? Math.min(len, remaining) : -1;
        if (bytesToRead > 0)
        {
            this.buffer.get(b, off, bytesToRead);
        }
        return bytesToRead;
    }

}
