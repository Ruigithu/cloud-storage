package com.ruipeng.cloudstorage.config.mybatis;


import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.type.TypeHandler;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import javax.sql.DataSource;

@Configuration
public class MyBatisConfig {

    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);

        // 注册 TypeHandler
        factoryBean.setTypeHandlers(new TypeHandler[]{
                new LtreeTypeHandler(),
                new PermissionTypeHandler(),
                new UUIDTypeHandler()
        });

        return factoryBean.getObject();
    }
}
