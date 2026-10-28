package com.omnisys.pricechallenge;

import java.util.ArrayList;
import java.util.List;

public class ProductRepository {
    // Hasierako demo-katalogoa. Hurrengo fasean Odoo-ko produktuekin sinkronizatuko da.
    public static List<Product> getProducts() {
        List<Product> products = new ArrayList<>();
        products.add(new Product("Pantaila 4K Toshiba", "Pantailak", 831.00, "monitor"));
        products.add(new Product("MSI Sword 16 HX B14V", "Ordenagailuak", 1295.00, "laptop"));
        products.add(new Product("MSI GM41 sagua", "Osagarriak", 49.90, "mouse"));
        products.add(new Product("Teklatu mekanikoa", "Osagarriak", 89.95, "keyboard"));
        products.add(new Product("USB-C dockinga", "Osagarriak", 119.00, "dock"));
        products.add(new Product("Haririk gabeko entzungailuak", "Audioa", 74.50, "headphones"));
        products.add(new Product("Wi-Fi bideratzailea", "Sarea", 69.99, "dock"));
        products.add(new Product("1 TB SSD diskoa", "Biltegiratzea", 89.90, "dock"));
        products.add(new Product("Web kamera HD", "Osagarriak", 45.90, "monitor"));
        products.add(new Product("Mahaigaineko ordenagailua", "Ordenagailuak", 849.00, "laptop"));
        products.add(new Product("USB hub-a", "Osagarriak", 29.99, "dock"));
        products.add(new Product("Ordenagailu eramangarriaren euskarria", "Osagarriak", 39.50, "keyboard"));
        return products;
    }
}
