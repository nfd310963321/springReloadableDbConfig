package com.nfd.common.spring;

/**
 */
public interface ReconfigurationAware {
  public void beforeReconfiguration() throws Exception;;
  public void afterReconfiguration() throws Exception;;
}
