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

package io.github.mahmoudbahaa.gcsemulator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.cloud.WriteChannel;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import com.google.cloud.storage.StorageOptions;
import io.github.mahmoudbahaa.gcsemulator.store.FileStore;
import io.github.mahmoudbahaa.gcsemulator.store.MemoryStore;
import io.github.mahmoudbahaa.gcsemulator.store.NioStore;
import io.github.mahmoudbahaa.gcsemulator.store.Store;
import io.github.mahmoudbahaa.gcsemulator.store.helper.FileStoreConfig;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpStatus;
import org.jboss.resteasy.spi.config.SizeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class GcsEmulatorTest {
    private static final int PORT1 = 8080;
    private static final int PORT2 = 8081;
    private static final int PORT3 = 8082;
    private static final String BUCKET1= "bucket1";
    private static final String BUCKET2= "bucket2";
    private static final String OBJECT1= "object1";
    private static final String SHORT_DATA= "Hello World!";
    private static final int BIG_DATA_SIZE = (int) SizeUnit.GIGABYTE.toBytes(4);
    private static final byte[] BIG_DATA = new byte[BIG_DATA_SIZE];
    private static final String INVALID_BUCKET= "***bucket***";
    private static final Store memoryStore = new MemoryStore();
    private static final Store nioStore = new NioStore();
    private static final Store fileStore = new FileStore(new FileStoreConfig("storage", true));
    private static GCSServer SERVER = null;

    static {
        Arrays.fill(BIG_DATA, (byte) 'a');
    }

    private final Storage STORAGE;

    public GcsEmulatorTest(int port, Store store) {
        if (SERVER != null) SERVER.stop();
        SERVER = new GCSServer(port, store).start();
        STORAGE = StorageOptions
            .newBuilder()
            .setHost("http://localhost:" + port)
            .build()
            .getService();
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        FileUtils.deleteQuietly(new File("storage"));
        return Arrays.asList(
            new Object[] {PORT1, memoryStore},
            new Object[] {PORT2, nioStore},
            new Object[] {PORT3, fileStore}
        );
    }

    @Before
    public void before() {
        Bucket bucket = STORAGE.create(BucketInfo.of(BUCKET1));
        assertNotNull(bucket);
    }

    @After
    public void after() {
        STORAGE.delete(BUCKET1);
    }

    @Test
    public void testCreateListBucket() {
        Bucket bucket;
        StorageException err = null;
        try {
            STORAGE.create(BucketInfo.of(BUCKET1));
        } catch (StorageException e) {
            err = e;
            assertEquals(HttpStatus.SC_CONFLICT, e.getCode());
            assertEquals(e.getMessage(), "Your previous request to create the named bucket succeeded and you already own it.");
        }

        assertNotNull(err);
        err = null;
        try {
            STORAGE.create(BucketInfo.of(INVALID_BUCKET));
        } catch (StorageException e) {
            err = e;
            assertEquals(HttpStatus.SC_BAD_REQUEST, e.getCode());
        }

        assertNotNull(err);
        bucket = STORAGE.create(BucketInfo.of(BUCKET2));
        assertNotNull(bucket);

        Set<String> buckets = new HashSet<>();
        for (Bucket aBucket : STORAGE.list().iterateAll()) {
            buckets.add(aBucket.getName());
        }

        assertEquals(2, buckets.size());
        assertTrue(buckets.contains(BUCKET1));
        assertTrue(buckets.contains(BUCKET2));
        STORAGE.delete(BUCKET2);
    }

    @Test
    public void testDeleteBucket() {
        assertTrue(STORAGE.delete(BUCKET1));
        Bucket bucket = STORAGE.create(BucketInfo.of(BUCKET1));
        assertNotNull(bucket);
    }

    @Test
    public void testGetBucket() {
        Bucket bucket = STORAGE.get(BUCKET1);
        assertNotNull(bucket);
        assertEquals(BUCKET1, bucket.getName());
        assertNotNull(bucket.getGeneratedId());
        assertNotNull(bucket.getCreateTimeOffsetDateTime());
        assertNotNull(bucket.getUpdateTimeOffsetDateTime());
        assertNotNull(bucket.getLocation());
    }

    @Test
    public void testCreateObject() {
        Bucket bucket = STORAGE.get(BUCKET1);
        Blob blob = bucket.create(OBJECT1, SHORT_DATA.getBytes(StandardCharsets.UTF_8));
        assertNotNull(blob);
    }

    @Test
    public void testCreateObjectEmpty() {
        Bucket bucket = STORAGE.get(BUCKET1);
        Blob blob = bucket.create(OBJECT1, new byte[0]);
        assertNotNull(blob);
    }

    @Test
    public void testCreateSmallObject() {
        Bucket bucket = STORAGE.get(BUCKET1);
        Blob blob = bucket.create(OBJECT1, SHORT_DATA.getBytes(StandardCharsets.UTF_8));
        assertNotNull(blob);
    }

    @Test
    public void testCreateLargeObject() {
        Bucket bucket = STORAGE.get(BUCKET1);
        Blob blob = bucket.create(OBJECT1, BIG_DATA);
        assertNotNull(blob);
    }

    @Test
    public void testCreateResumableObject() throws IOException {
        Bucket bucket = STORAGE.get(BUCKET1);
        Blob blob = STORAGE.create(BlobInfo.newBuilder(bucket, OBJECT1).build(), new byte[0]);
        assertNotNull(blob);

        int numWrites = 50;
        try (WriteChannel writer = blob.writer()) {
            for (int i = 0; i < numWrites; i++) {
                writer.write(ByteBuffer.wrap(SHORT_DATA.getBytes(StandardCharsets.UTF_8)));
            }
        }

        assertNotNull(blob);
    }
}
