package com.symphony.bdk.workflow.swadl;


import lombok.Data;

import java.util.List;

@Data
public class Workflow {
    private String name;
    private List<Activity> activities;
}
