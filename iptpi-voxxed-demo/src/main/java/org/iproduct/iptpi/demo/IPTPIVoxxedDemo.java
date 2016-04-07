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
		arduino = new ArduinoComFluxion(); 

		positionsPub = PositionFactory.createPositionFluxion(arduino);
		presentationViews.add(PositionFactory.createPositionPanel(positionsPub));
		
		//wire robot main controller with services
		movementSub = MovementFactory.createCommnadMotorsSubscriber(positionsPub);
		controller = new RobotController(Subscribers.consumer(this::tearDown), movementSub);
		
		//create view with controller and delegate material views from query services
		view = new RobotView("IPTPI Reactive Robotics Demo", controller, presentationViews);
		
		//expose as WS service
		movementSub2 = MovementFactory.createCommnadMotorsSubscriber(positionsPub);
		positionsService = new PositionsWsService(positionsPub, movementSub2);		
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
