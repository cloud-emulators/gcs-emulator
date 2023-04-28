# frozen_string_literal: true

# Copyright (c) 2023 Mahmoud Bahaa and others
#
# Permission is hereby granted, free of charge, to any person obtaining
# a copy of this software and associated documentation files (the
# "Software"), to deal in the Software without restriction, including
# without limitation the rights to use, copy, modify, merge, publish,
# distribute, sublicense, and/or sell copies of the Software, and to
# permit persons to whom the Software is furnished to do so, subject to
# the following conditions:
#
# The above copyright notice and this permission notice shall be
# included in all copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
# EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
# MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
# NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
# LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
# OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
# WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

require 'minitest/autorun'
require 'google/cloud/storage'
require 'google/cloud/errors'
# require 'net/http/status'

# Testing Buckets
class TestBuckets < Minitest::Test
  BUCKET1 = "bucket1"
  BUCKET2 = "bucket2"
  INVALID_BUCKET_NAME = "a****bucket****a"

  def setup
    @storage = Google::Cloud::Storage.new endpoint: "http://localhost:8080/"
    bucket1 = @storage.bucket(BUCKET1)
    if bucket1 != nil
      bucket1.delete
    end
  end

  def test_bucket_created
    bucket = @storage.create_bucket BUCKET1
    refute_nil bucket
  end

  def test_bucket_invalid_name
    begin
      @storage.create_bucket INVALID_BUCKET_NAME
    rescue Google::Cloud::Error => e
      assert e.instance_of? Google::Cloud::InvalidArgumentError
      assert e.message.include? "Invalid bucket name"
    end
  end

  def test_bucket_already_exists
    bucket = @storage.create_bucket BUCKET1
    refute_nil bucket
    begin
      @storage.create_bucket BUCKET1
    rescue Google::Cloud::Error => e
      assert e.instance_of? Google::Cloud::AlreadyExistsError
      assert_equal "conflict: Your previous request to create the named bucket succeeded and you already own it.", e.message
    end
  end

end
