package com.hmdp.service;

import reactor.core.publisher.Flux;

public interface AIService {

    Flux<String> chat(String message, String sessionId);

}
