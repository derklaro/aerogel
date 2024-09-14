module aerogel {
  requires jakarta.inject;
  requires io.leangen.geantyref;
  requires static org.apiguardian.api;
  requires static org.jetbrains.annotations;

  exports dev.derklaro.aerogel;
  exports dev.derklaro.aerogel.registry;
  exports dev.derklaro.aerogel.binding;
  exports dev.derklaro.aerogel.binding.key;
  exports dev.derklaro.aerogel.binding.builder;

  exports dev.derklaro.aerogel.internal.context;
  exports dev.derklaro.aerogel.internal.context.scope;

  uses dev.derklaro.aerogel.internal.context.scope.InjectionContextProvider;
  provides dev.derklaro.aerogel.internal.context.scope.InjectionContextProvider
    with dev.derklaro.aerogel.internal.context.scope.threadlocal.ThreadLocalInjectionContextProvider;
}
