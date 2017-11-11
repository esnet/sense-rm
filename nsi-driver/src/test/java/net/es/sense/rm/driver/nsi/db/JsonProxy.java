package net.es.sense.rm.driver.nsi.db;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.io.FileReader;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author hacksaw
 */
@Slf4j
public class JsonProxy {
    private final Gson serializer;

    public JsonProxy() {
        GsonBuilder gson = new GsonBuilder();
        gson.registerTypeAdapter(XMLGregorianCalendar.class, new XMLGregorianCalendarSerializer());
        gson.registerTypeAdapter(XMLGregorianCalendar.class, new XMLGregorianCalendarDeserializer());
        gson.setExclusionStrategies(new JsonExclusionStrategy());
        serializer = gson.create();
    }

    public String serialize(Object obj) {
        return serializer.toJson(obj);
    }

    public <T extends Object> T deserialize(String json, Class<T> classOfT) {
        return serializer.fromJson(json, classOfT);
    }

    public <T extends Object> T deserialize(FileReader fileReader, Class<T> classOfT) {
        return serializer.fromJson(fileReader, classOfT);
    }

    public <T extends Object> String serializeList(Object obj, Class<T> classOfT) {
        Type type = new ListParameterizedType(classOfT);
        return serializer.toJson(obj, type);
    }

    public <T extends Object> List<T> deserializeList(String json, Class<T> classOfT) {
        Type type = new ListParameterizedType(classOfT);
        return serializer.fromJson(json, type);
    }

    public <T extends Object> List<T> deserializeList(FileReader fileReader, Class<T> classOfT) {
        Type type = new ListParameterizedType(classOfT);
        return serializer.fromJson(fileReader, type);
    }

    private static class ListParameterizedType implements ParameterizedType {

        private final Type type;

        private ListParameterizedType(Type type) {
            this.type = type;
        }

        @Override
        public Type[] getActualTypeArguments() {
            return new Type[] {type};
        }

        @Override
        public Type getRawType() {
            return ArrayList.class;
        }

        @Override
        public Type getOwnerType() {
            return null;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 83 * hash + Objects.hashCode(this.type);
            return hash;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (other == null) return false;
            if (this.getClass() != other.getClass()) return false;
            ListParameterizedType otherType = (ListParameterizedType) other;
            return (this.type == otherType.type);
        }
    }

    private class XMLGregorianCalendarSerializer implements JsonSerializer<XMLGregorianCalendar> {
        @Override
        public JsonElement serialize(XMLGregorianCalendar src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.toString());
        }
    }

    private class XMLGregorianCalendarDeserializer implements JsonDeserializer<XMLGregorianCalendar> {
      @Override
      public XMLGregorianCalendar deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
          throws JsonParseException {
          try {
              return DatatypeFactory.newInstance().newXMLGregorianCalendar(json.getAsJsonPrimitive().getAsString());
          } catch (DatatypeConfigurationException ex) {
              log.error("XMLGregorianCalendarDeserializer: Could not convert supplied date " +  json.getAsJsonPrimitive().getAsString());
              return(null);
          }
      }
    }
}
