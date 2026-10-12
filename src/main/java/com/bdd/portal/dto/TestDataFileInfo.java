package com.bdd.portal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestDataFileInfo {
    private String filename;
    private String displayName;
    private long sizeBytes;
    private String lastModified;
    private int recordCount;
    private String recordLabel;
    private boolean coreFile;
}
