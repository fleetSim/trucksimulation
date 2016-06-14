package demo;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;

import io.vertx.core.Launcher;
import io.vertx.core.json.JsonObject;

public class Bootstrapper {
	
		

	public static void main(String[] args) throws IOException, InterruptedException {
		JsonObject conf = readConf();
		bootstrapCities(conf);
		checkOsmFile(conf);
		Launcher.main(args);
	}

	private static JsonObject readConf() throws IOException {
		File f = new File("conf.json");
		JsonObject conf = new JsonObject(new String(Files.readAllBytes(f.toPath())));
		return conf;
		
	}

	private static void checkOsmFile(JsonObject conf) {
		File file = new File(conf.getJsonObject("simulation").getString("osmFile"));
		if(!file.exists()) {
			System.err.println("OSM file could not be found. Please download OSM file or fix configuration and try again.");
			System.exit(1);
		}
	}

	private static void bootstrapCities(JsonObject conf) throws IOException, InterruptedException {
		String cmd = String.format("mongoimport -d %s --collection cities fixtures/DE/citiesde.json", conf.getJsonObject("mongodb").getString("db_name"));
		System.out.println("Running import command " + cmd);
		Process c = Runtime.getRuntime().exec(cmd);
		c.waitFor();
		BufferedReader reader = new BufferedReader(new InputStreamReader(c.getInputStream()));
		String line = "";
		while ((line = reader.readLine())!= null) {
			System.out.println(line);
		}
		if(c.exitValue() != 0) {
			BufferedReader stderr = new BufferedReader(new InputStreamReader(c.getErrorStream()));
			while ((line = stderr.readLine())!= null) {
				System.out.println(line);
			}
			System.err.println("Mongoimport failed.");
			System.exit(1);
		}
	}

}
