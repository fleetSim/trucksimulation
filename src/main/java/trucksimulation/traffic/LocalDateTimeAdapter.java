package trucksimulation.traffic;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/**
 * Serialize Date instances to UTC epoch time representation in seconds.
 *
 */
public class LocalDateTimeAdapter implements JsonSerializer<LocalDateTime> {

	@Override
	public JsonElement serialize(LocalDateTime src, Type typeOfSrc, JsonSerializationContext context) {
		long epochSeconds = src.toEpochSecond(ZoneOffset.UTC);
		return new JsonPrimitive(epochSeconds);
	}

}
