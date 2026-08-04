package com.bdd.portal.event;

import com.bdd.portal.entity.Execution;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class ExecutionCompletedEvent extends ApplicationEvent {
    private final Execution execution;

    public ExecutionCompletedEvent(Object source, Execution execution) {
        super(source);
        this.execution = execution;
    }
}
