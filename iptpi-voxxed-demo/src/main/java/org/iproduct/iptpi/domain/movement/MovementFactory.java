package org.iproduct.iptpi.domain.movement;
import org.iproduct.iptpi.domain.position.PositionFluxion;

public class MovementFactory {
	public static CommandMovementSubscriber createCommnadMotorsSubscriber(PositionFluxion positions) {
		return new CommandMovementSubscriber(positions);
	}
} 
