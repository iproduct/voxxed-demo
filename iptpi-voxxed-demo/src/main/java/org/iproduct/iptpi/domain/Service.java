package org.iproduct.iptpi.domain;

import reactor.bus.EventBus;
import reactor.bus.selector.Selector;

public interface Service {
	static <S extends Service> S create(EventBus eb, Selector<?> inEventSelector, Object outEventTtopic) {
		return null;
	}
}
