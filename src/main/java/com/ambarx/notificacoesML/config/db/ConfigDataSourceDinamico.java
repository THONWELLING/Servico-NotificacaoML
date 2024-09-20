package com.ambarx.notificacoesML.config.db;

import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

@Configuration
public class ConfigDataSourceDinamico {
  public DataSource createDataSource(String pUrl, String pUserName, String pPassword) {
    DriverManagerDataSource dataSource = new DriverManagerDataSource();
    dataSource.setUrl(pUrl);
    dataSource.setUsername(pUserName);
    dataSource.setPassword(pPassword);
    return dataSource;
  }
}