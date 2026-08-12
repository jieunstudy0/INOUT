package com.jstudy.inout.stock.testsupport;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcClientAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.sql.init.SqlInitializationAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.TestDatabaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManagerAutoConfiguration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootConfiguration
@Profile("jpa-slice")
@EnableJpaRepositories(basePackages = {
        "com.jstudy.inout.stock.repository",
        "com.jstudy.inout.common.auth.repository"
})
@EntityScan(basePackages = {
        "com.jstudy.inout.stock.entity",
        "com.jstudy.inout.common.auth.entity"
})
@ImportAutoConfiguration({
        TransactionAutoConfiguration.class,
        JpaRepositoriesAutoConfiguration.class,
        TestEntityManagerAutoConfiguration.class,
        CacheAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        SqlInitializationAutoConfiguration.class,
        TestDatabaseAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        JdbcClientAutoConfiguration.class,
        DataSourceAutoConfiguration.class,
        JdbcTemplateAutoConfiguration.class
})
public class StockJpaTestApplication {
}
