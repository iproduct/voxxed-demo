package org.iproduct.iptpi.domain.movement;
import org.iproduct.iptpi.domain.position.PositionFluxion;

public class MovementFactory {
	public static MovementCommandSubscriber createCommandMovementSubscriber(PositionFluxion positions) {
		return new MovementCommandSubscriber(positions);
	}
} 
