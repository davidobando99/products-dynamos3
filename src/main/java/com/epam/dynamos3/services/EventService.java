package com.epam.dynamos3.services;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.epam.dynamos3.pojos.Product;

import java.util.List;

public interface EventService {

    boolean isUpdate(DynamodbEvent input);

    List<Product> createProducts(DynamodbEvent input, Context context);

}
