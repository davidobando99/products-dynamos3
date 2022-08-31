package com.epam.dynamos3.services;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.epam.dynamos3.config.AWSConfig;
import com.epam.dynamos3.pojos.Product;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class FileServiceS3Impl implements FileService{

    private static final AmazonS3 s3Client = new AWSConfig().getS3Client();
    private static final BuilderService builderService = new BuilderServiceHTMLImpl();
    private static final EventService eventService = new EventServiceDynamoImpl();
    private final String S3_BUCKET_NAME = "orders-lambda-html";
    private final String PRODUCTS_FILE_NAME = "index";

    public void processFinalFile(DynamodbEvent input, Context context){
        LambdaLogger LOGGER = context.getLogger();

        List<Product> products = eventService.createProducts(input,context);

        LOGGER.log("Connecting to S3 Bucket");

        if(!doesLogFileExists()){
            LOGGER.log("Creating new index.html file with new product");
            InputStream inputProduct = new ByteArrayInputStream(builderService.createHtml(products).getBytes());
            writeStreamToS3File(inputProduct);
        }else{
            createAndUpdateS3(products,context,eventService.isUpdate(input));
        }

    }

    private void createAndUpdateS3(List<Product> products, Context context, boolean isUpdate){
        LambdaLogger LOGGER = context.getLogger();

        try {
            S3Object s3object = s3Client.getObject(S3_BUCKET_NAME, PRODUCTS_FILE_NAME);
            S3ObjectInputStream inputStream = s3object.getObjectContent();
            if(isUpdate) {
                LOGGER.log("Appending new product to index.html file");
                InputStream finalStreamHtml = new ByteArrayInputStream(builderService.appendToHtml(inputStream,products.get(0)).getBytes());
                writeStreamToS3File(finalStreamHtml);
            }else{
                LOGGER.log("Updating product in index.html file");
                InputStream finalStreamHtml = new ByteArrayInputStream(builderService.editHtml(inputStream,products.get(0)).getBytes());
                writeStreamToS3File(finalStreamHtml);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private void writeStreamToS3File(InputStream stream) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType("text/html");
        s3Client.putObject(S3_BUCKET_NAME, PRODUCTS_FILE_NAME, stream, metadata);

    }

    private boolean doesLogFileExists() {
        return s3Client.doesObjectExist(S3_BUCKET_NAME, PRODUCTS_FILE_NAME);
    }
}
