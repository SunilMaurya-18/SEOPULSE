package com.seopulse.report.storage;

import java.io.IOException;

public interface ReportStorage {

    void put(String key, byte[] content) throws IOException;

    /** @throws java.io.FileNotFoundException when the object does not exist */
    byte[] get(String key) throws IOException;

    void delete(String key) throws IOException;
}
