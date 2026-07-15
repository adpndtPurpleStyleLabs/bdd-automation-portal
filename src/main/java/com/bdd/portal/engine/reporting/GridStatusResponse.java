package com.bdd.portal.engine.reporting;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GridStatusResponse {
    private GridValue value;
}

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
class GridValue {
    private boolean ready;
    private List<GridNode> nodes;
}

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
class GridNode {
    private String id;
    private String uri;
    private int maxSessions;
    private List<GridSlot> slots;
}

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
class GridSlot {
    private GridStereotype stereotype;
    private Object session; // If null, the slot is free
}

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
class GridStereotype {
    private String browserName;
}
