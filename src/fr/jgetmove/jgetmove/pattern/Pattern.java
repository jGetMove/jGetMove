package fr.jgetmove.jgetmove.pattern;

import javax.json.JsonObject;
import java.util.List;

public interface Pattern {
    List<JsonObject> getJsonLinks(int index);
}
