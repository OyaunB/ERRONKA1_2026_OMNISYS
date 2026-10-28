package com.omnisys.pricechallenge;

public class Product {

    public final String name;
    public final String category;
    public final double price;
    public final String imageName;

    public Product(String name, String category,
                   double price, String imageName) {
        this.name = name;
        this.category = category;
        this.price = price;
        this.imageName = imageName;
    }
}