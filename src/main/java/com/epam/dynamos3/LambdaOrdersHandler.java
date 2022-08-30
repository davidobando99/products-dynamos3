package com.epam.dynamos3;

import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemUtils;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.StreamsEventResponse;
import com.amazonaws.services.lambda.runtime.events.transformers.v1.DynamodbEventTransformer;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.epam.dynamos3.pojos.Product;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.util.List;
import java.util.stream.Collectors;

import static j2html.TagCreator.*;

public class LambdaOrdersHandler implements RequestHandler<DynamodbEvent, Serializable> {


    private final String S3_BUCKET_NAME = "orders-lambda-html";
    private final String PRODUCTS_FILE_NAME = "index";

    @Override
    public StreamsEventResponse handleRequest(DynamodbEvent input, Context context) {

        LambdaLogger LOGGER = context.getLogger();

        List<Product> products = createProducts(input,context);

        LOGGER.log("Connecting to S3 Bucket");

        AmazonS3 s3Client = getS3Client();


        if(!doesLogFileExists(s3Client)){
            LOGGER.log("Creating new index.html file with new product");
            InputStream inputProduct = new ByteArrayInputStream(createHtml(products).getBytes());
            writeStreamToS3File(inputProduct, s3Client);
        }else{
            createAndUpdateS3(s3Client,products,context,input);
        }

        return new StreamsEventResponse();
    }

    private AmazonS3 getS3Client(){
        return AmazonS3ClientBuilder.standard()
                .build();
    }

    private boolean isUpdate(DynamodbEvent input){
        return DynamodbEventTransformer.toRecordsV1(input)
                .stream()
                .anyMatch(record ->record.getEventName().equals("MODIFY"));
    }

    private List<Product> createProducts(DynamodbEvent input, Context context){
        LambdaLogger LOGGER = context.getLogger();

        List<Item> newItems = geImagesFromEvent(input, true);
        LOGGER.log("Getting new images from event records");
        List<String> new_products_json = newItems.stream().map(Item::toJSON).collect(Collectors.toList());
        List<Product> new_products = mapperObjects(new_products_json);
        LOGGER.log("New Product "+new_products);
        return new_products;


    }

    private List<Item> geImagesFromEvent(DynamodbEvent input, boolean isNewImage){
        return DynamodbEventTransformer.toRecordsV1(input)
                .stream()
                .map(record -> ItemUtils.toItem(isNewImage ? record.getDynamodb().getNewImage(): record.getDynamodb().getOldImage()))
                .collect(Collectors.toList());
    }

    private boolean doesLogFileExists(AmazonS3 s3Client) {
        return s3Client.doesObjectExist(S3_BUCKET_NAME, PRODUCTS_FILE_NAME);
    }

    private List<Product> mapperObjects(List<String> products_json){
        ObjectMapper mapper = new ObjectMapper();
        return products_json.stream().map(p -> {
            try {
                return mapper.readValue(p, Product.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList());

    }

    private void createAndUpdateS3(AmazonS3 s3Client, List<Product> products, Context context, DynamodbEvent input){
        LambdaLogger LOGGER = context.getLogger();

        try {
            if(!isUpdate(input)) {
                LOGGER.log("Appending new product to index.html file");
                InputStream finalStreamHtml = new ByteArrayInputStream(appendToHtml(s3Client,products.get(0)).getBytes());
                writeStreamToS3File(finalStreamHtml, s3Client);
            }else{
                LOGGER.log("Updating product in index.html file");
                InputStream finalStreamHtml = new ByteArrayInputStream(editHtml(s3Client,products.get(0)).getBytes());
                writeStreamToS3File(finalStreamHtml, s3Client);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private void writeStreamToS3File(InputStream stream, AmazonS3 s3Client) {
        String result = "";

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType("text/html");
        s3Client.putObject(S3_BUCKET_NAME, PRODUCTS_FILE_NAME, stream, metadata);

    }

    private String createHtml(List<Product> products){
        return html(
                style("table, th, td { border: 1px solid black; }"),
                head(
                        title("Products")
                ),
                h2("Products"),
                table(
                        tbody(
                                tr(
                                        td("Product Name"),
                                        td("Product Price"),
                                        td("Product Category")
                                ),
                                each(products, product -> tr(
                                       td(
                                               String.valueOf(product.getName())
                                        ),
                                        td(
                                                String.valueOf(product.getPrice())
                                        ),
                                        td(
                                                String.valueOf(product.getCategory()))
                                        ).withId(product.getId()))
                        )

                ).withId("table_products")
        ).render();

    }

    private String appendToHtml(AmazonS3 s3Client, Product newProduct) throws IOException {
        S3Object s3object = s3Client.getObject(S3_BUCKET_NAME, PRODUCTS_FILE_NAME);
        S3ObjectInputStream inputStream = s3object.getObjectContent();

        Document doc = Jsoup.parse(inputStream,"UTF-8","");
        doc.select("tbody").append(
                tr(
                        td(newProduct.getName()),
                        td(newProduct.getPrice()),
                        td(newProduct.getCategory())

                ).withId(newProduct.getId()).render()
        );
        return doc.html();

    }

    private String editHtml(AmazonS3 s3Client, Product updatedProduct) throws IOException {
        S3Object s3object = s3Client.getObject(S3_BUCKET_NAME, PRODUCTS_FILE_NAME);
        S3ObjectInputStream inputStream = s3object.getObjectContent();

        Document doc = Jsoup.parse(inputStream,"UTF-8","");
        Elements product_tr = doc.select("tr#"+updatedProduct.getId());
        for(Element product:product_tr) {
            Elements product_tds = product.select("td");

            product_tds.get(0).text(updatedProduct.getName());
            product_tds.get(1).text(updatedProduct.getPrice());
            product_tds.get(2).text(updatedProduct.getCategory());
        }
        return doc.html();

    }




}
