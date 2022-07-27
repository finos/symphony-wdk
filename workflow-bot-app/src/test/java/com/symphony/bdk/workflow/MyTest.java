package com.symphony.bdk.workflow;

import com.symphony.bdk.workflow.swadl.SwadlParser;
import com.symphony.bdk.workflow.swadl.v1.Workflow;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class MyTest extends IntegrationTest {

  @Test
  void loopOverridesOutputsTest() throws IOException, ProcessingException {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/complex/test.swadl.yaml"));
    engine.deploy(workflow);
  }

}
