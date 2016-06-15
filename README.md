# Truck Simulation
This is a server which allows running simulations of truck fleets.
The server simulates the movements of various trucks. It calculates routes from start to
destination cities and lets trucks drive these routes.
Simulated telematics boxes of these trucks then regularly emit deteriorated telematics
data such as GPS position, speed, forward azimuth/bearing and engine temperature.

The model for each simulation is stored in MongoDB.
When a simulation is started all trucks and their associated routes are loaded into memory. 
Routes consist of several segments with different driving speeds.
The trucks then progress along the route with each interval in the speed that is specified
in the current route segment.

In order to model traffic incidents, a separate collection exists in MongoDB. Each document 
in the traffic collection contains a geometry and a speed attribute. When a truck
enters a traffic incident, its speed is lowered to the value specified in the traffic
incident document.

## Requirements

- MongoDB >= 3.2
- Java 8
- maven >= 3.3

## Sample Data
Initially you can run `mvn compile exec:java@bootstrap` to load a demo simulation
into the datastore and to index the collections in mongodb.

## Configuration
The main configuration is a json file. It can be specified when running the application 
with the `-conf conf.json` option.

An exemplary configuration file might look like this:

```json
{
	"simulation": {
		"osmFile": "osm/germany-latest.osm.pbf",
		"msgInterval": 1,
		"receiverUrl": "http://localhost:1088/trucks",
		"interval_ms": 500
	},
	"mongodb": {
		"db_name": "trucksimulation"
	}
}
```

Make sure that the osmFile exists, downloads are e.g. provided 
by [download.geofabrik.de](http://download.geofabrik.de). The internally used graphhopper library needs
to process the provided osm file when first calculating a route. This may take some time initially.

## Receiving simulated telematics data

### Eventbus
The simulation uses vert.x 3 and sends messages via the vert.x eventbus.
Run the simulation server and an adapter verticle in the same cluster to receive those messages.

### Receiving HTTP requests
HTTP Post reuests will be sent to the `receiverUrl` specified in the configuration file.
The URL must contain the protocol and may optionally contain port and path.

The format of an http post request sent by the simulation server looks as follows:

```json
{ "timeStamp": 1465985004000,
  "truckId": "57600524c91aff1b6865e0eb",
  "altitude": 0,
  "verticalAccuracy": 20,
  "bearing": 324.08819041630136,
  "temperature": 20,
  "horizontalAccuracy": 4,
  "id": "57600524c91aff1b6865e0eb",
  "position": 
   { "type": "Point",
     "coordinates": [ 11.51435004278778, 48.15301418564581 ] },
  "speed": 9.863748019093341 }
```

Speed is provided as m/s, accuracy is in meters and the timestamp is in milliseconds since unix epoch.


### SockJS
Events are emitted using the vert.x sockjs bridge.  
Clients can connect using the [vertx3-eventbus-client](https://www.npmjs.com/package/vertx3-eventbus-client)


## Management API
In a future release it will be possible to create and control simulation projects using a RESTful API.