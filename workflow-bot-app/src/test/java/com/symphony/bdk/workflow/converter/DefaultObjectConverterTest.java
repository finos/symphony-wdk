package com.symphony.bdk.workflow.converter;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.List;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DefaultObjectConverterTest {

  @Mock
  Converter<Source, Target> converter;
  @Mock
  private Source source;
  @Mock
  private Target target;
  private ObjectConverter objectConverter;

  @BeforeEach
  void setUp() {
    when(converter.getSourceClass()).thenReturn(Source.class);
    when(converter.getTargetClass()).thenReturn(Target.class);
    objectConverter = new DefaultObjectConverter(Collections.singletonList(converter));
    when(converter.apply(any(Source.class))).thenReturn(target);
    doCallRealMethod().when(converter).applyCollection(anyList());
  }

  @Test
  void convert_SourceToTarget_Succeed() {
    Target target = objectConverter.convert(new Source(), Target.class);
    then(target).isNotNull();
  }

  @Test
  void convert_SourceToTarget_Failed() {
    assertThatThrownBy(() -> objectConverter.convert(new Target(), Source.class)).isInstanceOf(
        IllegalArgumentException.class);
  }

  @Test
  void convertCollection_SourceToTarget_Succeed() {
    List<Target> target = objectConverter.convertCollection(Collections.singletonList(new Source()), Target.class);
    then(target).isNotEmpty();
    then(target).hasSize(1);
  }

  @Test
  void convertCollection_SourceToTarget_Failed() {
    assertThatThrownBy(
        () -> objectConverter.convertCollection(Collections.singletonList(new Target()), Source.class)).isInstanceOf(
        IllegalArgumentException.class);
  }

  private static class Source {}

  private static class Target {}

}
