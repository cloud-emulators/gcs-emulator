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

package main

import (
	"cloud.google.com/go/storage"
	"context"
	"google.golang.org/api/googleapi"
	"google.golang.org/api/option"
	"strings"
	"testing"
)

var (
	ProjectId     = "project-id"
	Bucket1       = "bucket1"
	InvalidBucket = "****bucket****"
	//BUCKET2   = "bucket2"
)

var ctx context.Context
var client *storage.Client
var bucket1 *storage.BucketHandle

func setup(t *testing.T) func(t *testing.T) {
	ctx = context.Background()
	endpoint := option.WithEndpoint("http://localhost:8080/storage/v1/")
	noAuth := option.WithoutAuthentication()
	var err error
	client, err = storage.NewClient(ctx, endpoint, noAuth)
	if err != nil {
		t.Errorf("storage.NewClient: %v", err)
	}

	bucket1 = client.Bucket(Bucket1)
	_ = bucket1.Delete(ctx)
	return func(t *testing.T) {
		func(client *storage.Client) {
			err := client.Close()
			if err != nil {
				t.Errorf("Error while closing client: %v", err)
			}
		}(client)
	}
}

func TestBucketValid(t *testing.T) {
	defer setup(t)(t)
	if err := bucket1.Create(ctx, ProjectId, &storage.BucketAttrs{}); err != nil {
		t.Errorf("Bucket(%q).Create: %v", Bucket1, err)
	}

	t.Logf("Created bucket %v.\n", Bucket1)
}

func TestBucketInValid(t *testing.T) {
	defer setup(t)(t)
	if err := client.Bucket(InvalidBucket).Create(ctx, ProjectId, &storage.BucketAttrs{}); err != nil {
		t.Logf("Bucket(%q).Create: %v", InvalidBucket, err)
		gErr, ok := err.(*googleapi.Error)
		if !ok {
			t.Errorf("Incorrect Error type thrown: %v", err)
		}

		if gErr.Code != 400 {
			t.Errorf("Incorrect status code returned, expected: 400 but got: %q", gErr.Code)
		}

		if !strings.HasPrefix(gErr.Message, "Invalid bucket name") {
			t.Errorf("Incorrect error message returned: %q", gErr.Message)
		}

		return
	}

	t.Errorf("Should throw an error when bucket name is invalid")
}
func TestBucketAlreadyExists(t *testing.T) {
	defer setup(t)(t)
	if err := bucket1.Create(ctx, ProjectId, &storage.BucketAttrs{}); err != nil {
		t.Errorf("Bucket(%q).Create: %v", Bucket1, err)
	}

	t.Logf("Created bucket %v.\n", Bucket1)

	if err := bucket1.Create(ctx, ProjectId, &storage.BucketAttrs{}); err != nil {
		gErr, ok := err.(*googleapi.Error)
		if !ok {
			t.Errorf("Incorrect Error type thrown: %v", err)
		}

		if gErr.Code != 409 {
			t.Errorf("Incorrect status code returned, expected: 409 but got: %q", gErr.Code)
		}

		if gErr.Message != "Your previous request to create the named bucket succeeded and you already own it." {
			t.Errorf("Incorrect error message returned: %q", gErr.Message)
		}

		return
	}

	t.Errorf("Should throw an error when bucket name already exists")
}
