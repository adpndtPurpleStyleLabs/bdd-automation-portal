package com.bdd.portal.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StepDto {
    private String keyword;
    private String text;
    private List<List<String>> dataTable;
    private String docString;
}
