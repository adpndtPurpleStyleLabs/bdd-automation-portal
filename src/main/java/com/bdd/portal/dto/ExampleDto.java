package com.bdd.portal.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExampleDto {
    private String name;
    private List<String> headers;
    private List<List<String>> rows;
}
