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

import {beforeEach} from "mocha";
import {Bucket, Storage} from "@google-cloud/storage";
import {ApiError} from "@google-cloud/storage/build/src/nodejs-common";
import {SC} from "./statuscodes"

const assert = require("assert");

describe('test bucket Operations', function () {
    const storage = new Storage({
        apiEndpoint: "http://localhost:8080/",
        projectId: "test-project",
    });
    const BUCKET1 = new Bucket(storage, "bucket1");
    const INVALID_BUCKET_NAME = "***bucket***";
    // const BUCKET2 = new Bucket(storage, "bucket2");


    beforeEach(async function () {
        try {
            await BUCKET1.delete();
        } catch (err) { /**/
        }
    })

    it('should create bucket successfully', async function () {
        const [bucket] = await storage.createBucket(BUCKET1.name);
        assert.ok(bucket);
        assert.equal(bucket.name, BUCKET1.name);
    });

    it('should not recreate the same bucket but throw an error', async function () {
        let [bucket] = await storage.createBucket(BUCKET1.name);
        assert.ok(bucket);

        let error: ApiError | undefined = undefined;
        try {
            await storage.createBucket(BUCKET1.name);
        } catch (err) {
            error = err as ApiError;
            assert.equal(error.code, SC.CONFLICT)
            assert.equal(error.message, "Your previous request to create the named bucket succeeded and you already own it.")
        }

        assert.ok(error);
    });

    it('should not create a bucket with invalid name', async function () {
        let error: ApiError | undefined = undefined;
        try {
            await storage.createBucket(INVALID_BUCKET_NAME);
        } catch (err) {
            error = err as ApiError;
            assert.equal(error.code, SC.BAD_REQUEST)
            assert.ok(error.message.startsWith("Invalid bucket name "))
        }

        assert.ok(error);
    });
});