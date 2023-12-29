package com.symphony.bdk.workflow.swadl;

import com.symphony.bdk.workflow.management.BigStringCompressor;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class CompressionTest {

  @Test
  void compressString() throws IOException {
    String inputString = "id: my-workflow\n"
        + "\n"
        + "activities:\n"
        + "  - send-message:\n"
        + "      id: counter\n"
        + "      on:\n"
        + "        message-received:\n"
        + "          content: /count\n"
        + "      content: \"version1\"\n"
        + "  - execute-script:\n"
        + "      id: vars\n"
        + "      script: |\n"
        + "        counter = wdk.readShared('test', 'counter')\n"
        + "        counter++\n"
        + "        wdk.writeShared('test', 'counter', counter)\n"
        + "  - send-message:\n"
        + "      id: send_counter\n"
        + "      content: ${readShared('test', 'counter')}\n";

    BigStringCompressor compressor = new BigStringCompressor();
    byte[] converted = compressor.convertToDatabaseColumn(inputString);
    assertThat(converted.length).isLessThan(inputString.length());

    String attribute = compressor.convertToEntityAttribute(converted);
    assertThat(attribute).isEqualTo(inputString);
  }
}
