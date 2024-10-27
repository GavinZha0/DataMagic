package com.ninestar.datapie.datamagic.utils;

import cn.hutool.core.util.StrUtil;
import io.minio.*;
import io.minio.errors.*;
import io.minio.messages.Bucket;
import io.minio.messages.Item;
import org.springframework.stereotype.Component;
import io.minio.http.Method;
import javax.annotation.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

@Component
public class MinioUtil {

    @Resource
    private MinioClient sysMinioClient;

    private MinioClient userMinioClient;

    public Boolean connect(String url, String accessKey, String secretKey) {
        String[] segs = url.split("/");
        userMinioClient = MinioClient.builder()
                .endpoint(url)
                .credentials(accessKey, secretKey)
                .build();

        if(userMinioClient != null){
            return true;
        } else {
            return false;
        }
    }

    public void disconnect(){
        userMinioClient = null;
    }

    /**
     * 创建一个桶
     */
    public Boolean createBucket(String bucket) throws Exception {
        MinioClient minioClient = null;
        if(userMinioClient != null){
            minioClient = userMinioClient;
        } else if(sysMinioClient != null){
            minioClient = sysMinioClient;
        } else {
            return false;
        }

        boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!found) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }
        return true;
    }

    public Boolean existsBucket(String bucketName) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        MinioClient minioClient = null;
        if(userMinioClient != null){
            minioClient = userMinioClient;
        } else if(sysMinioClient != null){
            minioClient = sysMinioClient;
        } else {
            return false;
        }

        return minioClient.bucketExists(BucketExistsArgs.builder()
                .bucket(bucketName)
                .build());
    }

    /**
     * 上传一个文件
     */
    public Boolean uploadFile(InputStream stream, String bucket, String objectName) throws Exception {
        MinioClient minioClient = null;
        if(userMinioClient != null){
            minioClient = userMinioClient;
        } else if(sysMinioClient != null){
            minioClient = sysMinioClient;
        } else {
            return false;
        }

        minioClient.putObject(PutObjectArgs.builder().bucket(bucket).object(objectName)
                .stream(stream, -1, 10485760).build());
        return true;
    }

    /**
     * 列出所有的桶
     */
    public List<String> listBuckets() throws Exception {
        MinioClient minioClient = null;
        if(userMinioClient != null){
            minioClient = userMinioClient;
        } else if(sysMinioClient != null){
            minioClient = sysMinioClient;
        } else {
            return null;
        }

        List<Bucket> list = minioClient.listBuckets();
        List<String> names = new ArrayList<>();
        list.forEach(b -> {
            names.add(b.name());
        });
        return names;
    }

    /**
     * 列出一个桶中的所有文件和目录
     */
    public List<FileInfo> listFiles(String bucket, String folder) throws Exception {
        MinioClient minioClient = null;
        if(userMinioClient != null){
            minioClient = userMinioClient;
        } else if(sysMinioClient != null){
            minioClient = sysMinioClient;
        } else {
            return null;
        }

        Iterable<Result<Item>> results = null;
        if(StrUtil.isEmpty(folder)){
            results = minioClient.listObjects(
                    ListObjectsArgs.builder().bucket(bucket).recursive(true).build());
        } else {
            results = minioClient.listObjects(
                    ListObjectsArgs.builder().bucket(bucket).prefix(folder).recursive(true).build());
        }


        List<FileInfo> infos = new ArrayList<>();
        results.forEach(r->{
            FileInfo info = new FileInfo();
            try {
                Item item = r.get();
                info.setFilename(item.objectName());
                info.setDirectory(item.isDir() || item.objectName().endsWith("/"));
                infos.add(info);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        return infos;
    }

    /**
     * 下载一个文件
     */
    public InputStream download(String bucket, String objectName) throws Exception {
        MinioClient minioClient = null;
        if(userMinioClient != null){
            minioClient = userMinioClient;
        } else if(sysMinioClient != null){
            minioClient = sysMinioClient;
        } else {
            return null;
        }

        InputStream stream = minioClient.getObject(
                GetObjectArgs.builder().bucket(bucket).object(objectName).build());
        return stream;
    }

    /**
     * 删除一个桶
     */
    public void deleteBucket(String bucket) throws Exception {
        MinioClient minioClient = null;
        if(userMinioClient != null){
            minioClient = userMinioClient;
        } else if(sysMinioClient != null){
            minioClient = sysMinioClient;
        } else {
            return;
        }

        minioClient.removeBucket(RemoveBucketArgs.builder().bucket(bucket).build());
    }

    /**
     * 删除一个对象
     */
    public void deleteObject(String bucket, String objectName) throws Exception {
        MinioClient minioClient = null;
        if(userMinioClient != null){
            minioClient = userMinioClient;
        } else if(sysMinioClient != null){
            minioClient = sysMinioClient;
        } else {
            return;
        }

        minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(objectName).build());
    }


    /**
     * 复制文件
     *
     * @Param: [sourceBucket, sourceObject, targetBucket, targetObject]
     * @return: void
     * @Author: MrFugui
     * @Date: 2021/11/15
     */
    public void copyObject(String sourceBucket, String sourceObject, String targetBucket, String targetObject) throws Exception {
        MinioClient minioClient = null;
        if(userMinioClient != null){
            minioClient = userMinioClient;
        } else if(sysMinioClient != null){
            minioClient = sysMinioClient;
        } else {
            return;
        }

        this.createBucket(targetBucket);
        minioClient.copyObject(CopyObjectArgs.builder().bucket(targetBucket).object(targetObject)
                .source(CopySource.builder().bucket(sourceBucket).object(sourceObject).build()).build());
    }

    /**
     * 获取文件信息
     *
     * @Param: [bucket, objectName]
     * @return: java.lang.String
     * @Author: MrFugui
     * @Date: 2021/11/15
     */
    public String getObjectInfo(String bucket, String objectName) throws Exception {
        MinioClient minioClient = null;
        if(userMinioClient != null){
            minioClient = userMinioClient;
        } else if(sysMinioClient != null){
            minioClient = sysMinioClient;
        } else {
            return null;
        }

        return minioClient.statObject(StatObjectArgs.builder().bucket(bucket).object(objectName).build()).toString();

    }

    /**
     * 生成一个给HTTP GET请求用的presigned URL。浏览器/移动端的客户端可以用这个URL进行下载，即使其所在的存储桶是私有的。
     *
     * @Param: [bucketName, objectName, expires]
     * @return: java.lang.String
     * @Author: MrFugui
     * @Date: 2021/11/15
     */
    public String getPresignedObjectUrl(String bucketName, String objectName, Integer expires) throws Exception {
        MinioClient minioClient = null;
        if(userMinioClient != null){
            minioClient = userMinioClient;
        } else if(sysMinioClient != null){
            minioClient = sysMinioClient;
        } else {
            return null;
        }

        GetPresignedObjectUrlArgs build = GetPresignedObjectUrlArgs
                .builder().bucket(bucketName).object(objectName).expiry(expires).method(Method.GET).build();
        return minioClient.getPresignedObjectUrl(build);
    }

    /**
     * 获取minio中所有的文件
     *
     * @Param: []
     * @return: java.util.List<boot.spring.domain.Fileinfo>
     * @Author: MrFugui
     * @Date: 2021/11/15
     */
    public List<FileInfo> listAllFile() throws Exception {
        List<String> list = this.listBuckets();
        List<FileInfo> fileinfos = new ArrayList<>();
        for (String bucketName : list) {
            fileinfos.addAll(this.listFiles(bucketName, null));
        }


        return fileinfos;
    }
}
