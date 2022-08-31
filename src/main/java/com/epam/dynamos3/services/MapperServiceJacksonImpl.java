package com.epam.dynamos3.services;

import com.epam.dynamos3.pojos.Product;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.stream.Collectors;

public class MapperServiceJacksonImpl implements MapperService{

    public List<Product> mapperObjects(List<String> products_json){
        ObjectMapper mapper = new ObjectMapper();
        return products_json.stream().map(p -> {
            try {
                return mapper.readValue(p, Product.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList());

    }
}
