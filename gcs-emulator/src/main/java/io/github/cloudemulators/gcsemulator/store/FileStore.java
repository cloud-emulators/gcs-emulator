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
import io.github.cloudemulators.gcsemulator.store.helper.FileStoreConfig;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

public class FileStore extends Store {
    private static final Logger logger = LoggerFactory.getLogger(FileStore.class);
    private static final String TIME_CREATED_ATTRIBUTE = "user:time created";
    private final FileStoreConfig config;

    public FileStore(FileStoreConfig config) {
        this.config = config;
        File parent = new File(config.getLocation());
        if (config.isStartClean()) {
            FileUtils.deleteQuietly(parent);
            try {
                Files.createDirectories(parent.toPath());
            } catch (IOException e) {
                logger.error("failed to create parent store location", e);
            }
        }
    }

    public Iterator<Bucket> listBuckets() {
        File parentLocation = new File(config.getLocation());
        File[] bucketFiles = parentLocation.listFiles();
        if (bucketFiles == null) return Collections.emptyIterator();
        List<Bucket> buckets = new ArrayList<>();
        for (File file : bucketFiles) {
            buckets.add(getBucketFromFile(file));
        }
        return buckets.iterator();
    }

    public void createBucket(Bucket bucket) throws StorageException {
        if (!validNames.matcher(bucket.getName()).matches()) {
            throw StorageExceptionFactory.getInvalidBucketName(bucket);
        }

        File bucketFile = getBucketFile(bucket.getName(), false);
        if (bucketFile.exists()) {
            throw StorageExceptionFactory.getBucketConflict(bucket);
        }

        try {
            Files.createDirectory(bucketFile.toPath());
        } catch (IOException e) {
            throw StorageExceptionFactory.getErrorWhileCreatingBucket(e);
        }

        DateTime now = new DateTime(new Date());
        try {
            Files.setAttribute(bucketFile.toPath(), TIME_CREATED_ATTRIBUTE,
                    now.toStringRfc3339().getBytes(StandardCharsets.UTF_8));
        } catch (IOException ignored) { /**/ }
    }

    @Override
    public Bucket getBucket(String bucketName) throws StorageException {
        return getBucketFromFile(getBucketFile(bucketName, true));
    }

    @Override
    public void deleteBucket(String bucketName) throws StorageException {
        File bucketFile = getBucketFile(bucketName, true);
        try {
            Files.delete(bucketFile.toPath());
        } catch (IOException e) {
            throw StorageExceptionFactory.getErrorWhileDeletingBucket(e);
        }
    }

    @Override
    public StorageObject createObject(StorageObject object, byte[] data) throws StorageException {
        String bucketName = object.getBucket();
        File bucketFile = getBucketFile(bucketName, true);
        File objectFile = new File(bucketFile, object.getName());
        try {
            Files.createFile(objectFile.toPath());
        } catch (IOException e) {
            throw StorageExceptionFactory.getErrorWhileCreatingObject(e);
        }

        try (FileOutputStream fos = new FileOutputStream(objectFile);
             BufferedOutputStream bos = new BufferedOutputStream(fos)) {
            bos.write(data);
        } catch (IOException e) {
            throw StorageExceptionFactory.getErrorWhileCreatingObject(e);
        }

        return object;
    }

    @Override
    public StorageObject upload(String uploadId, InputStream inputStream) throws StorageException {
        StorageObject object = getUpload(uploadId);
        File bucketFile = getBucketFile(object.getBucket(), true);
        File objectFile = new File(bucketFile, object.getName());
        try {
            Files.createFile(objectFile.toPath());
        } catch (IOException e) {
            throw StorageExceptionFactory.getErrorWhileCreatingObject(e);
        }

        try (FileOutputStream fos = new FileOutputStream(objectFile);
             BufferedOutputStream bos = new BufferedOutputStream(fos)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }

        } catch (IOException e) {
            throw StorageExceptionFactory.getErrorWhileCreatingObject(e);
        }

        return object;
    }

    private Bucket getBucketFromFile(File file) {
        Bucket bucket = new Bucket();
        bucket.setId(file.getName());
        bucket.setName(file.getName());
        try {
            byte[] timeCreatedBytes = (byte[]) Files.getAttribute(file.toPath(), TIME_CREATED_ATTRIBUTE);
            String timeCreated = new String(timeCreatedBytes, StandardCharsets.UTF_8);
            bucket.setTimeCreated(DateTime.parseRfc3339(timeCreated));
        } catch (IOException ignored) { /**/ }
        bucket.setUpdated(new DateTime(file.lastModified()));
        bucket.setLocation("US-CENTRAL1");
        return bucket;
    }

    private File getBucketFile(String bucketName, boolean forceExists) throws StorageException {
        File parentLocation = new File(config.getLocation());
        File bucketFile = new File(parentLocation, bucketName);
        if (forceExists && !bucketFile.exists()) {
            throw StorageExceptionFactory.getBucketNotFound(bucketName);
        }

        return bucketFile;
    }
}
