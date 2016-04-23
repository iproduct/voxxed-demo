package org.iproduct.iptpi.domain.arduino;
// START SNIPPET: serial-snippet

import java.io.IOException;
import java.nio.ByteBuffer;

import org.reactivestreams.Subscriber;

import com.pi4j.io.serial.Serial;
import com.pi4j.io.serial.SerialDataEvent;
import com.pi4j.io.serial.SerialDataEventListener;
import com.pi4j.io.serial.SerialFactory;
import com.pi4j.io.serial.SerialPortException;

import reactor.core.subscriber.SignalEmitter;
import reactor.rx.Broadcaster;
import reactor.rx.Fluxion;

/**
 * This class represents an event stream (Fluxion in Reactor terms) providing fluent API 
 * for event transformations and allowing all interested consumenrs to listen for 
 * EncoderReadings received from Arduino Leonardo USB connected microcontroller.
 *  
 * @author Trayan Iliev
 */
public class ArduinoDataFluxion extends Fluxion<EncoderReadings> {
	
	public final long START_ARDUINO_SERVICE_TIME;
	public static final long ARDUNO_SERIAL_REPORT_PERIOD = 50; //ms
	public static final String PORT = "/dev/ttyACM0";
	protected static final byte IN_ENCODERS_POSITION = 1;
	private final Broadcaster<EncoderReadings> fluxion;
	private final SignalEmitter<EncoderReadings> emitter;
	private long numberReadings = 0;

	public ArduinoDataFluxion() {
		fluxion = Broadcaster.create();
		emitter = fluxion.startEmitter();
                
        System.out.println("Arduino serial communication started.");
        System.out.println(" Connection settings: 38400, N, 8, 1.");
        
        // create an instance of the serial communications class
        final Serial serial = SerialFactory.createInstance();

        // create and register the serial data listener
        serial.addListener(new SerialDataEventListener() {
        	private ByteBuffer buffer = ByteBuffer.allocate(1024);
			
            @Override
            public void dataReceived(SerialDataEvent event) {
				try {
					ByteBuffer newBuffer = event.getByteBuffer();
					buffer.put(newBuffer);
					buffer.flip();
					System.out.println("Readings length: " + buffer.remaining());
					boolean hasMoreData = true;
					while(buffer.hasRemaining() && hasMoreData) {
						switch(buffer.get(buffer.position())) {
						case IN_ENCODERS_POSITION:
							if(buffer.remaining() >= 17) {
								buffer.get();
								long timestamp = buffer.getInt(); //get timestamp
								int encoderL = -buffer.getInt(); //two motors are mirrored
								int encoderR = buffer.getInt();
								EncoderReadings readings = new EncoderReadings(encoderR, encoderL, timestamp);
//								EncoderReadings readings = new EncoderReadings(encoderR, encoderL,
//										START_ARDUINO_SERVICE_TIME + (numberReadings++) * ARDUNO_SERIAL_REPORT_PERIOD);
//								System.out.println(readings);
								emitter.submit(readings); 
							} else {
								hasMoreData = false;
							}
							break;
						}	
					}
					buffer.compact();
				} catch (Exception e) {
					e.printStackTrace();
				}      
            }
        });
                
        try {
			serial.open(PORT, 38400);
        } catch(SerialPortException | IOException ex) {
            System.out.println(" ==>> SERIAL SETUP FAILED : " + ex.getMessage());
        }
		START_ARDUINO_SERVICE_TIME  = System.currentTimeMillis();
	}
	
	private int bytesToInt(byte[] bytes, int from) {
		return bytes[from + 3] | ( (int)bytes[from + 2] << 8 ) | 
			( (int)bytes[from + 1] << 16 ) | ( (int)bytes[from] << 24 );
	}

	@Override
	public void subscribe(Subscriber<? super EncoderReadings> s) {
		fluxion.subscribe(s);	
	}
}

