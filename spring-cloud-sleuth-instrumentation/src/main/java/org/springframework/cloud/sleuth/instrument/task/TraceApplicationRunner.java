/*
 * Copyright 2018-2021 the original author or authors.
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

package org.springframework.cloud.sleuth.instrument.task;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;

/**
 * Trace representation of a {@link ApplicationRunner}.
 *
 * @author Marcin Grzejszczak
 * @since 3.1.0
 */
public class TraceApplicationRunner implements ApplicationRunner {

	private final BeanFactory beanFactory;

	private final ApplicationRunner delegate;

	private final String beanName;

	private Tracer tracer;

	public TraceApplicationRunner(BeanFactory beanFactory, ApplicationRunner delegate, String beanName) {
		this.beanFactory = beanFactory;
		this.delegate = delegate;
		this.beanName = beanName;
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {
		Span span = tracer().nextSpan().name(this.beanName);
		try (Tracer.SpanInScope spanInScope = tracer().withSpan(span.start())) {
			this.delegate.run(args);
		}
		finally {
			span.end();
		}
	}

	private Tracer tracer() {
		if (this.tracer == null) {
			this.tracer = this.beanFactory.getBean(Tracer.class);
		}
		return this.tracer;
	}

}
