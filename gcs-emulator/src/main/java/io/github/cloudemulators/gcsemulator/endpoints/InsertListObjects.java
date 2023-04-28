//package io.github.cloudemulators.gcsemulator.endpoints;
//
//import java.io.IOException;
//import java.math.BigInteger;
//import java.util.ArrayList;
//import java.util.Date;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.zip.CRC32C;
//
//import org.apache.commons.codec.digest.DigestUtils;
//
//import com.google.api.client.json.GenericJson;
//import com.google.api.client.util.DateTime;
//import com.google.api.services.storage.model.ObjectAccessControl;
//import com.google.api.services.storage.model.StorageObject;
//
//import io.github.cloudemulators.gcsemulator.GCSServer;
//import store.io.github.cloudemulators.gcsemulator.Store;
//
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//
//public class InsertListObjects extends Base {
//    private static final String UPLOAD_TYPE_MEDIA = "media";
//    private static final String UPLOAD_TYPE_MULTIPART = "multipart";
//    private static final String UPLOAD_TYPE_RESUMABLE = "resumable";
//    private static final String METHOD_POST = "POST";
//    private static final String METHOD_PUT = "PUT";
//
////    @Override
////    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
////        Store store = GCSServer.getServer(request.getLocalPort()).getStore();
////        String bucketName = request.getPathInfo().substring(1);
////        Bucket bucket = exec(response, () -> store.getBucket(bucketName));
////        if (bucket != null) {
////            bucket.setFactory(JSON_FACTORY);
////            response.getWriter().println(bucket);
////        }
////    }
//
////    @Override
//    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
//        String uploadType = request.getParameter("uploadType");
//        if ((uploadType == null || uploadType.isEmpty()) && UPLOAD_TYPE_RESUMABLE.equals(request.getHeader("X-Goog-Upload-Protocol")))  {
//            uploadType = UPLOAD_TYPE_RESUMABLE;
//        } else if (uploadType == null || uploadType.isEmpty()) {
//            sendError(HttpServletResponse.SC_BAD_REQUEST, "invalid uploadType", response);
//            return;
//        }
//
//        switch (uploadType) {
//        case UPLOAD_TYPE_MEDIA:
//            simpleUpload(request, response);
//            return;
//        case UPLOAD_TYPE_MULTIPART:
//            multipartUpload(request, response);
//        case UPLOAD_TYPE_RESUMABLE:
//            break;
//        default:
//            if (request.getHeader("X-Goog-Algorithm") != null) {
//                switch (request.getMethod()) {
//                case METHOD_POST:
//                case METHOD_PUT:
//                    break;
//                default:
//                    sendError(HttpServletResponse.SC_BAD_REQUEST, "invalid uploadType", response);
//                }
//            }
//        }
//
//        sendError(HttpServletResponse.SC_BAD_REQUEST, "invalid uploadType", response);
//    }
//
//    private void simpleUpload(HttpServletRequest request, HttpServletResponse response) throws IOException {
//        Store store = GCSServer.getStore();
//        String name = request.getParameter("name");
//        String predefinedACL = request.getParameter("predefinedAcl");
//        String contentEncoding = request.getParameter("contentEncoding");
//        String customTime = request.getParameter("customTime");
//        if (name == null || name.isEmpty()) {
//            sendError(HttpServletResponse.SC_BAD_REQUEST, "name is required for simple uploads", response);
//            return;
//        }
//
//        byte[] data = new byte[0];
//        StorageObject object = new StorageObject();
//        object.setName(name);
//        object.setBucket(getBucketName(request));
//        object.setContentType(request.getHeader("Content-Type"));
//        object.setContentEncoding(contentEncoding);
//        object.setCustomTime(DateTime.parseRfc3339(customTime));
//        object.setAcl(getAcl(predefinedACL));
//        calculateExtraFields(object, data);
//
//        boolean success = exec(response, () -> store.createObject(object, data));
//        sendResponse(success, object, response);
//    }
//
//    private void multipartUpload(HttpServletRequest request, HttpServletResponse response) throws IOException {
//        String[] contentTypeParts = request.getHeader("Content-Type").split(";");
//        String mediatype = contentTypeParts[0];
//        if (!mediatype.startsWith(UPLOAD_TYPE_MULTIPART)) {
//            sendError(HttpServletResponse.SC_BAD_REQUEST, "invalid Content-Type header", response);
//            return;
//        }
//        Map<String, String> params = new HashMap<>();
//        for (int i = 1; i < contentTypeParts.length; i++) {
//            String[] paramParts = contentTypeParts[i].split("=");
//            if (paramParts.length != 2) {
//                sendError(HttpServletResponse.SC_BAD_REQUEST, "invalid Content-Type header", response);
//                return;
//            }
//
//            params.put(paramParts[0], paramParts[1]);
//        }
//
//
//        String boundary = params.get("boundary");
//        if (boundary == null) {
//            sendError(HttpServletResponse.SC_BAD_REQUEST, "invalid Content-Type header", response);
//            return;
//        }
//
//        //        var (
////            metadata *multipartMetadata
////            content  []byte
////	)
////        var contentType string
////        reader := multipart.NewReader(r.Body, params["boundary"])
////
////        var partReaders []io.Reader
////
////        part, err := reader.NextPart()
////        for ; err == nil; part, err = reader.NextPart() {
////            if metadata == nil {
////                metadata, err = loadMetadata(part)
////                contentType = metadata.ContentType
////            } else {
////                contentType = part.Header.Get(contentTypeHeader)
////                content, err = loadContent(part)
////                partReaders = append(partReaders, bytes.NewReader(content))
////            }
////            if err != nil {
////                break
////            }
////        }
////        if err != io.EOF {
////            return jsonResponse{errorMessage: err.Error()}
////        }
////
////        objName := r.URL.Query().Get("name")
////        predefinedACL := r.URL.Query().Get("predefinedAcl")
////        if objName == "" {
////            objName = metadata.Name
////        }
////
////        conditions, err := s.wrapUploadPreconditions(r, bucketName, objName)
////        if err != nil {
////            return jsonResponse{
////                status:       http.StatusBadRequest,
////                    errorMessage: err.Error(),
////            }
////        }
////
////        obj := StreamingObject{
////            ObjectAttrs: ObjectAttrs{
////                BucketName:      bucketName,
////                    Name:            objName,
////                    ContentType:     contentType,
////                    ContentEncoding: metadata.ContentEncoding,
////                    CustomTime:      metadata.CustomTime,
////                    ACL:             getObjectACL(predefinedACL),
////                    Metadata:        metadata.Metadata,
////            },
////            Content: notImplementedSeeker{io.NopCloser(io.MultiReader(partReaders...))},
////        }
////
////        obj, err = s.createObject(obj, conditions)
////        if err != nil {
////            return errToJsonResponse(err)
////        }
////        defer obj.Close()
////        return jsonResponse{data: newObjectResponse(obj.ObjectAttrs)}
//    }
//
//    private String getBucketName(HttpServletRequest request) {
//        String uri = request.getRequestURI();
//        int start = "/upload/storage/v1/b/".length();
//        int end = uri.indexOf("/o");
//        return uri.substring(start, end);
//    }
//
//    private List<ObjectAccessControl> getAcl(String predefinedACL) {
//        List<ObjectAccessControl> acl = new ArrayList<>();
//        ObjectAccessControl accessControl = new ObjectAccessControl();
//        if (predefinedACL.equals("publicRead")) {
//            accessControl.setEntity("allUsers");
//            accessControl.setRole("READER");
//        } else {
//            accessControl.setEntity("projectOwner-test-project");
//            accessControl.setRole("OWNER");
//        }
//
//        acl.add(accessControl);
//        return acl;
//    }
//

//
//    private void sendResponse(boolean success, StorageObject object, HttpServletResponse response) throws IOException {
//        if (!success) return;
//        GenericJson res = new GenericJson();
//        res.setFactory(JSON_FACTORY);
//        res.put("data", object);
//        response.getWriter().println(res);
//    }
//}
