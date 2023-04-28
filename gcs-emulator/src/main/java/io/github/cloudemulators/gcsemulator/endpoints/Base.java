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

package io.github.cloudemulators.gcsemulator.endpoints;


import com.google.api.client.json.GenericJson;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.cloud.storage.StorageException;
import io.github.cloudemulators.gcsemulator.GCSServer;
import io.github.cloudemulators.gcsemulator.store.Store;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.spi.HttpRequest;

public class Base {
    protected static final JsonFactory JSON_FACTORY = GsonFactory.builder().setReadLeniency(true).build();

    protected Store getStore(HttpRequest request) {
        return GCSServer.getStore(request.getUri().getBaseUri().getPort());
    }

    //{"code"=>403, "message"=>"message",
    // "errors"=>[{"message"=>"message", "domain"=>"global", "reason"=>"forbidden"}]}
    protected Response errorToResponse(StorageException err) {
        GenericJson res = new GenericJson();
        res.setFactory(JSON_FACTORY);

        GenericJson[] errors = new GenericJson[1];
        errors[0] = new GenericJson();
        errors[0].put("message", err.getMessage());
        errors[0].put("domain", "global");
        errors[0].put("reason", err.getReason());

        GenericJson error = new GenericJson();
        error.put("code", err.getCode());
        error.put("message", err.getMessage());
        error.put("errors", errors);

        res.put("error", error);
        return Response.status(err.getCode()).entity(res.toString()).build();
    }
}
