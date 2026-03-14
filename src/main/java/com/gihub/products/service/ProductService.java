package com.gihub.products.service;


import com.gihub.products.dto.Product;
import com.gihub.products.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository repo;
    private final QueueMessagingTemplate queueMessagingTemplate;
    @Value("${cloud.aws.end-point}")
    private String endpoint;

    public ProductService(ProductRepository repo, QueueMessagingTemplate queueMessagingTemplate) {
        this.repo = repo;
        this.queueMessagingTemplate = queueMessagingTemplate;
    }

    public Product save(Product product) {
        queueMessagingTemplate.send(endpoint, MessageBuilder.withPayload(product.toString()).build());
        return repo.save(product);
    }

    public List<Product> findAll() {
        return repo.findAll();
    }
}