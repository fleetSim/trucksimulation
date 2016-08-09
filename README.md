# Truck Simulation

[![Build Status](https://travis-ci.org/fleetSim/trucksimulation.svg?branch=master)](https://travis-ci.org/fleetSim/trucksimulation)

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

## Setup

### Requirements

- MongoDB >= 3.2
- Java 8
- maven >= 3.3


### Generating Sample Data

Initially you can run `mvn compile exec:java@bootstrap` to load a demo simulation
into the datastore and to index the collections in mongodb. The bootstrap task uses the mongo
settings which are present in the `conf.json` file.

This also imports the cities collection from a json dump via 
`mongoimport -d trucksimulation -c cities fixtures/DE/citiesde.json`.
The cities collection is required to generate sample data and to determine random destinations
in *endless* mode.


### Configuration

The main configuration is a json file. It can be specified when running the application 
with the `-conf conf.json` option.

An exemplary configuration file might look like this:

```json
{
	"port": 8080,
	"simulation": {
		"osmFile": "osm/germany-latest.osm.pbf",
		"msgInterval": 10,
		"receiverUrl": "http://localhost:8081/telematics/fleetsim",
		"postData": true,
		"interval_ms": 1000
	},
	"mongodb": {
		"db_name": "trucksimulation"
	},
	"amqp": {
		"enabled": false,
		"uri": "amqp://localhost"
	}
}
```

Make sure that the osmFile exists, downloads are e.g. provided 
by [download.geofabrik.de](http://download.geofabrik.de). The internally used GraphHopper library needs
to process the provided OSM file when first calculating a route. This may take some time initially.



## Usage

### Management API

#### listing available simulations

`curl -X GET http://localhost:8080/api/v1/simulations`

```json
[
    {
        "_id": "demo",
        "description": "small demo simulation"
    },
    {
        "_id": "demoBig",
        "description": "large endless simulation",
        "endless": true
    }
]
```

#### starting/stopping a simulation

In order to start the simulation *demo*, issue a POST request to `http://localhost:8080/api/v1/simulations/demo/start`

`curl -X POST http://localhost:8080/api/v1/simulations/demo/start`

```json
{
    "status": "started"
}
```

and to stop it:

`curl -X POST http://localhost:8080/api/v1/simulations/demo/stop`

```json
{
    "status": "stopped"
}
```


#### listing trucks that belong to a simulation

`curl http://localhost:8080/api/v1/simulations/demo/trucks`

```json

[
    {
        "_id": "5772c761320e5c287a200d0f",
        "route": "5772c761320e5c287a200d0e",
        "simulation": "demo"
    },
    {
        "_id": "5772c75f320e5c287a200d0a",
        "route": "5772c75f320e5c287a200d09",
        "simulation": "demo"
    },
    {
        "_id": "5772c75e320e5c287a200d05",
        "route": "5772c75e320e5c287a200d04",
        "simulation": "demo"
    }
]
```

### Receiving simulated telematics data

#### Message Format
Messages are provided in JSON format and originate from the simulated telematics boxes.
GSON adapters are used for serialization. Consult the `trucksimulation.Serializer` class to
see which adapters are in use.

#### Eventbus
The simulation uses vert.x 3 and sends messages via the vert.x eventbus.
Run the simulation server and an adapter verticle in the same cluster to receive those messages.
Bus addresses are listed in the `trucksimulation.Bus` enum.

#### Receiving HTTP requests
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


#### SockJS
Events are emitted using the vert.x sockjs bridge.
Clients can connect using the [vertx3-eventbus-client](https://www.npmjs.com/package/vertx3-eventbus-client)

