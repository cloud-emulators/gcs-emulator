#include "google/cloud/storage/client.h"
#include <iostream>
using ::google::cloud::StatusOr;
int main(int argc, char* argv[]) {
    std::string const bucket_name = "bucket1";

    // Create aliases to make the code easier to read.
    namespace gc = ::google::cloud;
    namespace gcs = gc::storage;

    // Create a client to communicate with Google Cloud Storage. This client
    // uses the default configuration for authentication and project id.
    StatusOr<gcs::ClientOptions> options =
            gcs::ClientOptions::CreateDefaultClientOptions();
    if (!options) throw std::runtime_error(options.status().message());

    options->set_endpoint("http://localhost:8080/");
    gcs::Client client(*options);

    client.DeleteBucket(bucket_name);
    auto* metadata = new gcs::BucketMetadata();
    auto bucket_metadata = client.CreateBucket(bucket_name, *metadata);
    if (!bucket_metadata) throw std::move(bucket_metadata).status(); // NOLINT(hicpp-exception-baseclass)
    std::cout << "The Bucket " << bucket_metadata->name()
              << "\nFull metadata: " << *bucket_metadata << "\n";
//    auto writer = client.WriteObject(bucket_name, "quickstart.txt");
//    writer << "Hello World!";
//    writer.Close();
//    if (writer.metadata()) {
//        std::cout << "Successfully created object: " << *writer.metadata() << "\n";
//    } else {
//        std::cerr << "Error creating object: " << writer.metadata().status()
//                  << "\n";
//        return 1;
//    }
//
//    auto reader = client.ReadObject(bucket_name, "quickstart.txt");
//    if (!reader) {
//        std::cerr << "Error reading object: " << reader.status() << "\n";
//        return 1;
//    }
//
//    std::string contents{std::istreambuf_iterator<char>{reader}, {}};
//    std::cout << contents << "\n";

    return 0;
}