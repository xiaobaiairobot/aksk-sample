package com.yunli.sample.aksk.sample.domain;

/**
 * @author david
 * @date 2021/3/17 2:31 下午
 */
public class LoginDomain {
  private String name;
  private String password;
  private String type;

  public LoginDomain() {
  }

  public LoginDomain(String name, String password, String type) {
    this.name = name;
    this.password = password;
    this.type = type;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }
}
