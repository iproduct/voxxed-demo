package org.iproduct.iptpi.domain;

public interface CommandService<C extends Command<D>, D> extends Service {
	void execute(C command);
}
