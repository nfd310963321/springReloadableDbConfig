package com.nfd.common.spring;

public interface ReconfigurableBean {
  void reloadConfiguration() throws Exception;
}
