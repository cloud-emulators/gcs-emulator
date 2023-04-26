using System.Net;
using Google;
using Google.Apis.Auth.OAuth2;

namespace csharp_client;

using Google.Apis.Storage.v1.Data;
using Google.Cloud.Storage.V1;

public class Tests
{
    private const string ProjectId = "idyllic-slice-381317";
    private static readonly Bucket Bucket1 = new() { Name = "gcs-testing-bucket1234",  };
    // private static readonly Bucket Bucket1 = new() { Name = "********************bucket1*********************8",  };
    // private static readonly Bucket Bucket2 = new() { Name = "bucket2" };
    // private const string Location = "us";

    [SetUp]
    public void Setup()
    {
        
    }

    [Test]
    [TestCase(8080)]
    public void TestCreateListBucket(int port)
    {
        // var storage = new StorageClientBuilder
        // {
        //     BaseUri = $"http://localhost:{port}/storage/v1/"
        // }.Build();

        var storage = StorageClient.Create();
        // try { storage.DeleteBucket(Bucket1); } catch { /* ignored */ }

        // using var enumerator = storage.ListBuckets(ProjectId).GetEnumerator();
        // Assert.That(enumerator.MoveNext(), Is.False);
        var bucket = storage.GetBucket( Bucket1.Name);
        Assert.That(bucket, Is.Not.Null);

        GoogleApiException? exception = null;
        try
        {
            storage.CreateBucket(ProjectId, Bucket1);
        }
        catch(GoogleApiException err)
        {
            Assert.Multiple(() =>
            {
                Assert.That(err.HttpStatusCode, Is.EqualTo(HttpStatusCode.Conflict));
                Assert.That(err.Error.Code, Is.EqualTo((int)HttpStatusCode.Conflict));
                Assert.That(err.Error.Message, Does.StartWith("The requested bucket name is not available"));
            });
            exception = err;
        }

        Assert.That(exception, Is.Not.Null);
    }
}