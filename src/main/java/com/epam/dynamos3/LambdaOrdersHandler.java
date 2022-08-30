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

        List<Item> items = DynamodbEventTransformer.toRecordsV1(input)
                .stream()
                .map(record -> ItemUtils.toItem(record.getDynamodb().getNewImage()))
                .collect(Collectors.toList());

        LOGGER.log("Getting event records");

        List<String> products_json = items.stream().map(Item::toJSON).collect(Collectors.toList());

        List<Product> products = mapperObjects(products_json);

        LOGGER.log("New Product "+products);

        LOGGER.log("Connecting to S3 Bucket");

        AmazonS3 s3Client = getS3Client();

        InputStream inputProduct = new ByteArrayInputStream(createHtml(products).getBytes());

        createAndUpdateS3(s3Client,inputProduct, products,context);

        return new StreamsEventResponse();
    }

    private AmazonS3 getS3Client(){
        return AmazonS3ClientBuilder.standard()
                .build();
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

    private void createAndUpdateS3(AmazonS3 s3Client, InputStream inputProduct, List<Product> products, Context context){
        LambdaLogger LOGGER = context.getLogger();

        if(!doesLogFileExists(s3Client)){
            LOGGER.log("Creating new index.html file with new product");
            writeStreamToS3File(inputProduct, s3Client);
        }else{
            try {
                LOGGER.log("Appending new product to index.html file");
                InputStream finalStreamHtml = new ByteArrayInputStream(appendToHtml(s3Client,products.get(0)).getBytes());
                writeStreamToS3File(finalStreamHtml, s3Client);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
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
                                        ))
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

                ).render()
        );
        return doc.html();

    }



}
