package com.shrekshellraiser.formats;

import java.io.File;
import java.io.IOException;

public interface IFormat {
    void save(String filename) throws IOException;

    void save(File filename) throws IOException;

    String get();
}
