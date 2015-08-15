package hawsensoring;

import net.java.dev.jaxb.array.StringArray;

import java.util.ArrayList;
import java.util.Collections;

public class StringArrayConverter extends StringArray {

    public StringArrayConverter(String[] strings) {
        item = new ArrayList<String>();
        Collections.addAll(item, strings);
    }
}
