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

import io.github.mahmoudbahaa.gcsemulator.store.FileStore;
import io.github.mahmoudbahaa.gcsemulator.store.MemoryStore;
import io.github.mahmoudbahaa.gcsemulator.store.NioStore;
import io.github.mahmoudbahaa.gcsemulator.store.helper.FileStoreConfig;

public class Main {
    public static void main(String[] args) {
        new GCSServer(8080, new MemoryStore()).start();
        System.out.println("Memory Store Server started on port 8080");
        new GCSServer(8081, new NioStore()).start();
        System.out.println("Nio Store Server started on port 8081");
        new GCSServer(8082, new FileStore(new FileStoreConfig("storage", true))).start();
        System.out.println("File Store Server started on port 8082");
    }
}