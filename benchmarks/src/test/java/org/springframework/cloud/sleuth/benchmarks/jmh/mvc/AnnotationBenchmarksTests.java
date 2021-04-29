/*
 * Copyright 2013-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.sleuth.benchmarks.jmh.mvc;

import java.util.concurrent.TimeUnit;

import jmh.mbr.junit5.Microbenchmark;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

import org.springframework.boot.SpringApplication;
import org.springframework.cloud.sleuth.benchmarks.app.mvc.SleuthBenchmarkingSpringApp;
import org.springframework.cloud.sleuth.benchmarks.jmh.TracerImplementation;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.BDDAssertions.then;

@Measurement(iterations = 10, time = 1)
@Warmup(iterations = 10, time = 1)
@Fork(4)
@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Threads(Threads.MAX)
@Microbenchmark
public class AnnotationBenchmarksTests {

	@Benchmark
	public void manuallyCreatedSpans(BenchmarkContext context) throws Exception {
		then(context.sleuth.manualSpan()).isEqualTo("continued");
	}

	@Benchmark
	public void spanCreatedWithAnnotations(BenchmarkContext context) throws Exception {
		then(context.sleuth.newSpan()).isEqualTo("continued");
	}

	@State(Scope.Benchmark)
	public static class BenchmarkContext {

		volatile ConfigurableApplicationContext withSleuth;

		volatile SleuthBenchmarkingSpringApp sleuth;

		@Param
		private TracerImplementation tracerImplementation;

		@Setup
		public void setup() {
			this.withSleuth = new SpringApplication(SleuthBenchmarkingSpringApp.class).run("--spring.jmx.enabled=false",

					"--spring.application.name=withSleuth_" + this.tracerImplementation.name());
			this.sleuth = this.withSleuth.getBean(SleuthBenchmarkingSpringApp.class);
		}

		@TearDown
		public void clean() {
			this.sleuth.clean();
			this.withSleuth.close();
		}

	}

}
