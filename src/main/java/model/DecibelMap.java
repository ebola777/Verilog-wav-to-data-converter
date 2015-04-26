package model;

import java.util.HashMap;
import java.util.Map;

public class DecibelMap {
    private Map<String, Object> db;

    public Map<String, Object> getDb() {
        return new HashMap<String, Object>(this.db);
    }

    public void setDb(final Map<String, Object> db) {
        this.db = new HashMap<String, Object>(db);
    }
}
