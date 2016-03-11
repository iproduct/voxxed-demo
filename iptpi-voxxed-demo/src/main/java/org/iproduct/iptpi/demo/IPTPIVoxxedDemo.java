package org.iproduct.iptpi.demo;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;

import org.iproduct.iptpi.demo.controller.RobotController;
import org.iproduct.iptpi.demo.net.PositionsWsService;
import org.iproduct.iptpi.demo.view.RobotView;
import org.iproduct.iptpi.domain.arduino.ArduinoComFluxion;
import org.iproduct.iptpi.domain.movement.CommandMovementSubscriber;
import org.iproduct.iptpi.domain.movement.MovementFactory;
import org.iproduct.iptpi.domain.position.PositionFactory;
import org.iproduct.iptpi.domain.position.PositionFluxion;

import reactor.core.subscriber.Subscribers;
import reactor.core.util.Logger;

public class IPTPIVoxxedDemo {
	private RobotController controller;
	private RobotView view;
	private ArduinoComFluxion arduino;
	private PositionFluxion positionsPub;
	private CommandMovementSubscriber movementSub, movementSub2;
	private List<JComponent> presentationViews = new ArrayList<>();
	private PositionsWsService positionsService;
	
	public IPTPIVoxxedDemo() {
		//Create services first
		//Eventbus
//		bus = EventBus.create(); 
		arduino = new ArduinoComFluxion();

//		bus.on($("iptpi.position"), (Event<String> ev) -> {
//		  String s = ev.getData();
//		  System.out.printf("Got %s on thread %s%n", s, Thread.currentThread());
//		}); 

		positionsPub = PositionFactory.createPositionFluxion(arduino);
		presentationViews.add(PositionFactory.createPositionPanel(positionsPub));
		
		//wire controller with services
		movementSub = MovementFactory.createCommnadMotorsSubscriber(positionsPub);
		controller = new RobotController(Subscribers.consumer(this::tearDown), movementSub);
		
		//create view wiht controller and delegate material views from query services
		view = new RobotView("IPTPI Reactive Robotics Demo", controller, presentationViews);
		
		//expose as WS service
		movementSub2 = MovementFactory.createCommnadMotorsSubscriber(positionsPub);
		positionsService = new PositionsWsService(positionsPub, movementSub2);
		
		
//		// initialize wiringPi library
//		Gpio.wiringPiSetupGpio();
//		
//		// Motor direction pins
//		Gpio.pinMode(5, Gpio.OUTPUT);
//		Gpio.pinMode(6, Gpio.OUTPUT);
//
//		Gpio.pinMode(12, Gpio.PWM_OUTPUT);
//		Gpio.pinMode(13, Gpio.PWM_OUTPUT);
//		Gpio.pwmSetMode(Gpio.PWM_MODE_MS);
//		Gpio.pwmSetRange(480);
//		Gpio.pwmSetClock(2);
//
//
//		System.out.println("Turning left");
//		//setting motor directions 
//		Gpio.digitalWrite(5, 1);
//		Gpio.digitalWrite(6, 1);
//		//setting speed
//		Gpio.pwmWrite(12, 460); // speed 460 of 480 max
//		Gpio.pwmWrite(13, 460);
//		// turn duration
//		try {
//			Thread.sleep(3000);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}


//		System.out.println("Turning right");
//		//setting motor directions 
//		Gpio.digitalWrite(5, 1);
//		Gpio.digitalWrite(6, 0);
//		//setting speed
//		Gpio.pwmWrite(12, 460); // speed 460 of 480 max
//		Gpio.pwmWrite(13, 460);
//		// turn duration
//		Thread.sleep(3000);
//
//		System.out.println("Running motors forward accelerating");
//
//		for (int i = 0; i <= 480; i++) {
//			//setting motor directions 
//			Gpio.digitalWrite(5, 1);
//			Gpio.digitalWrite(6, 1);
//			//setting speed
//			Gpio.pwmWrite(12, i); 
//			Gpio.pwmWrite(13, i);
//			Thread.sleep(40);
//		}
//
//		System.out.println("Running motors forward decelerating");
//		for (int i = 480; i > 0; i--) {
//			//setting motor directions 
//			Gpio.digitalWrite(5, 1);
//			Gpio.digitalWrite(6, 1);
//			//setting speed
//			Gpio.pwmWrite(12, i);
//			Gpio.pwmWrite(13, i);
//			Thread.sleep(20);
//		}
//
//		//setting motor directions 
//		Gpio.digitalWrite(5, 0);
//		Gpio.digitalWrite(6, 1);
//		//setting speed
//		Gpio.pwmWrite(12, 240);
//		Gpio.pwmWrite(13, 240);
//		Thread.sleep(5000);
//
//		System.out.println("Running motors backwards decelerating");
//		for (int i = 480; i > 0; i--) {
//			//setting motor directions 
//			Gpio.digitalWrite(5, 0);
//			Gpio.digitalWrite(6, 0);
//			//setting speed
//			Gpio.pwmWrite(12, i);
//			Gpio.pwmWrite(13, i);
//			Thread.sleep(40);
//		}

//		// turning the motors off
//		Gpio.digitalWrite(5, 0);
//		Gpio.digitalWrite(6, 0);
//		Gpio.pwmWrite(12, 0);
//		Gpio.pwmWrite(13, 0);
//
//		System.out.println("End of the demo.");
		
		
//		// Create an async message-passing Processor exposing a Flux API
//		TopicProcessor<String> sink = TopicProcessor.create();
//		SchedulerGroup eventLoops = SchedulerGroup.async();
//
//		// Scatter Gather the input sequence
////		sink
////		  .map(String::toUpperCase)
////		  .flatMap(s -> 
////		      Mono.fromCallable(() -> s)
////		          .publishOn(eventLoops)
////		  )
////		  .subscribe(Subscribers.consumer(System.out::println));
//
//		// Sink values asynchronously
//		sink.onNext("Rx");
//		sink.onNext("ReactiveStreams");
//		sink.onNext("ReactiveStreamsCommons");
//		sink.onNext("RingBuffer");
//
//		//Shutdown and clean async resources
//		sink.onComplete();
	}
	
	public void tearDown(Integer exitStatus) {
		Logger log = Logger.getLogger(this.getClass().getName());
		log.info("Tearing down services and exiting the system");
		try {
			positionsService.teardown();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.exit(exitStatus);
	}
	
	public static void main(String[] args) {
		IPTPIVoxxedDemo demo = new IPTPIVoxxedDemo();
	}
}
