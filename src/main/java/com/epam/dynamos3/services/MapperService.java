package com.epam.dynamos3.services;

import com.epam.dynamos3.pojos.Product;

import java.util.List;

public interface MapperService {

    List<Product> mapperObjects(List<String> products_json);
}
