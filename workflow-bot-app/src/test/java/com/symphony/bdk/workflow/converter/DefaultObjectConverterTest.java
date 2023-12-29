package com.symphony.bdk.workflow.converter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DefaultObjectConverterTest {

  @Mock
  Converter<Source, Target> converter;
  @Mock
  BiConverter<Source, Object, Target> biConverter;
  @Mock
  private Source source;
  @Mock
  private Target target;
  private ObjectConverter objectConverter;

  @BeforeEach
  void setUp() {
    when(converter.getSourceClass()).thenReturn(Source.class);
    when(converter.getTargetClass()).thenReturn(Target.class);
    when(biConverter.getSourceClass()).thenReturn(Source.class);
    when(biConverter.getTargetClass()).thenReturn(Target.class);
    objectConverter = new DefaultObjectConverter(List.of(converter), Optional.of(List.of(biConverter)));
    when(converter.apply(any(Source.class))).thenReturn(target);
    when(biConverter.apply(any(Source.class), any())).thenReturn(target);
    doCallRealMethod().when(converter).applyCollection(anyList());
    doCallRealMethod().when(biConverter).applyCollection(anyList(), any());
  }

  @Test
  void convert_SourceToTarget_Succeed() {
    Target target = objectConverter.convert(new Source(), Target.class);
    assertThat(target).isNotNull();
    target = objectConverter.convert(new Source(), new Object(), Target.class);
    assertThat(target).isNotNull();
  }

  @Test
  void convert_SourceToTarget_SucceedWithSpecificSourceClass() {
    Target target = objectConverter.convert(new Source(), Source.class, Target.class);
    assertThat(target).isNotNull();
    target = objectConverter.convert(new Source(), new Object(), Source.class, Target.class);
    assertThat(target).isNotNull();
  }

  @Test
  void convert_SourceToTarget_FailWithSpecificSourceClass() {
    assertThatThrownBy(() -> objectConverter.convert(new Source(), Target.class, Target.class)).isInstanceOf(
        IllegalArgumentException.class).hasMessageContaining("Cannot find converter for the given source type");
    assertThatThrownBy(
        () -> objectConverter.convert(new Source(), new Object(), Target.class, Target.class)).isInstanceOf(
        IllegalArgumentException.class).hasMessageContaining("Cannot find converter for the given source type");
  }

  @Test
  void convert_SourceToTarget_Failed() {
    assertThatThrownBy(() -> objectConverter.convert(new Target(), Source.class)).isInstanceOf(
        IllegalArgumentException.class);
    assertThatThrownBy(() -> objectConverter.convert(new Target(), new Object(), Source.class)).isInstanceOf(
        IllegalArgumentException.class);
  }

  @Test
  void convertCollection_SourceToTarget_Succeed() {
    List<Target> target = objectConverter.convertCollection(Collections.singletonList(new Source()), Target.class);
    assertThat(target).isNotEmpty();
    assertThat(target).hasSize(1);
    target = objectConverter.convertCollection(Collections.singletonList(new Source()), new Object(), Target.class);
    assertThat(target).isNotEmpty();
    assertThat(target).hasSize(1);
  }

  @Test
  void convertCollection_SourceToTarget_SucceedWithSpecificSourceClass() {
    List<Target> target =
        objectConverter.convertCollection(Collections.singletonList(new Source()), Source.class, Target.class);
    assertThat(target).isNotEmpty();
    assertThat(target).hasSize(1);
    target = objectConverter.convertCollection(Collections.singletonList(new Source()), new Object(), Source.class,
        Target.class);
    assertThat(target).isNotEmpty();
    assertThat(target).hasSize(1);
  }

  @Test
  void convertCollection_SourceToTarget_FailWithSpecificSourceClass() {
    assertThatThrownBy(
        () -> objectConverter.convertCollection(Collections.singletonList(new Source()), Target.class,
            Target.class)).isInstanceOf(
        IllegalArgumentException.class);
    assertThatThrownBy(
        () -> objectConverter.convertCollection(Collections.singletonList(new Source()), new Object(), Target.class,
            Target.class)).isInstanceOf(
        IllegalArgumentException.class);
  }

  @Test
  void convertCollection_SourceToTarget_Failed() {
    assertThatThrownBy(
        () -> objectConverter.convertCollection(Collections.singletonList(new Target()), Source.class)).isInstanceOf(
        IllegalArgumentException.class);
    assertThatThrownBy(
        () -> objectConverter.convertCollection(Collections.singletonList(new Target()), new Object(),
            Source.class)).isInstanceOf(
        IllegalArgumentException.class);
  }

  private static class Source {}


  private static class Target {}

}
