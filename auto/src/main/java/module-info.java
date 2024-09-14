module aerogel.auto {
  requires aerogel;
  requires static org.apiguardian.api;
  requires static transitive java.compiler;
  requires static org.jetbrains.annotations;

  exports dev.derklaro.aerogel.auto;
  exports dev.derklaro.aerogel.auto.annotation;
  exports dev.derklaro.aerogel.auto.processing;

  uses dev.derklaro.aerogel.auto.AutoEntryDecoder;
  provides dev.derklaro.aerogel.auto.AutoEntryDecoder with
    dev.derklaro.aerogel.auto.internal.factory.FactoryAutoEntryDecoder,
    dev.derklaro.aerogel.auto.internal.provides.ProvidesAutoEntryDecoder;

  uses dev.derklaro.aerogel.auto.processing.AutoEntryProcessorFactory;
  provides dev.derklaro.aerogel.auto.processing.AutoEntryProcessorFactory with
    dev.derklaro.aerogel.auto.processing.internal.factory.FactoryAutoEntryProcessorFactory,
    dev.derklaro.aerogel.auto.processing.internal.provides.ProvidesAutoEntryProcessorFactory;

  provides javax.annotation.processing.Processor
    with dev.derklaro.aerogel.auto.processing.internal.AutoEntryAnnotationProcessor;
}
