package com.bdd.portal.event;

import com.bdd.portal.entity.Execution;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class ExecutionStartedEvent extends ApplicationEvent {
    private final Execution execution;

    public ExecutionStartedEvent(Object source, Execution execution) {
        super(source);
        this.execution = execution;
    }
}
