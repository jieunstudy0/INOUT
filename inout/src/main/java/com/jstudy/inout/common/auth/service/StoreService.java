package com.jstudy.inout.common.auth.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.jstudy.inout.common.auth.repository.StoreRepository;
import com.jstudy.inout.common.config.CacheConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StoreService {

    private final StoreRepository storeRepository;

    @Cacheable(value = CacheConfig.STORE_LIST, key = "'all'")
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getPublicStoreList() {
        return storeRepository.findAll().stream()
                .map(s -> Map.<String, Object>of("id", s.getId(), "name", s.getName()))
                .collect(Collectors.toList());
    }
}
