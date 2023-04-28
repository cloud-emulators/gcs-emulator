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

package io.github.cloudemulators.gcsemulator.endpoints.object;

import com.google.api.client.json.GenericJson;
import com.google.api.client.util.DateTime;
import com.google.api.services.storage.model.ObjectAccessControl;
import com.google.api.services.storage.model.StorageObject;
import com.google.cloud.storage.StorageException;
import com.google.common.primitives.Ints;
import io.github.cloudemulators.gcsemulator.StorageExceptionFactory;
import io.github.cloudemulators.gcsemulator.endpoints.Base;
import io.github.cloudemulators.gcsemulator.helpers.Util;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.jboss.resteasy.annotations.GZIP;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartRelatedInput;
import org.jboss.resteasy.spi.HttpRequest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.zip.CRC32C;

@Path("/upload/storage/v1/b/{bucketName}/o")
public class CreateObject extends Base {
    private static final String UPLOAD_TYPE_MEDIA = "media";
    private static final String UPLOAD_TYPE_MULTIPART = "multipart";
    private static final String UPLOAD_TYPE_RESUMABLE = "resumable";

    @Context
    HttpRequest request;

    @POST
    @Produces("application/json")
    @Consumes("multipart/related")
    public Response CreateObject(@PathParam("bucketName") String bucketName, @GZIP MultipartRelatedInput input) throws IOException {
        List<InputPart> parts = input.getParts();
        StorageObject object = JSON_FACTORY.fromInputStream(parts.get(0).getBody(), StorageObject.class);
        if (!object.getBucket().equals(bucketName)) {
            return errorToResponse(StorageExceptionFactory.getBucketNameMismatch());
        }

        calculateDateFields(object);
        byte[] data;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            for (int i = 1; i < parts.size(); i++) {
                InputPart part = parts.get(i);
                bos.write(IOUtils.toByteArray(part.getBody()));
            }

            data = bos.toByteArray();
        } catch (IOException e) {
            throw StorageExceptionFactory.getErrorWhileCreatingObject(e);
        }


        try {
            calculateCheckSums(object, data);
        } catch (StorageException e) {
            return errorToResponse(e);
        }

        try {
            StorageObject object1 = getStore(request).createObject(object, data);
            object1.setFactory(JSON_FACTORY);
            return Response.ok().entity(object1).build();
        } catch (StorageException e) {
            return errorToResponse(e);
        }
    }

    @POST
    @PUT
    @Produces("application/json")
    public Response createObject(@PathParam("bucketName") String bucketName, @GZIP InputStream inputStream) throws IOException {
        MultivaluedMap<String, String> queryParams = request.getUri().getQueryParameters();
        String uploadType = queryParams.getFirst("uploadType");
        if (uploadType == null || uploadType.isEmpty()) {
            String uploadProtocol = request.getHttpHeaders().getHeaderString("X-Goog-Upload-Protocol");
            if (uploadProtocol == null || !uploadProtocol.equals(UPLOAD_TYPE_RESUMABLE)) {
                return errorToResponse(StorageExceptionFactory.getInvalidUploadType(uploadType));
            }

            uploadType = UPLOAD_TYPE_RESUMABLE;
        }

        switch (uploadType) {
            case UPLOAD_TYPE_MEDIA:
                return createObjectMedia(bucketName, inputStream);
            case UPLOAD_TYPE_RESUMABLE:
                return createObjectResumable(bucketName, inputStream);
            case UPLOAD_TYPE_MULTIPART:
            default:
                return errorToResponse(StorageExceptionFactory.getInvalidUploadType(uploadType));
        }
    }

    private Response createObjectResumable(String bucketName, InputStream inputStream) throws IOException {
        MultivaluedMap<String, String> queryParams = request.getUri().getQueryParameters();
        String uploadId = queryParams.getFirst("upload_id");
        if (uploadId != null && !uploadId.isEmpty()) {
            return uploadFileContent(uploadId, inputStream);
        }

        boolean hasEmptyBody;
        PushbackInputStream pushbackInputStream = new PushbackInputStream(inputStream);
        int b = pushbackInputStream.read();
        hasEmptyBody = b == -1;
        pushbackInputStream.unread(b);
        StorageObject object = new StorageObject();
        if (!hasEmptyBody) {
            try {
                object = JSON_FACTORY.fromInputStream(pushbackInputStream, StorageObject.class);
            } catch (IOException e) {
                return errorToResponse(StorageExceptionFactory.getErrorWhileCreatingObject(e));
            }
        }

        String name = queryParams.getFirst("name");
        String predefinedACL = queryParams.getFirst("predefinedAcl");
        String contentEncoding = queryParams.getFirst("contentEncoding");
        if (name != null && !name.isEmpty()) object.setName(name);
        if (contentEncoding != null && !contentEncoding.isEmpty()) object.setContentEncoding(contentEncoding);
        if (predefinedACL != null && !predefinedACL.isEmpty()) object.setAcl(getAcl(predefinedACL));

        object.setBucket(bucketName);
        uploadId = generateUploadID();
        getStore(request).storeUpload(uploadId, object);
        String location = String.format("%supload/storage/v1/b/%s/o?uploadType=resumable&name=%s&upload_id=%s",
                request.getUri().getBaseUri(),
                bucketName,
                URLEncoder.encode(name, StandardCharsets.UTF_8),
                uploadId);
        GenericJson json = new GenericJson();
        json.put("data", object);
        json.setFactory(JSON_FACTORY);
        Response.ResponseBuilder responseBuilder = Response.ok().entity(json).header("location", location);
        String command = request.getHttpHeaders().getHeaderString("X-Goog-Upload-Command");
        if (command != null && command.equals("start")) {
            responseBuilder = responseBuilder.header("X-Goog-Upload-URL", location);
            responseBuilder = responseBuilder.header("X-Goog-Upload-Status", "active");
        }
        return responseBuilder.build();
    }

    private String generateUploadID() {
        return Util.randomHexString(16);
    }

    private Response uploadFileContent(String uploadId, InputStream inputStream) {
        try {
            StorageObject object = getStore(request).upload(uploadId, inputStream);
            return Response.ok().entity(object).build();
        } catch (StorageException e) {
            return errorToResponse(e);
        }
    }

    private Response createObjectMedia(String bucketName, InputStream inputStream) {
        return errorToResponse(StorageExceptionFactory.getInvalidUploadType(UPLOAD_TYPE_MEDIA));

    }

    private void calculateDateFields(StorageObject object) {
        DateTime now = new DateTime(new Date());
        object.setGeneration(now.getValue());
        object.setTimeCreated(now);
        object.setUpdated(now);
    }

    private void calculateCheckSums(StorageObject object, byte[] data) {
        Base64.Encoder encoder = Base64.getEncoder();

        object.setSize(BigInteger.valueOf(data.length));

        String md5Hash = object.getMd5Hash();

        String calculatedMd5hash = encoder.encodeToString(DigestUtils.md5(data));

        String crc = object.getCrc32c();
        CRC32C crc32C = new CRC32C();
        crc32C.update(data);
        String calculatedCrc = encoder.encodeToString(Ints.toByteArray((int) crc32C.getValue()));

        if ((md5Hash != null && !md5Hash.equals(calculatedMd5hash)) ||
                (crc != null && !crc.equals(calculatedCrc))) {
            throw StorageExceptionFactory.getCorruptData();
        }

        object.setCrc32c(calculatedCrc);
        object.setMd5Hash(calculatedMd5hash);

        if (object.getEtag() == null || object.getEtag().isEmpty()) {
            object.setEtag(object.getMd5Hash());
        }
    }

    private List<ObjectAccessControl> getAcl(String predefinedACL) {
        List<ObjectAccessControl> acl = new ArrayList<>();
        ObjectAccessControl accessControl = new ObjectAccessControl();
        if (predefinedACL.equals("publicRead")) {
            accessControl.setEntity("allUsers");
            accessControl.setRole("READER");
        } else {
            accessControl.setEntity("projectOwner-test-project");
            accessControl.setRole("OWNER");
        }

        acl.add(accessControl);
        return acl;
    }
}
