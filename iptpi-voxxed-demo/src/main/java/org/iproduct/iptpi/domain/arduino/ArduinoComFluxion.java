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
 * This example code demonstrates how to perform serial communications using the
 * Raspberry Pi.
 * 
 * @author Robert Savage
 */
public class ArduinoComFluxion extends Fluxion<EncoderReadings> {
	
	public final long START_ARDUINO_SERVICE_TIME;
	public static final long ARDUNO_SERIAL_REPORT_PERIOD = 50; //ms
	public static final String PORT = "/dev/ttyACM0";
	protected static final byte IN_ENCODERS_POSITION = 1;
	private final Broadcaster<EncoderReadings> fluxion;
	private final SignalEmitter<EncoderReadings> emitter;
	private long numberReadings = 0;

	public ArduinoComFluxion() {
		fluxion = Broadcaster.create();
		emitter = fluxion.startEmitter();
        
		// !! ATTENTION !!
        // By default, the serial port is configured as a console port 
        // for interacting with the Linux OS shell.  If you want to use 
        // the serial port in a software program, you must disable the 
        // OS from using this port.  Please see this blog article by  
        // Clayton Smith for step-by-step instructions on how to disable 
        // the OS console for this port:
        // http://www.irrational.net/2012/04/19/using-the-raspberry-pis-serial-port/
                
        System.out.println("<--Pi4J--> Serial Communication Example ... started.");
        System.out.println(" ... connect using settings: 38400, N, 8, 1.");
        System.out.println(" ... data received on serial port should be displayed below.");
        
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
            // open the default serial port provided on the GPIO header
//            serial.open(Serial.DEFAULT_COM_PORT, 38400);
            try {
				serial.open(PORT, 38400);
			} catch (IOException e) {
				e.printStackTrace();
			}
            
            // continuous loop to keep the program running until the user terminates the program
//            for (;;) {
//                try {
//                    // write a formatted string to the serial transmit buffer
//                    serial.write("CURRENT TIME: %s", new Date().toString());
//    
//                    // write a individual bytes to the serial transmit buffer
//                    serial.write((byte) 13);
//                    serial.write((byte) 10);
//    
//                    // write a simple string to the serial transmit buffer
//                    serial.write("Second Line");
//    
//                    // write a individual characters to the serial transmit buffer
//                    serial.write('\r');
//                    serial.write('\n');
//    
//                    // write a string terminating with CR+LF to the serial transmit buffer
//                    serial.writeln("Third Line");
//                }
//                catch(IllegalStateException ex){
//                    ex.printStackTrace();                    
//				}
//                
//                // wait 1 second before continuing
//                Thread.sleep(1000);
//            }
            
        } catch(SerialPortException ex) {
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

// END SNIPPET: serial-snippet
