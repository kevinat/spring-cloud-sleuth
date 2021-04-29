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

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

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
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.cloud.sleuth.benchmarks.app.mvc.SleuthBenchmarkingSpringApp;
import org.springframework.cloud.sleuth.benchmarks.jmh.TracerImplementation;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Warmup(iterations = 5)
@Measurement(iterations = 10, time = 1)
@Fork(2)
@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Threads(Threads.MAX)
@Microbenchmark
public class HttpFilterNoSleuthBenchmarksTests {

	@Benchmark
	public void filterWithoutSleuth(BenchmarkContext context) throws IOException, ServletException {
		MockHttpServletRequest request = builder().buildRequest(new MockServletContext());
		MockHttpServletResponse response = new MockHttpServletResponse();
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);

		context.dummyFilter.doFilter(request, response, new MockFilterChain());
	}

	@Benchmark
	public void asyncWithoutSleuth(BenchmarkContext context) throws Exception {
		performRequest(context.mockMvcForUntracedController, "vanilla", "vanilla");
	}

	private MockHttpServletRequestBuilder builder() {
		return get("/").accept(MediaType.APPLICATION_JSON).header("User-Agent", "MockMvc");
	}

	private void performRequest(MockMvc mockMvc, String url, String expectedResult) throws Exception {
		MvcResult mvcResult = mockMvc.perform(get("/" + url)).andExpect(status().isOk())
				.andExpect(request().asyncStarted()).andReturn();

		mockMvc.perform(asyncDispatch(mvcResult)).andExpect(status().isOk())
				.andExpect(content().string(expectedResult));
	}

	@State(Scope.Benchmark)
	public static class BenchmarkContext {

		volatile ConfigurableApplicationContext app;

		volatile DummyFilter dummyFilter = new DummyFilter();

		volatile MockMvc mockMvcForUntracedController;

		@Param
		private TracerImplementation tracerImplementation;

		@Setup
		public void setup() {
			this.app = new SpringApplication(SleuthBenchmarkingSpringApp.class).run("--spring.jmx.enabled=false", "--spring.sleuth.enabled=false",

					"--spring.application.name=noSleuth_" + this.tracerImplementation.name());
			assertThat(this.app.getBeanProvider(Tracer.class).getIfAvailable(() -> null)).isNull();
			this.mockMvcForUntracedController = MockMvcBuilders.standaloneSetup(new VanillaController()).build();
		}

		@TearDown
		public void clean() {
			this.app.getBean(SleuthBenchmarkingSpringApp.class).clean();
			this.app.close();
		}

	}

	private static class DummyFilter implements Filter {

		@Override
		public void init(FilterConfig filterConfig) throws ServletException {
		}

		@Override
		public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
				throws IOException, ServletException {
			chain.doFilter(request, response);
		}

		@Override
		public void destroy() {
		}

	}

	@RestController
	private static class VanillaController {

		@RequestMapping("/vanilla")
		public Callable<String> vanilla() {
			return () -> "vanilla";
		}

	}

}
