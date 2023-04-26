/*
 * Copyright (c) 2023 Mahmoud Bahaa and others
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mahmoudbahaa.gcsemulator.store;

import com.google.api.services.storage.model.Bucket;
import com.google.api.services.storage.model.StorageObject;
import com.google.cloud.storage.StorageException;
import io.github.mahmoudbahaa.gcsemulator.StorageExceptionFactory;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;
import org.jboss.resteasy.spi.config.SizeUnit;

public abstract class Store {
    protected static final int BUFFER_SIZE = (int) SizeUnit.MEGABYTE.toBytes(16);
    protected static final Map<String, StorageObject> uploads = new HashMap<>();
    protected static final Pattern validNames = Pattern.compile( "^[0-9a-z.\\-_]{1,222}$");

    public abstract Iterator<Bucket> listBuckets() throws StorageException;
    public abstract void createBucket(Bucket bucket) throws StorageException;
    public abstract Bucket getBucket(String bucketName) throws StorageException;
    public abstract void deleteBucket(String bucketName) throws StorageException;
    public abstract StorageObject createObject(StorageObject object, byte[] data) throws StorageException;
    public abstract StorageObject upload(String uploadId, InputStream inputStream) throws StorageException;

    public void storeUpload(String uploadId, StorageObject object) throws StorageException {
        uploads.put(uploadId, object);
    }

    protected StorageObject getUpload(String uploadId) throws StorageException {
        StorageObject object = uploads.get(uploadId);
        if (object != null) return  object;
        throw StorageExceptionFactory.getUploadIdNotFound(uploadId);
    }
}
