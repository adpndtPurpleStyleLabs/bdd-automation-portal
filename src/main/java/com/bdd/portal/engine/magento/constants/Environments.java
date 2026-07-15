package com.bdd.portal.engine.magento.constants;

import java.util.HashMap;

public class Environments {
    public static HashMap<String, String> MAGENTO_ENVIRONMENTS = new HashMap<>();
    static {
//        MAGENTO_ENVIRONMENTS.put("STAGE", "https://magento.ppustage.dev/uspp-admin");
        MAGENTO_ENVIRONMENTS.put("MAGE1", "https://mage1.ppustage.dev/uspp-admin");
        MAGENTO_ENVIRONMENTS.put("MAGE2", "https://mage2.ppustage.dev/uspp-admin");
        MAGENTO_ENVIRONMENTS.put("MAGE3", "https://mage3.ppustage.dev/uspp-admin");
//        MAGENTO_ENVIRONMENTS.put("RC", "https://rc.ppustage.dev/uspp-admin");
//        MAGENTO_ENVIRONMENTS.put("UAT", "https://uat.ppustage.dev/uspp-admin");
    }

    public static String getUrlByEnvKey(String envKey){
       return MAGENTO_ENVIRONMENTS.getOrDefault(envKey, "https://magento.ppustage.dev/uspp-admin");
    }
}
