package com.ambarx.notificacoesML.utils.enviroment;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class EnvProperties {
    private static final Properties config = new Properties();

    public static String getValue(String attribute) {
        try {
            String arquivo = "env.properties";
            config.load(new FileInputStream(arquivo));
            return config.getProperty(attribute);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}