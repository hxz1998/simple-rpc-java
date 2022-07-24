/**
 * DataLake
 *
 * @Description: TODO
 * @Author: cherry
 * @Create on: 2022/7/23
 **/
package org.metastore.proxy.conf;

import lombok.extern.slf4j.Slf4j;
import org.metastore.common.Configure;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

@Slf4j
public class ProxyConfigure extends Configure {
    static {
        PROPERTIES_FILE = "metastore.proxy.properties";
        try {
            InputStream propertiesInputStream =
                    Configure.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE);
            properties.load(propertiesInputStream);
            conf = new HashMap<>(properties.size());
            log.info("Loaded " + PROPERTIES_FILE);
            for (Object key : properties.keySet()) {
                log.info(key.toString() + ": " + properties.get(key));
                conf.put(key.toString(), properties.get(key).toString());
            }
        } catch (IOException e) {
            log.error("Can not load " + PROPERTIES_FILE);
            throw new RuntimeException(e);
        }
    }

    @Override
    public String get(String key) {
        return conf.get(key);
    }

    @Override
    public void set(String key, String value) {
        conf.put(key, value);
    }
}
