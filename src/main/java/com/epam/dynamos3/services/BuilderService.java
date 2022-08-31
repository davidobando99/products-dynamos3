package com.epam.dynamos3.services;

import com.epam.dynamos3.pojos.Product;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface BuilderService {

    String appendToHtml(InputStream htmlFile, Product newProduct) throws IOException;

    String editHtml(InputStream htmlFile, Product newProduct) throws IOException;

    String createHtml(List<Product> products);

}
