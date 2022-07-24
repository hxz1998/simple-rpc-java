/**
 * DataLake
 *
 * @Description: TODO
 * @Author: cherry
 * @Create on: 2022/7/23
 **/
package org.metastore.common;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Properties;

@Slf4j
public abstract class Configure {
    protected static Properties properties = new Properties();
    protected static HashMap<String, String> conf;

    protected static String PROPERTIES_FILE = null;

    public static String SERVICE_LIST = "service_list";

    public abstract String get(String key);

    public abstract void set(String key, String value);
}
