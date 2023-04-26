<?php declare(strict_types=1);
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

use Google\Cloud\Core\Exception\GoogleException;
use Google\Cloud\Storage\Bucket;
use Google\Cloud\Storage\StorageClient;
use PHPUnit\Framework\TestCase;
require_once("StatusCode.php");
final class BucketTest extends TestCase
{
    private StorageClient $storage;
    private Bucket $bucket1;
    private Bucket $bucket2;
    private const INVALID_BUCKET_NAME = "***Bucket****";

    public function __construct(string $name)
    {
        parent::__construct($name);
        $this->storage = new StorageClient(['apiEndpoint' => "http://localhost:8080"]);
        $this->bucket1 = $this->storage->bucket("bucket1");
        $this->bucket2 = $this->storage->bucket("bucket2");
    }

    protected function setUp(): void
    {
        try { $this->bucket1->delete(); } catch (Exception) {}
    }

    /**
     * @throws GoogleException
     */
    public function testCanBeCreatedFromValidName(): void
    {
        $bucket = $this->storage->createBucket($this->bucket1->name());
        self::assertNotNull($bucket);
    }

    public function testCannotBeCreatedFromInvalidName(): void
    {
        $error = null;
        try {
            $this->storage->createBucket(BucketTest::INVALID_BUCKET_NAME);
        } catch (GoogleException $e) {
            $error = $e;
            self::assertEquals(StatusCode::BAD_REQUEST->value, $error->getCode());
            self::assertStringContainsString("Invalid bucket name ", $error->getMessage());
        }
        self::assertNotNull($error);
    }

    /**
     * @throws GoogleException
     */
    public function testCannotBeCreatedFromExistingBucketName(): void
    {
        $this->storage->createBucket($this->bucket1->name());
        $error = null;
        try {
            $this->storage->createBucket($this->bucket1->name());
        } catch (GoogleException $e) {
            $error = $e;
            self::assertEquals(StatusCode::CONFLICT->value, $error->getCode());
            self::assertStringContainsString("Your previous request to create the named bucket succeeded and you already own it.", $error->getMessage());
        }
        self::assertNotNull($error);
    }

    protected function tearDown(): void
    {
        try { $this->bucket1->delete(); } catch (Exception) {}
    }
}