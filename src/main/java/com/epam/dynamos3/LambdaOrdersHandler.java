package com.epam.dynamos3;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.StreamsEventResponse;
import com.epam.dynamos3.services.FileService;
import com.epam.dynamos3.services.S3FileService;

import java.io.*;

public class LambdaOrdersHandler implements RequestHandler<DynamodbEvent, Serializable> {

    @Override
    public StreamsEventResponse handleRequest(DynamodbEvent input, Context context) {

        LambdaLogger LOGGER = context.getLogger();

        FileService fileService = new S3FileService();

        fileService.processFinalFile(input,context);

        return new StreamsEventResponse();
    }


















}
