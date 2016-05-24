package org.iproduct.iptpi.demo.controller;

import org.iproduct.iptpi.domain.Command;
import org.iproduct.iptpi.domain.movement.MovementCommandSubscriber;
import org.iproduct.iptpi.domain.movement.Movement;
import org.reactivestreams.Subscriber;

import reactor.core.publisher.Mono;
import reactor.core.publisher.TopicProcessor;
import reactor.rx.Promise;

public class RobotController {
	private MovementCommandSubscriber commandSub;
	private TopicProcessor<Command<Movement>> commands = TopicProcessor.create();
	private Subscriber<Integer> onExitSubscriber;
		
	public RobotController(Subscriber<Integer> onExitSubscriber, MovementCommandSubscriber commandSub) {
		this.onExitSubscriber = onExitSubscriber;
		this.commandSub = commandSub;
		commands.subscribe(commandSub);
	}

	public void moveUp() {
		commands.onNext(new Command<>("move", new Movement(400, 0, 0,  50)));
	}

	public void moveDown() {
		commands.onNext(new Command<>("move", new Movement(-200, 0, 0,  -50)));
	}

	public void moveLeft() {
		commands.onNext(new Command<>("move", new Movement(200, 0, 1/300f,  40)));
	}

	public void moveRight() {
		commands.onNext(new Command<>("move", new Movement(400, 0, -1/300f,  40)));
	}

	public void exit() {
		Mono.just(0).subscribe(onExitSubscriber);
		
	}

}
