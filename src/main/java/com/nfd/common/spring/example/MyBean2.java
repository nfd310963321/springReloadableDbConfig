package com.nfd.common.spring.example;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 */
public class MyBean2 {
  Log log = LogFactory.getLog(MyBean2.class);

  String cachesize;

  public String getCachesize() {
    return cachesize;
  }

  public void setCachesize(String cachesize) {
    log.info("cache size set to "+cachesize);
    this.cachesize = cachesize;
  }
}
