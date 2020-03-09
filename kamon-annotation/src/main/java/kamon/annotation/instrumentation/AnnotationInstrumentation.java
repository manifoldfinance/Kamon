/*
 * =========================================================================================
 * Copyright © 2013-2019 the kamon project <http://kamon.io/>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law withReturnValues agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express withReturnValues implied. See the License for the specific language governing permissions
 * and limitations under the License.
 * =========================================================================================
 */

package kamon.annotation.instrumentation;

import kamon.annotation.instrumentation.advisor.*;
import kanela.agent.api.instrumentation.InstrumentationBuilder;

import static kanela.agent.libs.net.bytebuddy.matcher.ElementMatchers.*;

public final class AnnotationInstrumentation extends InstrumentationBuilder {

  private static final String Trace = "kamon.annotation.api.Trace";
  private static final String CustomizeSpan = "kamon.annotation.api.CustomizeInnerSpan";
  private static final String Count = "kamon.annotation.api.Count";
  private static final String TrackConcurrency = "kamon.annotation.api.TrackConcurrency";
  private static final String Timer = "kamon.annotation.api.Time";
  private static final String Histogram = "kamon.annotation.api.Histogram";
  private static final String Gauge = "kamon.annotation.api.Gauge";

  public AnnotationInstrumentation() {

    onTypesWithMethodsAnnotatedWith(Trace)
        .advise(isAnnotatedWith(named(Trace)), TraceAnnotationAdvisor.class);

    onTypesWithMethodsAnnotatedWith(CustomizeSpan)
        .advise(isAnnotatedWith(named(CustomizeSpan)), CustomizeInnerSpanAnnotationAdvisor.class);

    onTypesWithMethodsAnnotatedWith(Count)
        .advise(isAnnotatedWith(named(Count)), CountAnnotationAdvisor.class);

    onTypesWithMethodsAnnotatedWith(TrackConcurrency)
        .advise(isAnnotatedWith(named(TrackConcurrency)), TrackConcurrencyAnnotationAdvisor.class);

    onTypesWithMethodsAnnotatedWith(Timer)
        .advise(isAnnotatedWith(named(Timer)), TimeAnnotationAdvisor.class);

    onTypesWithMethodsAnnotatedWith(Histogram)
        .advise(isAnnotatedWith(named(Histogram)).and(withReturnTypes(long.class, double.class, int.class, float.class)), HistogramAnnotationAdvisor.class);

    onTypesWithMethodsAnnotatedWith(Gauge)
        .advise(isAnnotatedWith(named(Gauge)).and(withReturnTypes(long.class, double.class, int.class, float.class)), GaugeAnnotationAdvisor.class);
  }
}
