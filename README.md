# voxxed-demo
IPT + RoboLearn reactive java robotics demo for Voxxed Days Bucharest (March 2016) 

Demo presents ent-to-end reactive hot event stream processing of IPTPI robot sensor events using Spring Reactor library from server side and WebSocket with Angular 2 (TypeScript) and RxJS (RxNext) from client side. No web server needed - all resources as well as live WebSocket events are served by custom reactive HTTP endpoint using Reactor Net.

The main application component class AppComponent is in src/main/webapp/app folder, together with custom reactive WebSocket implementation - IPTRxWebSocketSubject component. IPTRxWebSocketSubject is exposing WebSocket as RxJS bidirectional subject (by idea from RxDOM), plus reactive WebSocket open and close observers.

Limitaion: Demo works well in Chrome, but not in Firefox, because of WebSocket implementation coming from Reactor Net. Further investigation needed.

There are two types of clients - embedded client using Java Swing (whole screen mode) and mobile web client using Angular 2 (TypereScript) and RxJS:

 - The emebedded Swing client is in class RobotView (https://github.com/iproduct/voxxed-demo/blob/master/iptpi-voxxed-demo/src/main/java/org/iproduct/iptpi/demo/view/RobotView.java)  - The web client (ng2 + RxJS) is in src/main/webapp folder (https://github.com/iproduct/voxxed-demo/tree/master/iptpi-voxxed-demo/src/main/webapp)
 
The main ng2 application component class AppComponent is in src/main/webapp/app folder, together with custom reactive WebSocket implementation - IPTRxWebSocketSubject component. IPTRxWebSocketSubject is exposing WebSocket as RxJS bidirectional subject (by idea from RxDOM), plus reactive WebSocket open and close observers.

Server side is implemented using Reactor (http://projectreactor.io/) project. Main main class is org.iproduct.iptpi.demo.IPTPIVoxxedDemo in src/main/java folder. The network communication is implemented in class org.iproduct.iptpi.demo.net.PositionsWsService. It serves all resources required by the client using Reactor Net (Netty), and could be started as console application - no webserver required. Web sockect bi-directional communication is implemented in a reactive (and compact) way in getWsHandler() method.



