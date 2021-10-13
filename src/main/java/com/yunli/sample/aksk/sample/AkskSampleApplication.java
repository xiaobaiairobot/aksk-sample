package com.yunli.sample.aksk.sample;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import javax.crypto.NoSuchPaddingException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.exceptions.CsvException;
import com.yunli.sample.aksk.sample.domain.HealthBean;
import com.yunli.sample.aksk.sample.domain.LoginDomain;
import com.yunli.sample.aksk.sample.domain.SqlExecuteDomain;
import com.yunli.sample.aksk.sample.util.CsvReaderUtil;

/**
 * @author yunli
 */
public class AkskSampleApplication {

  private static final String LOGIN_TYPE = "aksk";

  public static void main(
      String[] args)
      throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidKeySpecException,
      IOException, SignatureException {
    String keyId = "2e059499-";
    String privateKey = "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCB***";
    String address = "http://IP:PORT";
    //通过AKSK生成密文
    String cipherText = getCipherText(privateKey);
    RestTemplate restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory());
    String token = getToken(restTemplate, address, keyId, cipherText);
    if (token != null) {
      System.out.printf("the token is: %s%n", token);
      queryData(restTemplate, address, token);
      queryDataByPage(restTemplate, address, token, 1, 20L);
    }
  }

  /***通过AKSK生成密文
   * @param privateKeyString 私钥
  云粒智慧数中台数据访问API使用说明
   */
  private static String getCipherText(String privateKeyString)
      throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidKeyException,
      SignatureException {
    KeyFactory factory = KeyFactory.getInstance("RSA");
    PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKeyString));
    PrivateKey privateKey = factory.generatePrivate(privateKeySpec);
    Signature privateSignature = Signature.getInstance("SHA256withRSA");
    privateSignature.initSign(privateKey);
    String input = String.format("%s@%s", UUID.randomUUID(), 3600 * 24 * 6);
    privateSignature.update(input.getBytes(StandardCharsets.UTF_8));
    return input + "#" + new String(Base64.getEncoder().encode(privateSignature.sign()), StandardCharsets.UTF_8);
  }

  /***获取token**@paramcipherText密文**@returntoken*/
  private static String getToken(RestTemplate restTemplate, String address, String keyId, String cipherText)
      throws IOException {
    String uri = address + "/x-authorization-service/authorizations/logins";
    HttpHeaders requestHeaders = new HttpHeaders();
    requestHeaders.add("Accept", MediaType.APPLICATION_JSON.toString());
    requestHeaders.setContentType(MediaType.APPLICATION_JSON);
    LoginDomain domain = new LoginDomain(
        keyId,
        cipherText,
        AkskSampleApplication.LOGIN_TYPE
    );
    try {
      HttpEntity<Object> request = new HttpEntity<>(domain, requestHeaders);
      ResponseEntity<String> responseEntity = restTemplate.exchange(uri, HttpMethod.POST, request, String.class);
      if (responseEntity.getStatusCode() == HttpStatus.OK || responseEntity.getStatusCode() == HttpStatus.CREATED) {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(responseEntity.getBody());
        return rootNode.get("token").asText();
      } else {
        System.out.println("get token failed-------------------");
      }
    } catch (HttpClientErrorException e) {
      System.out.println(new String(e.getResponseBodyAsByteArray(), StandardCharsets.UTF_8));
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }
    return null;
  }

  private static void queryData(RestTemplate restTemplate, String address, String token) {
    String uri = address + "/x-storage-service/storages/sqls/execute";
    HttpHeaders requestHeaders = new HttpHeaders();
    requestHeaders.set("x-token", token);
    requestHeaders.add("Accept", MediaType.APPLICATION_JSON.toString());
    requestHeaders.setContentType(MediaType.APPLICATION_JSON);
    UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(uri);

    SqlExecuteDomain sqlRequest = new SqlExecuteDomain();
    sqlRequest.setSql("select * from health_hospitalization");
    sqlRequest.setTimeout(3600);
    // sqlRequest.setDatabaseId(3L);

    HttpEntity<Object> request = new HttpEntity<>(sqlRequest, requestHeaders);
    try {
      ResponseEntity<byte[]> responseEntity = restTemplate
          .exchange(builder.toUriString(), HttpMethod.POST, request, byte[].class);
      if (responseEntity.getStatusCode() == HttpStatus.OK || responseEntity.getStatusCode() == HttpStatus.CREATED) {
        System.out.println("query Data =====================");
        byte[] body = responseEntity.getBody();
        String result = new String(body, StandardCharsets.UTF_8);
        // conver csv to json sample
        List listPojo = convertResultToPojo(result);
        System.out.println(listPojo);
      } else {
        System.out.println("query Data failed -------------------");
      }
    } catch (HttpClientErrorException e) {
      System.out.println(new String(e.getResponseBodyAsByteArray(), StandardCharsets.UTF_8));
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }
  }

  /**
   * 将CSV结果转换为json
   * 数据范例：
   * health_hospitalization.hospital_id,health_hospitalization.flow_num,health_hospitalization.id_card,health_hospitalization.illness_name,health_hospitalization.salvation_amount,health_hospitalization.self_amount,health_hospitalization.insurance_amount,health_hospitalization.in_hospital_date,health_hospitalization.out_hospital_date,health_hospitalization.fill_date
   * 620000074180,202009240001,622301197807299659,腰椎间盘突出症,,,,2020-09-25 06:13:11.0,2020-10-09 21:35:06.0,20201011
   * 620000074196,2020H00696,622301196308154426,粘连性肩周炎,,,,2020-09-02 22:21:36.0,2020-09-07 23:25:23.0,20201121
   * 620000074200,202009250001,622301196004052674,带状疱疹,,37.99,850.0,2020-09-25 22:09:07.0,2020-10-02 01:16:40.0,20201017
   * @param result
   */
  private static List convertResultToPojo(String result) throws IOException, CsvException {
    StringReader reader = new StringReader(result);
    List<String[]> listLine = CsvReaderUtil.readAll(reader);
    // TODO
    // change the HealthBean to your own domain
    List<HealthBean> list = new ArrayList<>();
    if (listLine.size() > 1) {
      for (int i = 1; i < listLine.size(); i++) {
        String[] line = listLine.get(i);
        if (line.length >= 10) {
          HealthBean model = new HealthBean();
          model.setId(Long.parseLong(line[0]));
          model.setFlowNum(line[1]);
          model.setIdCard(line[2]);
          model.setIllnessName(line[3]);
          if (!StringUtils.isBlank(line[7])) {
            try {
              model.setInHospitalDate(DateUtils.parseDate(line[7], "yyyy-MM-dd HH:mm:ss.sss"));
            } catch (ParseException e) {
              e.printStackTrace();
            }
          }
          list.add(model);
        }
      }
    }
    System.out.println("finish read all data from csv to domain");
    return list;
  }


  private static void queryDataByPage(RestTemplate restTemplate, String address, String token, Integer pageNumber,
      Long pageSize) {
    if (pageNumber <= 0) {
      pageNumber = 1;
    }
    String uri = address + "/x-storage-service/storages/sqls/execute";
    HttpHeaders requestHeaders = new HttpHeaders();
    requestHeaders.set("x-token", token);
    requestHeaders.add("Accept", MediaType.APPLICATION_JSON.toString());
    requestHeaders.setContentType(MediaType.APPLICATION_JSON);
    UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(uri);

    SqlExecuteDomain sqlRequest = new SqlExecuteDomain();
    sqlRequest
        .setSql(String
            .format("select * from ns_gaolang.y limit %d , %d", pageSize, pageNumber * pageSize - pageSize));
    sqlRequest.setTimeout(3600);
    // sqlRequest.setDatabaseId(3L);

    HttpEntity<Object> request = new HttpEntity<>(sqlRequest, requestHeaders);
    try {
      ResponseEntity<byte[]> responseEntity = restTemplate
          .exchange(builder.toUriString(), HttpMethod.POST, request, byte[].class);
      if (responseEntity.getStatusCode() == HttpStatus.OK || responseEntity.getStatusCode() == HttpStatus.CREATED) {
        System.out.println("query Data By Page=====================");
        byte[] body = responseEntity.getBody();
        String result = new String(body, StandardCharsets.UTF_8);
        System.out.println(result);
      } else {
        System.out.println("query Data failed -------------------");
      }
    } catch (HttpClientErrorException e) {
      System.out.println(new String(e.getResponseBodyAsByteArray(), StandardCharsets.UTF_8));
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }
  }
}
