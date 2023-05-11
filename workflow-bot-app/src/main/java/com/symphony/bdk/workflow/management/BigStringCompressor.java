package com.symphony.bdk.workflow.management;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter
public class BigStringCompressor implements AttributeConverter<String, byte[]> {
  @Override
  public byte[] convertToDatabaseColumn(String attribute) {
    try {
      return compress(attribute);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static byte[] compress(String attribute) throws IOException {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    GZIPOutputStream gzipOutputStream = new GZIPOutputStream(outputStream);
    gzipOutputStream.write(attribute.getBytes(StandardCharsets.UTF_8));
    gzipOutputStream.close();
    return outputStream.toByteArray();
  }

  @Override
  public String convertToEntityAttribute(byte[] dbData) {
    try {
      return decompress(dbData);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static String decompress(byte[] dbData) throws IOException {
    ByteArrayInputStream inputStream = new ByteArrayInputStream(dbData);
    GZIPInputStream gzipInputStream = new GZIPInputStream(inputStream);
    byte[] buffer = new byte[100];
    int length;
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    while ((length = gzipInputStream.read(buffer)) > 0) {
      outputStream.write(buffer, 0, length);
    }
    gzipInputStream.close();
    outputStream.close();
    return outputStream.toString(StandardCharsets.UTF_8);
  }
}
