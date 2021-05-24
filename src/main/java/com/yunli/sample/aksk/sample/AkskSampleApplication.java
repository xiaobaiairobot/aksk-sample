package com.yunli.sample.aksk.sample;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.UUID;

import javax.crypto.NoSuchPaddingException;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yunli.sample.aksk.sample.domain.LoginDomain;
import com.yunli.sample.aksk.sample.domain.SqlExecuteDomain;

/**
 * @author yunli
 */
public class AkskSampleApplication {

  private static final String LOGIN_TYPE = "aksk";

  public static void main(
      String[] args)
      throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidKeySpecException,
      IOException, SignatureException {
    String keyId = "485c344e-aa75-4";
    String privateKey = "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCl8vKjODtV/x3uKm9Lc8uGaC6BJOwUo+gX//+/cJnM**";
    String address = "http://IP:PORT";
    //通过AKSK生成密文
    String cipherText = getCipherText(privateKey);
    RestTemplate restTemplate = new RestTemplate();
    String token = getToken(restTemplate, address, keyId, cipherText);
    if (token != null) {
      System.out.println(String.format("the token is: %s", token));
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
    String input = String.format("%s@%s", UUID.randomUUID().toString(), 3600 * 24 * 6);
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
    HttpEntity<Object> request = new HttpEntity<>(domain, requestHeaders);
    ResponseEntity<String> responseEntity = restTemplate.exchange(uri, HttpMethod.POST, request, String.class);
    if (responseEntity.getStatusCode() == HttpStatus.OK || responseEntity.getStatusCode() == HttpStatus.CREATED) {
      ObjectMapper mapper = new ObjectMapper();
      JsonNode rootNode = mapper.readTree(responseEntity.getBody());
      return rootNode.get("token").asText();
    } else {
      System.out.println("get token failed-------------------");
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
    sqlRequest.setSql("select * from ns_gaolang.y");
    sqlRequest.setTimeout(3600);
    // sqlRequest.setDatabaseId(3L);

    HttpEntity<Object> request = new HttpEntity<>(sqlRequest, requestHeaders);
    try {
      ResponseEntity<byte[]> responseEntity = restTemplate
          .exchange(builder.toUriString(), HttpMethod.POST, request, byte[].class);
      if (responseEntity.getStatusCode() == HttpStatus.OK || responseEntity.getStatusCode() == HttpStatus.CREATED) {
        System.out.println("query Data =====================");
        byte[] body = responseEntity.getBody();
        String result = new String(body,
            StandardCharsets.UTF_8);
        System.out.println(result);
      } else {
        System.out.println("query Data failed -------------------");
      }
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }
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
        String result = new String(body,StandardCharsets.UTF_8);
        System.out.println(result);
      } else {
        System.out.println("query Data failed -------------------");
      }
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }
  }
}
