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

package io.github.cloudemulators.gcsemulator.endpoints.bucket;

import com.google.api.client.json.GenericJson;
import com.google.api.services.storage.model.Bucket;
import io.github.cloudemulators.gcsemulator.endpoints.Base;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.spi.HttpRequest;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Path("/storage/v1/b")
public class ListBuckets extends Base {

    @Context
    HttpRequest request;

    @GET
    @Produces("application/json")
    @Consumes("application/json")
    public Response listBuckets() {
        Iterator<Bucket> itr = getStore(request).listBuckets();
        List<Bucket> buckets = new ArrayList<>();
        while (itr.hasNext()) {
            buckets.add(itr.next());
        }

        GenericJson res = new GenericJson();
        res.put("kind", "storage#buckets");
        res.put("items", buckets);
        res.setFactory(JSON_FACTORY);
        return Response.ok(res).build();
    }
}
