package com.example.thinkfast.dto.survey;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaginationDto {
    private int currentPage;
    private int pageSize;
    private int totalPages;
    private long totalCount;
}

