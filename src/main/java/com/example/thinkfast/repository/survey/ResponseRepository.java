package com.example.thinkfast.repository.survey;

import com.example.thinkfast.domain.survey.Response;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResponseRepository extends JpaRepository<Response, Long> {
}
