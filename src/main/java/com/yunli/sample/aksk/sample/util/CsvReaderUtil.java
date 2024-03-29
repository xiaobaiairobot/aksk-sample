package com.yunli.sample.aksk.sample.util;


import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;
import com.opencsv.exceptions.CsvValidationException;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;


/**
 * @author david
 * @date 2021/7/8 7:14 下午
 */
public class CsvReaderUtil {
  public static List<String[]> readAll(Reader reader) throws IOException, CsvException {

    CSVParser parser = new CSVParserBuilder().build();
    // 大多数场景采用逗号分隔符，如使用其他分隔符请修改如下代码
//    CSVParser parser = new CSVParserBuilder()
//        .withSeparator(',')
//        .withQuoteChar(CSVWriter.NO_QUOTE_CHARACTER)
//        .build();

    CSVReader csvReader = new CSVReaderBuilder(reader)
        // 如果第一行是标题，指定如下代码
        .withSkipLines(0)
        .withCSVParser(parser)
        .build();

    List<String[]> list = new ArrayList<>();
    try {
      list = csvReader.readAll();
      reader.close();
      csvReader.close();
    } catch (Exception ex) {
      throw ex;
    }
    return list;
  }

  public static List<String[]> oneByOne(Reader reader) throws IOException, CsvValidationException {
    List<String[]> list = new ArrayList<>();
    try {
      CSVParser parser = new CSVParserBuilder().build();
      // 大多数分隔符为逗号分割，如果采用其他分隔符请参照如下代码修改
//      CSVParser parser = new CSVParserBuilder()
//          .withSeparator(',')
//          .withIgnoreQuotations(true)
//          .build();

      CSVReader csvReader = new CSVReaderBuilder(reader)
          .withSkipLines(0)
          .withCSVParser(parser)
          .build();

      String[] line;
      while ((line = csvReader.readNext()) != null) {
        list.add(line);
      }
      reader.close();
      csvReader.close();
    } catch (Exception ex) {
      throw ex;
    }
    return list;
  }
}
