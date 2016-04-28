# Truck Simulation
This is a server which allows running simulations of truck fleets.
The server simulates the movements of various trucks. It calculates routes from start to destination
cities and lets trucks drive these routes.
Simulated telematics boxes of these trucks then regularly emit telematics data such as GPS position, 
speed, forward azimuth/bearing and engine temperature.


## Configuration
The main configuration is a json file. It can be specified when running the application 
with the `-conf conf.json` option.

## Receiving simulated telematics data

### Eventbus
The simulation uses vert.x 3 and sends messages via the vert.x eventbus.
Run the simulation server and an adapter verticle in the same cluster to receive those messages.

### Receiving HTTP requests
The simulation can be configured to send HTTP POST requests to a specific URL.

### SockJS
Events are emitted using the vert.x sockjs bridge.  
Clients can connect using the [vertx3-eventbus-client](https://www.npmjs.com/package/vertx3-eventbus-client)


## Management API
In a future release it will be possible to create and control simulation projects using a RESTful API.