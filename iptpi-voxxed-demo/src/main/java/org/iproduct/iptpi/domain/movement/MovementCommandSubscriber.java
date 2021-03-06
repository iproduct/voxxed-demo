package org.iproduct.iptpi.domain.movement;

import org.iproduct.iptpi.domain.Command;
import org.iproduct.iptpi.domain.position.Position;
import org.iproduct.iptpi.domain.position.PositionFluxion;

import com.pi4j.wiringpi.Gpio;

import reactor.core.publisher.SchedulerGroup;
import reactor.core.subscriber.ConsumerSubscriber;
import reactor.core.subscriber.Subscribers;
import reactor.rx.Fluxion;

import static java.lang.Math.*;
import static org.iproduct.iptpi.domain.position.PositionFluxion.*;

import javax.swing.text.Highlighter.HighlightPainter;

import static org.iproduct.iptpi.demo.robot.RobotParametrs.*;

public class MovementCommandSubscriber extends ConsumerSubscriber<Command<Movement>>  {
	public static final int MAX_SPEED = 1024;
	public static final int CLOCK_DIVISOR = 2;
	public static final double LANDING_CURVE_PARAMETER = 0.000000005;
	
	public static final MotorsCommand STOP_COMMAND = new MotorsCommand(0, 0, 0, 0, 0);
	
	private PositionFluxion positions;
	private SchedulerGroup eventLoops = SchedulerGroup.async();
	
	public MovementCommandSubscriber(PositionFluxion positions) {
		this.positions = positions;
		
		// initialize wiringPi library
		Gpio.wiringPiSetupGpio();
		
		// Motor direction pins
		Gpio.pinMode(5, Gpio.OUTPUT);
		Gpio.pinMode(6, Gpio.OUTPUT);

		Gpio.pinMode(12, Gpio.PWM_OUTPUT);
		Gpio.pinMode(13, Gpio.PWM_OUTPUT);
		Gpio.pwmSetMode(Gpio.PWM_MODE_MS);
		Gpio.pwmSetRange(MAX_SPEED);
		Gpio.pwmSetClock(CLOCK_DIVISOR);
	}

	@Override
	public void doNext(Command<Movement> command) {
		
		switch (command.getName()) {
		case "move" :
			// start moving - and think later as it comes :)
			Movement move = command.getData();
			int directionL, directionR;
			if(move.getVelocity() < 0) {
				directionL = directionR = -1;
			} else {
				directionL = directionR = 1;
			}
			
			double targetVelocity = abs(move.getVelocity());
			int velocity = (int)(MAX_SPEED * targetVelocity / MAX_ROBOT_LINEAR_VELOCITY); // 50 mm/s max
			MotorsCommand initialCommand = new MotorsCommand(directionL, directionR, velocity, velocity, Long.MAX_VALUE); //distance still unknown
			System.out.println(initialCommand);
			runMotors(initialCommand);

			Position startPos = positions.elementAt(1).get();
			
			AbsoluteMovement absMove = null;			
			double targetX, targetY, targetDeltaX, targetDeltaY;
			if (move instanceof AbsoluteMovement) {
				absMove = (AbsoluteMovement) move;
				targetX = absMove.getX();
				targetY = absMove.getY();
				targetDeltaX = targetX - startPos.getX();
				targetDeltaY = targetY - startPos.getY();
			} else {
				targetDeltaX = move.getDeltaX();
				targetDeltaY = move.getDeltaY();
				targetX = startPos.getX() + targetDeltaX;
				targetY = startPos.getY() + targetDeltaY;
			}

			double distance = hypot(targetDeltaX, targetDeltaY);
			
			System.out.println("$$$$$$$$$$$$$$ TargetX=" + targetX );
			System.out.println("$$$$$$$$$$$$$$ TargetY=" + targetY );
			System.out.println("$$$$$$$$$$$$$$ Target Distance=" + distance);

			double targetHeading, targetDeltaHeading, targetCurvatureSigned, h;
			if(move.getCurvature() == 0 ) {
				if(absMove != null && absMove.getHeading() != 0) {
					targetHeading = absMove.getHeading();
					targetDeltaHeading = targetHeading - startPos.getHeading();
					double expr = 1 / sin(targetDeltaHeading);
					h = (distance / 2) * ( expr + sqrt(1/(expr * expr) - 1) );
					targetCurvatureSigned = sqrt(sin(targetDeltaHeading) / (h * distance));
				} else {
					targetCurvatureSigned = targetDeltaHeading = h = 0;
					targetHeading = startPos.getHeading();					
				}
			} else {
				targetCurvatureSigned = move.getCurvature();
				h = sqrt( 1/(targetCurvatureSigned * targetCurvatureSigned) - 0.25 * distance * distance );
				targetDeltaHeading = asin(h * distance * targetCurvatureSigned * targetCurvatureSigned) * signum(targetCurvatureSigned);
				targetHeading = startPos.getHeading() + targetDeltaHeading ;
			}

			double xC, yC; //circle center coordinates
			double r = hypot(distance/2, h);
			if(targetCurvatureSigned != 0) {
				double q = hypot( targetX - startPos.getX(), targetY - startPos.getY() ),
					x3 = (targetX + startPos.getX()) /2,
					y3 = (targetY + startPos.getY()) /2;
				if(targetCurvatureSigned > 0) {
					xC = x3 + sqrt(r*r - (q*q/4)) * (startPos.getY() - targetY)/q;
					yC = y3 + sqrt(r*r - (q*q/4)) * (targetX - startPos.getX() )/q;
				} else {
					xC = x3 - sqrt(r*r - (q*q/4)) * (startPos.getY() - targetY)/q;
					yC = y3 - sqrt(r*r - (q*q/4)) * (targetX - startPos.getX() )/q;					
				}
			} else {
				xC = (targetX + startPos.getX()) /2;
				yC = (targetY + startPos.getY()) /2;
			}
			
			System.out.println("$$$$$$$$$$$$$$ TargetCurvature=" + targetCurvatureSigned );
			System.out.println("$$$$$$$$$$$$$$ TargetHeading=" + targetHeading );
			
			double targetCurvature = targetCurvatureSigned;
			double targetAngularVelocity;
			if (targetCurvature != 0 && move.getAngularVelocity() == 0)
				targetAngularVelocity = targetVelocity * targetCurvature;
			else
				targetAngularVelocity = move.getAngularVelocity();
			
						
			double startH = startPos.getHeading();
			System.out.println("START POSITION: " + startPos);
			
			Fluxion<Position> skip1 = positions.skip(1);
			Fluxion.zip(positions, skip1)
				.scan(initialCommand, (last, tupple) -> {
					Position prevPos = ((Position)tupple.getT1());
					Position currPos = ((Position)tupple.getT2());
					float prevX = prevPos.getX();
					float prevY = prevPos.getY();
					double prevH = prevPos.getHeading();
					float currX = currPos.getX();
					float currY = currPos.getY();
					double currH = currPos.getHeading();
					System.out.println(currPos + " - " + prevPos);
					double dt = (currPos.getTimestamp() - prevPos.getTimestamp()) / 1000.0; //delta time in seconds between position redings
				
					if(dt <= 0)	return last; // if invalid sequence do nothing
					
					double time = (currPos.getTimestamp() - startPos.getTimestamp()) /1000.0;
					
					// calculating the ideal trajectory position
					double tarX, tarY, tarH, remainingPathLength;
					if(targetCurvature == 0) {
						tarX = startPos.getX() + targetVelocity * time * cos(targetHeading);
						tarY = startPos.getY() + targetVelocity * time * sin(targetHeading);
						remainingPathLength = hypot(targetX - currX, targetY - currY) ;
						tarH = targetHeading;
					} else {
						double deltaHeading = targetAngularVelocity * time;
						double startAng = atan((startPos.getY() - yC) / (startPos.getX() - xC));
						double angle = startAng + deltaHeading;
						if(signum(angle) != (startPos.getY() - yC))
							angle -= PI;
						tarX = cos(angle) * r + xC;
						tarY = sin(angle) * r + yC;
						tarH = startPos.getHeading() + deltaHeading;
						remainingPathLength = (targetDeltaHeading - deltaHeading ) / targetCurvature;
						System.out.println("   -----> tarX=" + tarX + ", tarY=" + tarY  + ", tarH=" + tarH + ", deltaHeading=" + deltaHeading + ", startAng=" + startAng + ", angle=" + angle);
						System.out.println("   -----> r=" + r + ", xC=" + xC  + ", yC=" + yC );
					}
					
					//calculating current trajectory parameters
					float dX = currX - prevX;
					float dY = currY - prevY;
					double currDist = hypot(dX, dY);
					double currV = currDist / dt; // current velocity [mm/s]
					double currAngV = (currH - prevH) / dt;
					
					//calculating errors
					double errX = (tarX - currX) * cos(tarH) + (tarY - currY) + sin(tarH);
					double errY = (tarX - currX) * sin(tarH) + (tarY - currY) + cos(tarH);
					double errH = tarH - currH;
					
					//calculating landing curve
					double Cx = LANDING_CURVE_PARAMETER;
					double dlandY = 3 * Cx * pow(cbrt(abs(errY) / Cx), 2) * signum(errY);
					double landH = tarH + atan(dlandY);
					double dErrY = -targetAngularVelocity * errX + currV * sin (errH); 
					double landAngV = targetAngularVelocity + (2 * (1 / cbrt(abs(errY) / Cx)) * dErrY) /
							(1 + tan(landH - tarH) * tan(landH - tarH));
					
					//calculating the corrected trajectory control parameters
					double switchAngV = landAngV - currAngV +
							sqrt(2 * MAX_ROBOT_ANGULAR_ACCELERATION * abs(landH - currH)) 
								* signum(landH - currH) * 0.2;
					double switchAngA = min(abs(switchAngV / dt), MAX_ROBOT_ANGULAR_ACCELERATION) * signum(switchAngV);
					double newAngV = currAngV + switchAngA * dt;
					
					
					//calculating new velocity
					double dErrX = targetVelocity - currV * cos(errH) + targetAngularVelocity * errY;
					double switchV = dErrX + sqrt( 2 * MAX_ROBOT_LINEAR_ACCELERATION * abs(errX)) * signum(errX);
					double switchA = min(abs(switchV / dt), MAX_ROBOT_LINEAR_ACCELERATION) * signum(switchV);
//					double newV = currV + switchA * dt;

					//calculating delta motor speed control values
					double k = 0.1;
					double newDeltaLR = k* MAX_SPEED * MAIN_AXE_LENGTH * dt * switchAngA / (2 * WHEEL_RADIUS);

					//calculating new motor speed control values
//					int newDelatLR = (int)((switchAngA * MAX_SPEED * k)/ (2 * MAX_ROBOT_ANGULAR_VELOCITY));
//					int newVL = last.getVelocityL() - newDeltaLR;
//					int newVR = last.getVelocityR() + newDeltaLR;
					int newVL = (int) (last.getVelocityL() + switchA * dt / WHEEL_RADIUS - newDeltaLR * last.getDirL());
					int newVR = (int) (last.getVelocityR() + switchA * dt / WHEEL_RADIUS + newDeltaLR * last.getDirL());

					
					
					System.out.println("!!! time=" + time + ", dt=" + dt  + ", tarX=" + tarX + ", tarY=" + tarY 
							+ ", startH=" + startH + ", errH=" + errH + ", targetX=" + targetX + ", targetY=" + targetY + ", targetHeading=" + targetHeading 
							+ ", errX=" + errX + ", errY=" + errY + ", dlandY=" + dlandY + ", currV=" + currV + ", dist=" + currDist 
							+ ", switchAngV/dt=" + switchAngV / dt );
					System.out.println("!!! landH=" + landH + ", dErrY=" + dErrY
							+ ", currAngV=" + currAngV + ", landAngV=" + landAngV + ", switchAngV=" + switchAngV 
							+ ", switchAngA=" + switchAngA + ", newAngV=" + newAngV );
					System.out.println("!!! remainingPathLength=" + remainingPathLength + ", dErrX=" + dErrX + ", switchV=" + switchV + ", switchA=" + switchA );
					System.out.println("!!! newDeltaV=" + switchA * dt / WHEEL_RADIUS + ", newDelatLR=" + newDeltaLR + ", newVL=" + newVL + ", newVR=" + newVR);

					
					
////					if(dH > 0.01) {
////						newVL -= delay/10;
////						newVR += delay/10;
////					} else 
//						if (dH > 0.003) {
//						newVL -= 1;
//						newVR += 1;
//					} 
////					if(dH < -0.01) {
//////						newVL += delay/10;
//////						newVR -= delay/10;
////					} else 
//						if (dH < -0.003) {
//						newVL += 1;
//						newVR -= 1;
//					} 
					
//					double v = 1000 * dist / dt; // velocity [mm/s]
//					double dLR = MAIN_AXE_LENGTH * dH /2;
//					double dVLR = 1000 * dLR / dt; // velocity difference between left and right motors [mm/s]
//					System.out.println("!!! v=" + v + ", targetV=" + targetV + ", dLR=" + dLR + ", dVLR=" + dVLR );
//					
//					int newVL = (int)(last.getVelocityL() * (1+ (targetV - v - dVLR) / targetV));
//					int newVR = (int)(last.getVelocityR() * (1+ (targetV - v + dVLR) / targetV));
					
					if(remainingPathLength < last.getRemainingPath() 
							&& remainingPathLength > currV * currV / ROBOT_STOPPING_DECCELERATION ) { //drive until minimum distance to target
						return new MotorsCommand(last.getDirL(), last.getDirR(),  newVL,  newVR, (float) remainingPathLength);
					} else {
						System.out.println("FINAL POSITION: " + currPos);
						return STOP_COMMAND;
					}				
				}).map((MotorsCommand motorsCommand) -> {
					runMotors(motorsCommand);
					return motorsCommand;
				})
				.takeUntil((MotorsCommand motorsCommand) -> motorsCommand.equals(STOP_COMMAND) ) 
				.subscribe( Subscribers.consumer( (MotorsCommand motorsCommand) -> {
					System.out.println(motorsCommand);
				}));
			break;
		case "stop" : 
			break;
		}
	}

	private void runMotors(MotorsCommand mc) {
		//setting motor directions 
		Gpio.digitalWrite(5, mc.getDirR() > 0 ? 1 : 0);
		Gpio.digitalWrite(6, mc.getDirL() > 0 ? 1 : 0);
		//setting speed
		if(mc.getVelocityR() >= 0 && mc.getVelocityR() <= MAX_SPEED)
			Gpio.pwmWrite(12, mc.getVelocityR()); // speed up to MAX_SPEED
		if(mc.getVelocityL() >= 0 && mc.getVelocityL() <= MAX_SPEED)
			Gpio.pwmWrite(13, mc.getVelocityL());
		
		
	}

}
