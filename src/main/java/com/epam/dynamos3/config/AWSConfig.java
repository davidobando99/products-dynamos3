package com.epam.dynamos3.config;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

public class AWSConfig {

    public AmazonS3 getS3Client(){
        return AmazonS3ClientBuilder.standard()
                .build();
    }
}
