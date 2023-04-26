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


import io.github.mahmoudbahaa.gcsemulator.store.MemoryStore;
import io.github.mahmoudbahaa.gcsemulator.store.Store;
import java.util.HashMap;
import java.util.Map;
import org.jboss.resteasy.plugins.server.undertow.UndertowJaxrsServer;

public class GCSServer {
    private static final Map<Integer, Store> stores = new HashMap<>();
    private final UndertowJaxrsServer server;
    private final int port;

    public GCSServer(int port, Store store) {
        if (stores.containsKey(port)) {
            throw new IllegalArgumentException("port already used: " + port);
        }

        if (store == null) {
            store = new MemoryStore();
        }

        this.port = port;
        stores.put(port, store);
        server = new UndertowJaxrsServer();
        server.deploy(GCSApplication.class);
        server.setPort(port);
    }

    public static Store getStore(int port) {
        return stores.get(port);
    }

    public GCSServer start() {
        server.start();
        return this;
    }

    public void stop() {
        server.stop();
        stores.remove(port);
    }
}
