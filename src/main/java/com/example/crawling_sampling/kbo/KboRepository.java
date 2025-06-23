package com.example.crawling_sampling.kbo;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@RequiredArgsConstructor
@Repository
public class KboRepository {

    private final EntityManager em;

}
