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

import com.google.api.services.storage.model.Bucket;
import com.google.cloud.storage.StorageException;
import java.io.IOException;
import org.apache.http.HttpStatus;

public class StorageExceptionFactory {

  private static final String ERROR_OCCURRED = "An error occurred while ";
  private static final String CREATION_ERROR = ERROR_OCCURRED + "creating ";
  private static final String CREATION_OBJECT_ERROR = CREATION_ERROR + "object: ";

  public static StorageException getInvalidBucketName(Bucket bucket) {
    return new StorageException(
        HttpStatus.SC_BAD_REQUEST, String.format("Invalid bucket name '%s'", bucket.getName()), "invalid", null);
  }

  public static StorageException getBucketConflict(Bucket bucket) {
    return new StorageException(
        HttpStatus.SC_CONFLICT,
        "Your previous request to create the named bucket succeeded and you already own it.",
        "conflict",
        null);
  }

  public static StorageException getBucketNotFound(String bucketName) {
    return new StorageException(HttpStatus.SC_NOT_FOUND, "The specified bucket does not exist.", "notFound", null);
  }

  public static StorageException getCorruptData() {
    return new StorageException(HttpStatus.SC_BAD_REQUEST, "md5 or crc didn't match");
  }

  public static StorageException getErrorWhileCreatingBucket(IOException e) {
    return new StorageException(
        HttpStatus.SC_INTERNAL_SERVER_ERROR, CREATION_ERROR + "bucket: " + e);
  }

  public static StorageException getErrorWhileCreatingObject(Exception e) {
    return new StorageException(HttpStatus.SC_INTERNAL_SERVER_ERROR, CREATION_OBJECT_ERROR + e);
  }

  public static StorageException getErrorWhileDeletingBucket(IOException e) {
    return new StorageException(
        HttpStatus.SC_INTERNAL_SERVER_ERROR, ERROR_OCCURRED + "deleting bucket: " + e);
  }

  public static StorageException getBucketNameMismatch() {
    return new StorageException(
        HttpStatus.SC_BAD_REQUEST, "Bucket Name doesn't match the one in the request");
  }

  public static StorageException getInvalidUploadType(String uploadType) {
    return new StorageException(HttpStatus.SC_BAD_REQUEST, "invalid uploadType");
  }

  public static StorageException getUploadIdNotFound(String uploadId) {
    return new StorageException(HttpStatus.SC_NOT_FOUND, null);
  }
}
