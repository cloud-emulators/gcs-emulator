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

import com.google.api.client.util.DateTime;
import com.google.api.services.storage.model.Bucket;
import com.google.api.services.storage.model.StorageObject;
import com.google.cloud.WriteChannel;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import com.google.cloud.storage.contrib.nio.testing.LocalStorageHelper;
import io.github.mahmoudbahaa.gcsemulator.StorageExceptionFactory;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.Iterator;

public class NioStore extends Store {
    private static final Storage storage = LocalStorageHelper.customOptions(false).getService();
    private final MemoryStore memoryStore = new MemoryStore();
    @Override
    public Iterator<Bucket> listBuckets() throws StorageException {
        return memoryStore.listBuckets();
    }

    @Override
    public void createBucket(Bucket bucket) throws StorageException {
        memoryStore.createBucket(bucket);
    }

    @Override
    public Bucket getBucket(String bucketName) throws StorageException {
        return memoryStore.getBucket(bucketName);
    }

    @Override
    public void deleteBucket(String bucketName) throws StorageException {
        memoryStore.deleteBucket(bucketName);
    }

    @Override
    public StorageObject createObject(StorageObject object, byte[] data) throws StorageException {
        getBucket(object.getBucket());
        Blob blob = storage.create(BlobInfo.newBuilder(object.getBucket(), object.getName()).build(), data);
        return fromBlob(blob);
    }

    @Override
    public StorageObject upload(String uploadId, InputStream inputStream) throws StorageException {
        StorageObject object = getUpload(uploadId);
        Blob blob = storage.get(BlobId.of(object.getBucket(), object.getName()));
        if (blob == null) {
            storage.create(BlobInfo.newBuilder(object.getBucket(), object.getName()).build(), new byte[0]);
            blob = storage.get(BlobId.of(object.getBucket(), object.getName()));
            if (blob == null) throw StorageExceptionFactory.getErrorWhileCreatingObject(null);
        }

        try (WriteChannel writer = blob.writer()) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                ByteBuffer byteBuffer = ByteBuffer.wrap(buffer, 0, len);
                writer.write(byteBuffer);
            }
        } catch (IOException e) {
            throw StorageExceptionFactory.getErrorWhileCreatingObject(e);
        }

        return object;
    }

    private StorageObject fromBlob(Blob blob) {
        StorageObject object = new StorageObject();
        object.setBucket(blob.getBucket());
        object.setName(blob.getName());
        object.setMetageneration(blob.getMetageneration());
        object.setTimeDeleted(toDateTime(blob.getDeleteTimeOffsetDateTime()));
        object.setUpdated(toDateTime(blob.getUpdateTimeOffsetDateTime()));
        object.setTimeCreated(toDateTime(blob.getCreateTimeOffsetDateTime()));
        object.setCustomTime(toDateTime(blob.getCustomTimeOffsetDateTime()));
        object.setId(blob.getGeneratedId());
        object.setSelfLink(blob.getSelfLink());
        object.setCacheControl(blob.getCacheControl());
        object.setSize(blob.getSize() == null ? BigInteger.ZERO : BigInteger.valueOf(blob.getSize()));
        object.setEtag(blob.getEtag());
        object.setMd5Hash(blob.getMd5());
        object.setCrc32c(blob.getCrc32c());
        object.setMediaLink(blob.getMediaLink());
        object.setMetadata(blob.getMetadata());
        object.setContentType(blob.getContentType());
        object.setContentEncoding(blob.getContentEncoding());
        object.setStorageClass(blob.getStorageClass() == null ? null : blob.getStorageClass().toString());
        object.setTimeStorageClassUpdated(toDateTime(blob.getTimeStorageClassUpdatedOffsetDateTime()));
        //        object.setAcl(blob.getAcl());
        //        object.setOwner(blob.getOwner());
        //        object.setCustomerEncryption(blob.getCustomerEncryption());
        object.setKmsKeyName(blob.getKmsKeyName());
        object.setEventBasedHold(blob.getEventBasedHold());
        object.setTemporaryHold(blob.getTemporaryHold());
        object.setRetentionExpirationTime(toDateTime(blob.getRetentionExpirationTimeOffsetDateTime()));
        object.setGeneration(blob.getGeneration());
        return object;
    }

    private DateTime toDateTime(OffsetDateTime offsetDateTime) {
        if (offsetDateTime == null) return null;
        return new DateTime(Date.from(offsetDateTime.toInstant()));
    }
}
