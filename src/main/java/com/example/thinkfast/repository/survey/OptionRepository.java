package com.example.thinkfast.repository.survey;

import com.example.thinkfast.domain.survey.Option;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OptionRepository extends JpaRepository<Option, Long> {
    // - save(Option entity)
    // - findById(Long id)
    // - findAll()
    // - deleteById(Long id)
    // - delete(Option entity)
    // - count()
    // - existsById(Long id)
} 