package org.orderofthebee.tools.webhook.hub;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * @author Axel Faust
 */
public class BufferedttpServletRequestWrapper extends HttpServletRequestWrapper
{

    private static final String SYSTEM_KEY_TEMP_DIR = "java.io.tmpdir";

    private static final int MEMORY_BUFFER_LIMIT = 10 * 1024 * 1024; // 10 MiB

    private static File getTempDir()
    {
        final String tempDirPath = System.getProperty(SYSTEM_KEY_TEMP_DIR);
        final File tempDir = new File(tempDirPath);
        if (!tempDir.exists())
        {
            if (!tempDir.mkdirs())
            {
                throw new IllegalStateException("Failed to create specified temporary file directory");
            }
        }
        return tempDir;
    }

    private transient File tempFile;

    private transient RandomAccessFile raTempFile;

    private transient ByteBuffer inputBuffer;

    public BufferedttpServletRequestWrapper(final HttpServletRequest request)
    {
        super(request);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public ServletInputStream getInputStream() throws IOException
    {
        if (this.inputBuffer == null)
        {
            long contentLength = this.getContentLengthLong();

            if (contentLength == -1 || contentLength > MEMORY_BUFFER_LIMIT)
            {
                // determine proper content length while copying
                contentLength = 0;
                this.tempFile = new File(getTempDir(), UUID.randomUUID().toString());
                try (FileOutputStream fos = new FileOutputStream(this.tempFile))
                {
                    try (ServletInputStream rawSIS = super.getInputStream())
                    {
                        final byte[] buf = new byte[MEMORY_BUFFER_LIMIT];
                        int bytesRead;
                        while ((bytesRead = rawSIS.read(buf)) != -1)
                        {
                            fos.write(buf, 0, bytesRead);
                            contentLength += bytesRead;
                        }
                    }
                }

                this.raTempFile = new RandomAccessFile(this.tempFile, "r");
                this.inputBuffer = this.raTempFile.getChannel().map(MapMode.READ_ONLY, 0, contentLength);
            }
            else
            {
                this.inputBuffer = ByteBuffer.allocateDirect((int) contentLength);
                try (ServletInputStream rawSIS = super.getInputStream())
                {
                    final byte[] buf = new byte[MEMORY_BUFFER_LIMIT];
                    int bytesRead;
                    while ((bytesRead = rawSIS.read(buf)) != -1)
                    {
                        this.inputBuffer.put(buf, 0, bytesRead);
                    }
                }
                this.inputBuffer.position(0);
            }
        }

        return new ByteBufferBackedServletInputStream(this.inputBuffer.duplicate());
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public BufferedReader getReader() throws IOException
    {
        String characterEncoding = this.getCharacterEncoding();
        if (characterEncoding == null)
        {
            characterEncoding = StandardCharsets.ISO_8859_1.name();
        }
        return new BufferedReader(new InputStreamReader(this.getInputStream(), characterEncoding));

    }

    public void cleanUp() throws IOException
    {
        this.inputBuffer = null;
        if (this.raTempFile != null)
        {
            this.raTempFile.close();
        }
        if (this.tempFile != null)
        {
            if (this.tempFile.delete())
            {
                this.tempFile.deleteOnExit();
            }
        }
    }
}
