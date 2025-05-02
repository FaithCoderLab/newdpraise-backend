package faithcoderlab.newdpraise.config;

import jakarta.persistence.EntityManagerFactory;
import java.util.Properties;
import javax.sql.DataSource;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@TestConfiguration
@EnableJpaRepositories(basePackages = "faithcoderlab.newdpraise")
@EnableTransactionManagement
public class TestDatabaseConfig {

  @Bean
  @Primary
  public DataSource dataSource() {
    DriverManagerDataSource dataSource = new DriverManagerDataSource();
    dataSource.setDriverClassName("org.h2.Driver");
    dataSource.setUrl("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MySQL");
    dataSource.setUsername("sa");
    dataSource.setPassword("");
    return dataSource;
  }

  @Bean
  @Primary
  public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
    LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
    em.setDataSource(dataSource());
    em.setPackagesToScan("faithcoderlab.newdpraise");

    HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
    vendorAdapter.setGenerateDdl(true);
    em.setJpaVendorAdapter(vendorAdapter);

    Properties properties = new Properties();
    properties.setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
    properties.setProperty("hibernate.hbm2ddl.auto", "create-drop");
    properties.setProperty("hibernate.show_sql", "true");
    properties.setProperty("hibernate.format_sql", "true");
    em.setJpaProperties(properties);

    return em;
  }

  @Bean
  @Primary
  public PlatformTransactionManager transactionManager(EntityManagerFactory emf) {
    return new JpaTransactionManager(emf);
  }
}
