package ru.learn.datalake;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/** Тонкая обёртка над S3. ETag используется как непрозрачная версия объекта. */
final class LakeStorage {
    private final S3Client s3;
    private final String bucket;

    LakeStorage(S3Client s3, String bucket) {
        this.s3 = s3;
        this.bucket = bucket;
    }

    record ObjectVersion(byte[] bytes, String etag) {
    }

    Optional<ObjectVersion> find(String key) {
        try {
            // Тело и ETag читаются одним GET, чтобы версия относилась именно к этим данным.
            var object = s3.getObjectAsBytes(request -> request.bucket(bucket).key(key));
            return Optional.of(new ObjectVersion(object.asByteArray(), object.response().eTag()));
        } catch (S3Exception error) {
            if (error.statusCode() == 404) {
                return Optional.empty();
            }
            throw error;
        }
    }

    boolean compareAndSet(String key, byte[] value, String previousEtag) {
        var request = PutObjectRequest.builder().bucket(bucket).key(key);
        if (previousEtag == null) {
            request.ifNoneMatch("*");
        } else {
            request.ifMatch(previousEtag);
        }
        try {
            s3.putObject(request.build(), RequestBody.fromBytes(value));
            return true;
        } catch (S3Exception error) {
            // При конфликте передаём решение вызывающему. Ошибки связи не превращаем в «не опубликовано».
            if (error.statusCode() == 412 || error.statusCode() == 409) {
                return false;
            }
            throw error;
        }
    }

    List<String> keys(String prefix) {
        return s3.listObjectsV2Paginator(request -> request.bucket(bucket).prefix(prefix)).contents().stream()
                .map(S3Object::key).sorted().toList();
    }

    void download(String key, Path file) {
        s3.getObject(request -> request.bucket(bucket).key(key), ResponseTransformer.toFile(file));
    }

    void upload(String key, Path file) {
        s3.putObject(request -> request.bucket(bucket).key(key), RequestBody.fromFile(file));
    }
}
