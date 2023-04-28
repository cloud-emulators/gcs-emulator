#  Copyright (c) 2023 Mahmoud Bahaa and others
#
#  Permission is hereby granted, free of charge, to any person obtaining
#  a copy of this software and associated documentation files (the
#  "Software"), to deal in the Software without restriction, including
#  without limitation the rights to use, copy, modify, merge, publish,
#  distribute, sublicense, and/or sell copies of the Software, and to
#  permit persons to whom the Software is furnished to do so, subject to
#  the following conditions:
#
#  The above copyright notice and this permission notice shall be
#  included in all copies or substantial portions of the Software.
#
#  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
#  EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
#  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
#  NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
#  LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
#  OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
#  WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.pip

#
#  Permission is hereby granted, free of charge, to any person obtaining
#  a copy of this software and associated documentation files (the
#  "Software"), to deal in the Software without restriction, including
#  without limitation the rights to use, copy, modify, merge, publish,
#  distribute, sublicense, and/or sell copies of the Software, and to
#  permit persons to whom the Software is furnished to do so, subject to
#  the following conditions:
#
#
import http.client

from google.auth.credentials import AnonymousCredentials
import pytest
from google.cloud import storage
from google.cloud.exceptions import *

BUCKET1 = "bucket1"
BUCKET2 = "bucket2"
INVALID_BUCKET_NAME = "a****bucket****a"


@pytest.fixture
def storage_client():
    storage_client = storage.Client(project="test-project",
                                    credentials=AnonymousCredentials(),
                                    client_options={"api_endpoint": "http://localhost:8080"})
    bucket: storage.Bucket = storage_client.bucket(BUCKET1)
    try:
        bucket.delete()
    except ClientError as e:
        assert e.code == http.client.NOT_FOUND
    return storage_client


class TestBucket:

    def test_create_bucket_valid_name(self, storage_client):
        new_bucket = storage_client.create_bucket(BUCKET1)
        assert new_bucket is not None

    def test_create_bucket_invalid_name_should_fail(self, storage_client):
        # noinspection PyGlobalUndefined
        global error
        try:
            storage_client.create_bucket(INVALID_BUCKET_NAME)
        except ClientError as e:
            error = e
            assert e.code == http.client.BAD_REQUEST
            assert str.__contains__(e.message, "Invalid bucket name")
        assert error is not None

    def test_create_bucket_twice_should_fail(self, storage_client):
        storage_client.create_bucket(BUCKET1)
        # noinspection PyGlobalUndefined
        global error
        try:
            storage_client.create_bucket(BUCKET1)
        except ClientError as e:
            error = e
            assert e.code == http.client.CONFLICT
            assert str.__contains__(e.message, "Your previous request to create the named bucket succeeded and you "
                                               "already own it.")
        assert error is not None
