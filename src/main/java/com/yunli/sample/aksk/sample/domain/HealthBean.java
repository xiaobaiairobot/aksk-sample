package com.yunli.sample.aksk.sample.domain;

import java.util.Date;

/**
 * @author david
 * @date 2021/7/8 7:34 下午
 */
public class HealthBean {
  private Long id;

  private String flowNum;

  private String idCard;

  private String illnessName;

  private Date inHospitalDate;

  public HealthBean() {
  }

  public HealthBean(Long id, String flowNum, String idCard, String illnessName, Date inHospitalDate) {
    this.id = id;
    this.flowNum = flowNum;
    this.idCard = idCard;
    this.illnessName = illnessName;
    this.inHospitalDate = inHospitalDate;
  }

  @Override
  public String toString() {
    return "HealthBean{" +
        "id=" + id +
        ", flowNum='" + flowNum + '\'' +
        ", idCard='" + idCard + '\'' +
        ", illnessName='" + illnessName + '\'' +
        ", inHospitalDate=" + inHospitalDate +
        '}';
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getFlowNum() {
    return flowNum;
  }

  public void setFlowNum(String flowNum) {
    this.flowNum = flowNum;
  }

  public String getIdCard() {
    return idCard;
  }

  public void setIdCard(String idCard) {
    this.idCard = idCard;
  }

  public String getIllnessName() {
    return illnessName;
  }

  public void setIllnessName(String illnessName) {
    this.illnessName = illnessName;
  }

  public Date getInHospitalDate() {
    return inHospitalDate;
  }

  public void setInHospitalDate(Date inHospitalDate) {
    this.inHospitalDate = inHospitalDate;
  }
}
