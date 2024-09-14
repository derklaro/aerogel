module aerogel.scopedvalues {
  requires aerogel;
  requires jakarta.inject;
  requires static org.apiguardian.api;
  requires static org.jetbrains.annotations;

  provides dev.derklaro.aerogel.internal.context.scope.InjectionContextProvider
    with dev.derklaro.aerogel.scopedvalue.ScopedValueInjectionContextProvider;
}
