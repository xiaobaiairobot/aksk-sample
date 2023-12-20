package com.yunli.sample.aksk.sample;

import com.yunli.sample.aksk.sample.configuration.HttpsClientRequestFactory;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
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

  public static final Logger LOGGER = LoggerFactory.getLogger(AkskSampleApplication.class);

  public static void main(String[] args)
      throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidKeySpecException,
      IOException, SignatureException {

    String keyId = "5cd57ea****";
    String privateKey = "25101995-3c25****";
    String address = "http://IP:port";
    //通过AKSK生成密文
     String cipherText = getCipherText(privateKey);
    RestTemplate restTemplate = null;
    if(StringUtils.isBlank(address)) {
      throw new NullPointerException("the address is null");
    }
    if(address.startsWith("https")) {
      restTemplate = AkskSampleApplication.getHttpsRestTemplate();
    }else {
      restTemplate = AkskSampleApplication.getHttpRestTemplate();
    }
    String token = getToken( restTemplate, address, keyId, cipherText);
    if (token != null) {
      LOGGER.info("the token is: {}", token);
      //查询SQL执行案例
      queryData(restTemplate, address, token);
      //查询SQL分页执行案例
      queryDataByPage(restTemplate, address, token, 1, 20L);
      //下载文件执行案例
//      downloadFile(restTemplate, address, token);
      //派生指标执行案例
//      queryIndicatorDeriveConsumption(restTemplate,address,token);
      //既定/复合标执行案例
//      queryIndicatorOpenIndicatorsValues(restTemplate,address,token);
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
      throws IOException, NullPointerException {
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
        LOGGER.info("get token failed-------------------");
      }
    } catch (HttpClientErrorException e) {
      LOGGER.info(new String(e.getResponseBodyAsByteArray(), StandardCharsets.UTF_8));
    } catch (Exception e) {
      LOGGER.warn(e.getMessage(), e);
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
    sqlRequest.setSql("select * from ns_dengbinfeng.pupil");
    sqlRequest.setTimeout(3600);

    HttpEntity<Object> request = new HttpEntity<>(sqlRequest, requestHeaders);
    try {
      ResponseEntity<byte[]> responseEntity = restTemplate
          .exchange(builder.toUriString(), HttpMethod.POST, request, byte[].class);
      if (responseEntity.getStatusCode() == HttpStatus.OK || responseEntity.getStatusCode() == HttpStatus.CREATED) {
        LOGGER.debug("query Data =====================");
        byte[] body = responseEntity.getBody();
        String result = new String(body, StandardCharsets.UTF_8);
        // conver csv to json sample
        List listPojo = convertResultToPojo(result);
      } else {
        LOGGER.debug("query Data failed -------------------");
      }
    } catch (HttpClientErrorException | HttpServerErrorException e) {
      LOGGER.warn(new String(e.getResponseBodyAsByteArray(), StandardCharsets.UTF_8));
    } catch (Exception e) {
      LOGGER.warn(e.getMessage(), e);
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
    LOGGER.info("finish read all data from csv to domain: {}", result);
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
            .format("select * from ns_gaolang.y limit %d , %d", pageNumber * pageSize - pageSize, pageSize));
    sqlRequest.setTimeout(3600);

    HttpEntity<Object> request = new HttpEntity<>(sqlRequest, requestHeaders);
    try {
      ResponseEntity<byte[]> responseEntity = restTemplate
          .exchange(builder.toUriString(), HttpMethod.POST, request, byte[].class);
      if (responseEntity.getStatusCode() == HttpStatus.OK || responseEntity.getStatusCode() == HttpStatus.CREATED) {
        LOGGER.debug("query Data By Page=====================");
        byte[] body = responseEntity.getBody();
        if(body == null){
          LOGGER.info("there is not result for this sql: {}", sqlRequest.getSql());
          return;
        }
        String result = new String(body, StandardCharsets.UTF_8);
        LOGGER.info(result);
      } else {
        LOGGER.warn("query Data failed -------------------");
      }
    } catch (HttpClientErrorException | HttpServerErrorException e) {
      LOGGER.warn(new String(e.getResponseBodyAsByteArray(), StandardCharsets.UTF_8));
    } catch (Exception e) {
      LOGGER.warn(e.getMessage(), e);
    }
  }

  private static void downloadFile(RestTemplate restTemplate, String address, String token) {
    //要保存的本地路径
    String savePath = "D://my_demo.pdf";
    //要下载的中台文件
    String uri = address + "/x-data-document-service/resources/documents/8/files/40/download";
    HttpHeaders requestHeaders = new HttpHeaders();
    requestHeaders.set("x-token", token);
    UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(uri);
    restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
      @Override
      public void handleError(ClientHttpResponse clientHttpResponse) {
      }
    });
    HttpEntity<Object> request = new HttpEntity<>(requestHeaders);
    ResponseEntity<Resource> responseEntity = restTemplate
        .exchange(builder.toUriString(), HttpMethod.GET, request, Resource.class);
    if (responseEntity.getStatusCode() == HttpStatus.OK) {
      LOGGER.debug("download File Success =====================");
      File file = new File(savePath);
      Resource body = responseEntity.getBody();
      try (InputStream is = body.getInputStream(); OutputStream os = new FileOutputStream(file)) {
        int read;
        byte[] bytes = new byte[1024];
        while ((read = is.read(bytes)) > 0) {
          os.write(bytes, 0, read);
        }
      } catch (IOException e) {
        LOGGER.warn(e.getMessage(), e);
      }
    } else {
      LOGGER.warn("download File failed -------------------");
    }
  }


  /**
   * 派生指标执行案例
   * @param restTemplate restTemplate
   * @param address address
   * @param token token
   */
  private static String queryIndicatorDeriveConsumption(RestTemplate restTemplate, String address, String token) {
    String uri = address + "/x-data-resource-service/v1/resources/indicator/derive/consumption";
    HttpHeaders requestHeaders = new HttpHeaders();
    requestHeaders.set("x-token", token);
    requestHeaders.add("Accept", MediaType.APPLICATION_JSON.toString());
    requestHeaders.setContentType(MediaType.APPLICATION_JSON);
    UriComponents builder = UriComponentsBuilder.fromHttpUrl(uri).
        queryParam("id", "207").
        queryParam("startTime", "2022071500").
        queryParam("endTime", "2023091500").build();

    return getIndicatorResult(restTemplate,builder,requestHeaders);
  }

  /**
   * 既定/复合指标执行案例
   * @param restTemplate restTemplate
   * @param address address
   * @param token token
   */
  private static String queryIndicatorOpenIndicatorsValues(RestTemplate restTemplate, String address, String token)
      throws UnsupportedEncodingException {
    String uri = address + "/x-data-resource-service/v1/indicator/open/indicators/values";
    HttpHeaders requestHeaders = new HttpHeaders();
    requestHeaders.set("x-token", token);
    requestHeaders.add("Accept", MediaType.APPLICATION_JSON.toString());
    requestHeaders.setContentType(MediaType.APPLICATION_JSON);
    UriComponents builder = UriComponentsBuilder.fromHttpUrl(uri).
        queryParam("_id", "17,8").
        queryParam("_start", "2023-01-01 00:00:00").
        queryParam("_end", "2023-10-01 00:00:00").
        queryParam("_format", "general").
        queryParam("_orders", "17").
        queryParam("_desc", "true").build();
    return getIndicatorResult(restTemplate,builder,requestHeaders);
  }


  /**
   * 获取指标执行结果
   * @param restTemplate restTemplate
   * @param builder builder
   * @param requestHeaders requestHeaders
   */
  private static String getIndicatorResult(RestTemplate restTemplate,UriComponents builder, HttpHeaders requestHeaders) {
    String result = "";
    HttpEntity<Object> request = new HttpEntity<>(requestHeaders);
    try {
      ResponseEntity<byte[]> responseEntity = restTemplate
          .exchange(builder.toUriString(), HttpMethod.GET, request, byte[].class);
      if (responseEntity.getStatusCode() == HttpStatus.OK || responseEntity.getStatusCode() == HttpStatus.CREATED) {
        LOGGER.debug("query Data =====================");
        byte[] body = responseEntity.getBody();
        result = new String(body, StandardCharsets.UTF_8);
        LOGGER.info(result);
      } else {
        LOGGER.debug("query Data failed -------------------");
      }
    } catch (HttpClientErrorException | HttpServerErrorException e) {
      LOGGER.warn(new String(e.getResponseBodyAsByteArray(), StandardCharsets.UTF_8));
    } catch (Exception e) {
      LOGGER.warn(e.getMessage(), e);
    }
    return result;
  }

  private static RestTemplate getHttpsRestTemplate() {
    return new RestTemplate(new HttpsClientRequestFactory());
  }

  private static RestTemplate getHttpRestTemplate() {
    return new RestTemplate(new HttpComponentsClientHttpRequestFactory());
  }
}
