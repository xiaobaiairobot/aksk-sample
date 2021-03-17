package com.yunli.sample.aksk.sample.domain;

import java.util.Map;

/**
 * @author david
 * @date 2021/3/17 2:51 下午
 */
public class SqlExecuteDomain {
  private String sql;

  private Integer timeout;

  private String engine;

  private Boolean isIncludeHeaders;

  private Map<String, String> environments;

  private Long databaseId;
  

  public String getSql() {
    return sql;
  }

  public void setSql(String sql) {
    this.sql = sql;
  }

  public Integer getTimeout() {
    return timeout;
  }

  public void setTimeout(Integer timeout) {
    this.timeout = timeout;
  }

  public String getEngine() {
    return engine;
  }

  public void setEngine(String engine) {
    this.engine = engine;
  }

  public Boolean getIncludeHeaders() {
    return isIncludeHeaders;
  }

  public void setIncludeHeaders(Boolean includeHeaders) {
    isIncludeHeaders = includeHeaders;
  }

  public Map<String, String> getEnvironments() {
    return environments;
  }

  public void setEnvironments(Map<String, String> environments) {
    this.environments = environments;
  }

  public Long getDatabaseId() {
    return databaseId;
  }

  public void setDatabaseId(Long databaseId) {
    this.databaseId = databaseId;
  }
}
