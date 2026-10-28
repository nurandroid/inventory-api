package org.example.inventory.service;

import org.springframework.stereotype.Service;

@Service
public class HelloService {

    public String getMessage() {
        return "Inventory API работает";
    }
}
