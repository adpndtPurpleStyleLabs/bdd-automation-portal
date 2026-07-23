package com.bdd.portal.event;

import com.bdd.portal.entity.Execution;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class ExecutionCancelledEvent extends ApplicationEvent {
    private final Execution execution;

    public ExecutionCancelledEvent(Object source, Execution execution) {
        super(source);
        this.execution = execution;
    }
}
