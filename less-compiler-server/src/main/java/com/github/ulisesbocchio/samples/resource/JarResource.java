package com.github.ulisesbocchio.samples.resource;

import org.springframework.core.io.Resource;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

/**
 * @author Ulises Bocchio, Sergio.U.Bocchio@Disney.com (BOCCS002)
 */
public class JarResource implements Resource {

    private final Resource delegate;
    private final String extractPath;
    private File tempFile;

    public JarResource(Resource resource) {
        this(resource, null);

    }

    public JarResource(Resource resource, String extractPath) {
        this.delegate = resource;
        this.extractPath = extractPath;
    }

    @Override
    public boolean exists() {
        return delegate.exists();
    }

    @Override
    public boolean isReadable() {
        return delegate.isReadable();
    }

    @Override
    public boolean isOpen() {
        return delegate.isOpen();
    }

    @Override
    public URL getURL() throws IOException {
        return delegate.getURL();
    }

    @Override
    public URI getURI() throws IOException {
        return delegate.getURI();
    }

    @Override
    public File getFile() throws IOException {
        if(ResourceUtils.isJarURL(getURL())) {
            if(tempFile == null) {
                tempFile = JarUtils.getFile(delegate, extractPath);
            }
            return new File(tempFile.toURI());
        }
        return delegate.getFile();
    }

    @Override
    public long contentLength() throws IOException {
        return getFile().length();
    }

    @Override
    public long lastModified() throws IOException {
        return getFile().lastModified();
    }

    @Override
    public Resource createRelative(String relativePath) throws IOException {
        return new JarResource(delegate.createRelative(relativePath), extractPath);
    }

    @Override
    public String getFilename() {
        return delegate.getFilename();
    }

    @Override
    public String getDescription() {
        return delegate.getDescription();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return delegate.getInputStream();
    }
}
