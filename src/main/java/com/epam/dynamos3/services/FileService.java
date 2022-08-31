package com.epam.dynamos3.services;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.epam.dynamos3.pojos.Product;

import java.io.InputStream;
import java.util.List;

public interface FileService {

    void processFinalFile(DynamodbEvent input, Context context);
}
