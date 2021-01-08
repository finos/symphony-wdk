package com.symphony.bdk.workflow.swadl;


import lombok.Data;

@Data
public class Activity {
    private Event on;
    private String reply;
    private String script;
}
