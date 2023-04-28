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

package io.github.cloudemulators.gcsemulator.store;

import com.google.api.client.util.DateTime;
import com.google.api.services.storage.model.Bucket;
import com.google.api.services.storage.model.StorageObject;
import com.google.cloud.storage.StorageException;
import io.github.cloudemulators.gcsemulator.StorageExceptionFactory;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MemoryStore extends Store {
    private static final Logger logger = LoggerFactory.getLogger(MemoryStore.class);
    private final Map<String, Bucket> buckets = new HashMap<>();
    private final Map<String, Map<String, StorageObject>> objects = new HashMap<>();
    private final Map<String, byte[]> data = new HashMap<>();

    public Iterator<Bucket> listBuckets() {
        return buckets.values().iterator();
    }

    public void createBucket(Bucket bucket) throws StorageException {
        logger.debug("Attempting to create Bucket: {}", bucket.getName());
        if (!validNames.matcher(bucket.getName()).matches()) {
            logger.debug("Bucket name is not valid: {}", bucket.getName());
            throw StorageExceptionFactory.getInvalidBucketName(bucket);
        }

        if (buckets.containsKey(bucket.getName())) {
            throw StorageExceptionFactory.getBucketConflict(bucket);
        }

        bucket.setId(bucket.getName());
        DateTime now = new DateTime(new Date());
        bucket.setTimeCreated(now);
        bucket.setUpdated(now);
        bucket.setLocation("US-CENTRAL1");
        buckets.put(bucket.getName(), bucket);
    }

    public Bucket getBucket(String bucketName) throws StorageException {
        Bucket bucket = buckets.get(bucketName);
        if (bucket == null) {
            throw StorageExceptionFactory.getBucketNotFound(bucketName);
        }

        return bucket;
    }

    @Override
    public void deleteBucket(String bucketName) throws StorageException {
        if (!buckets.containsKey(bucketName)) {
            throw StorageExceptionFactory.getBucketNotFound(bucketName);
        }

        buckets.remove(bucketName);
    }

    @Override
    public StorageObject createObject(StorageObject object, byte[] data) throws StorageException {
        String bucketName = object.getBucket();
        if (!buckets.containsKey(bucketName)) {
            throw StorageExceptionFactory.getBucketNotFound(bucketName);
        }

        Map<String, StorageObject> bucketObjects = objects.computeIfAbsent(bucketName, s -> new HashMap<>());
        bucketObjects.put(object.getName(), object);
        this.data.put(bucketName + "." + object.getName(), data);
        return object;
    }

    @Override
    public StorageObject upload(String uploadId, InputStream inputStream) throws StorageException {
        StorageObject object = getUpload(uploadId);
        String key = getDataKey(object);
        byte[] datum = data.get(key);

        byte[] newDatum;
        try {
            newDatum = IOUtils.toByteArray(inputStream);
        } catch (IOException e) {
            throw StorageExceptionFactory.getErrorWhileCreatingObject(e);
        }

        if (datum == null) {
            data.put(key, newDatum);
        } else {
            byte[] merged = new byte[datum.length + newDatum.length];
            System.arraycopy(datum, 0, merged, 0, datum.length);
            System.arraycopy(newDatum, 0, merged, datum.length, newDatum.length);
            data.put(key, merged);
        }

        return object;
    }

    private String getDataKey(StorageObject object) {
        return object.getBucket() + "." + object.getName();
    }

}
