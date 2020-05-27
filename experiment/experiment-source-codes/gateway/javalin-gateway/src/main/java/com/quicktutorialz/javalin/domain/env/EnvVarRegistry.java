package com.quicktutorialz.javalin.domain.env;

public class EnvVarRegistry {

    public static String getEnv(String envName) {
        String envValue = System.getenv(envName);
        if(envValue==null)
            envValue = "default";
        return envValue;
    }
}
