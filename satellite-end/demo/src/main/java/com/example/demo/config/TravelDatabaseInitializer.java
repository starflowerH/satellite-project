package com.example.demo.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * 灵境导览文旅数据库自动化初始化器
 * 在服务启动时自动检测 t_poi 等核心表是否存在或有无种子数据，若缺失则自动执行 schema_travel.sql
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TravelDatabaseInitializer implements ApplicationRunner {

    private final DataSource dataSource;

    @Override
    public void run(ApplicationArguments args) {
        log.info("[TravelDatabaseInitializer] 正在检测文旅数据库表与种子数据状态...");
        boolean needInitialize = false;

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(1) FROM t_poi")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    needInitialize = true;
                }
            } catch (Exception tableNotFoundEx) {
                log.info("[TravelDatabaseInitializer] 核心表 t_poi 尚未创建，准备执行 DDL 与种子数据初始化...");
                needInitialize = true;
            }

            if (needInitialize) {
                ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
                populator.addScript(new ClassPathResource("schema_travel.sql"));
                populator.setContinueOnError(true);
                populator.execute(dataSource);
                log.info("[TravelDatabaseInitializer] schema_travel.sql 结构与种子数据部署执行完毕！");
            } else {
                log.info("[TravelDatabaseInitializer] 文旅数据库核心数据已就绪，跳过重复初始化。");
            }

        } catch (Exception e) {
            log.warn("[TravelDatabaseInitializer] 数据库自动检测/初始化过程告警（若为离线环境可忽略）: {}", e.getMessage());
        }
    }
}
