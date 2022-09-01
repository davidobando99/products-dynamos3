package com.epam.dynamos3.services;

import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemUtils;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.transformers.v1.DynamodbEventTransformer;
import com.epam.dynamos3.pojos.Product;

import java.util.List;
import java.util.stream.Collectors;

public class DynamoEventService implements EventService{

    public static MapperService mapperService= new JacksonMapperService();

    public boolean isUpdate(DynamodbEvent input){
        return DynamodbEventTransformer.toRecordsV1(input)
                .stream()
                .anyMatch(record ->record.getEventName().equals("MODIFY"));
    }

    public List<Product> createProducts(DynamodbEvent input, Context context){
        LambdaLogger LOGGER = context.getLogger();

        List<Item> newItems = geImagesFromEvent(input);
        LOGGER.log("Getting new images from event records");
        List<String> new_products_json = newItems.stream().map(Item::toJSON).collect(Collectors.toList());
        List<Product> new_products = mapperService.mapperObjects(new_products_json);
        LOGGER.log("New Product "+new_products);
        return new_products;


    }

    private List<Item> geImagesFromEvent(DynamodbEvent input){
        return DynamodbEventTransformer.toRecordsV1(input)
                .stream()
                .map(record -> ItemUtils.toItem(record.getDynamodb().getNewImage()))
                .collect(Collectors.toList());
    }
}
