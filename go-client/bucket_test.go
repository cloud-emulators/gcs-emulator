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
	"context"
	"google.golang.org/api/option"
	"testing"
	"time"

	"cloud.google.com/go/storage"
)

var (
	projectId = "project-id"
	BUCKET1   = "bucket1"
	//BUCKET2   = "bucket2"
)

//func tempDir() string {
//	if //goland:noinspection GoBoolExpressions
//	runtime.GOOS == "linux" {
//		return "/var/tmp"
//	} else {
//		return os.TempDir()
//	}
//}

func TestServerClientBucketAlreadyExists(t *testing.T) {
	ctx := context.Background()
	endpoint := option.WithEndpoint("http://localhost:8080/storage/v1/")
	client, err := storage.NewClient(ctx, endpoint)
	if err != nil {
		t.Errorf("storage.NewClient: %v", err)
	}
	defer func(client *storage.Client) {
		err := client.Close()
		if err != nil {
			t.Errorf("Error while closing client: %v", err)
		}
	}(client)

	ctx, cancel := context.WithTimeout(ctx, time.Second*30)
	defer cancel()

	bucket := client.Bucket(BUCKET1)
	_ = bucket.Delete(ctx)
	if err := bucket.Create(ctx, projectId, &storage.BucketAttrs{}); err != nil {
		t.Errorf("Bucket(%q).Create: %v", BUCKET1, err)
	}

	t.Logf("Created bucket %v.\n", BUCKET1)
}
