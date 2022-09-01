package com.epam.dynamos3.services;

import com.epam.dynamos3.pojos.Product;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static j2html.TagCreator.*;
import static j2html.TagCreator.td;

public class HTMLBuilderService implements BuilderService{

    public String createHtml(List<Product> products){
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

    public String appendToHtml(InputStream htmlFile, Product newProduct) throws IOException {

        Document doc = Jsoup.parse(htmlFile,"UTF-8","");
        doc.select("tbody").append(
                tr(
                        td(newProduct.getName()),
                        td(newProduct.getPrice()),
                        td(newProduct.getCategory())

                ).withId(newProduct.getId()).render()
        );
        return doc.html();

    }

    public String editHtml(InputStream htmlFile, Product updatedProduct) throws IOException {


        Document doc = Jsoup.parse(htmlFile,"UTF-8","");
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
