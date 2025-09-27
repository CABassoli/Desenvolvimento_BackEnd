package com.ecommerce.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuração do banco de dados PostgreSQL com Hibernate e HikariCP
 */
public class DatabaseConfig {
    
    private static EntityManagerFactory entityManagerFactory;
    private static HikariDataSource dataSource;
    
    /**
     * Inicializa a configuração do banco de dados
     */
    public static void initialize() {
        try {
            // Configurar HikariCP
            setupDataSource();
            
            // Configurar Hibernate/JPA
            setupJPA();
            
            System.out.println("✅ Banco de dados PostgreSQL configurado com sucesso");
            System.out.println("📊 Database: " + System.getenv("PGDATABASE"));
            System.out.println("📋 Host: " + System.getenv("PGHOST"));
            
        } catch (Exception e) {
            System.err.println("❌ Erro ao configurar banco de dados: " + e.getMessage());
            throw new RuntimeException("Falha na configuração do banco de dados", e);
        }
    }
    
    /**
     * Configura o HikariCP DataSource
     */
    private static void setupDataSource() {
        HikariConfig config = new HikariConfig();
        
        // Configurações do PostgreSQL usando variáveis individuais
        String host = System.getenv("PGHOST");
        String port = System.getenv("PGPORT");
        String database = System.getenv("PGDATABASE");
        String username = System.getenv("PGUSER");
        String password = System.getenv("PGPASSWORD");
        
        if (host == null || database == null || username == null || password == null) {
            throw new IllegalStateException("PostgreSQL environment variables are not set");
        }
        
        // Construir URL JDBC corretamente
        String jdbcUrl = String.format("jdbc:postgresql://%s:%s/%s?sslmode=require", 
            host, port != null ? port : "5432", database);
        
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("org.postgresql.Driver");
        
        // Configurações do pool de conexões (OTIMIZADO para Replit)
        config.setMaximumPoolSize(10); // Reduzido para evitar sobrecarga
        config.setMinimumIdle(2); // Menor número de conexões idle
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(300000); // 5 minutos
        config.setMaxLifetime(600000); // 10 minutos - mais curto para evitar conexões velhas
        config.setLeakDetectionThreshold(120000); // 2 minutos - mais tolerante
        
        // Configurações de performance
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        
        dataSource = new HikariDataSource(config);
    }
    
    /**
     * Configura o JPA/Hibernate
     */
    private static void setupJPA() {
        Map<String, Object> properties = new HashMap<>();
        
        // Configurações do Hibernate
        properties.put("hibernate.connection.datasource", dataSource);
        properties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        properties.put("hibernate.hbm2ddl.auto", "update"); // Mudando de create-drop para update
        properties.put("hibernate.show_sql", "true");
        properties.put("hibernate.format_sql", "true");
        properties.put("hibernate.use_sql_comments", "true");
        
        // Configurações de performance
        properties.put("hibernate.jdbc.batch_size", "20");
        properties.put("hibernate.order_inserts", "true");
        properties.put("hibernate.order_updates", "true");
        properties.put("hibernate.jdbc.batch_versioned_data", "true");
        
        // CONFIGURAÇÕES PARA TRANSAÇÕES JPA
        // Removidas configurações de autocommit - usaremos transações explícitas
        
        // Cache de segundo nível (opcional)
        properties.put("hibernate.cache.use_second_level_cache", "false");
        properties.put("hibernate.cache.use_query_cache", "false");
        
        entityManagerFactory = Persistence.createEntityManagerFactory("ecommerce-pu", properties);
    }
    
    private static final ThreadLocal<EntityManager> requestEntityManager = new ThreadLocal<>();
    
    /**
     * Obtém o EntityManager do request atual
     */
    public static EntityManager getEntityManager() {
        EntityManager em = requestEntityManager.get();
        if (em == null) {
            throw new IllegalStateException("Nenhum EntityManager ativo para este request. Use createEntityManager() diretamente.");
        }
        return em;
    }
    
    /**
     * Cria um novo EntityManager (usado pelo TransactionFilter)
     */
    public static EntityManager createEntityManager() {
        if (entityManagerFactory == null) {
            throw new IllegalStateException("EntityManagerFactory não foi inicializado");
        }
        return entityManagerFactory.createEntityManager();
    }
    
    /**
     * Define o EntityManager para o request atual (usado pelo TransactionFilter)
     */
    public static void setRequestEntityManager(EntityManager em) {
        requestEntityManager.set(em);
    }
    
    /**
     * Limpa o EntityManager do request atual (usado pelo TransactionFilter)
     */
    public static void clearRequestEntityManager() {
        requestEntityManager.remove();
    }
    
    /**
     * Obtém o DataSource
     */
    public static DataSource getDataSource() {
        return dataSource;
    }
    
    /**
     * Fecha as conexões e limpa recursos
     */
    public static void shutdown() {
        try {
            if (entityManagerFactory != null && entityManagerFactory.isOpen()) {
                entityManagerFactory.close();
            }
            if (dataSource != null && !dataSource.isClosed()) {
                dataSource.close();
            }
            System.out.println("🛑 Banco de dados PostgreSQL fechado com sucesso");
        } catch (Exception e) {
            System.err.println("⚠️ Erro ao fechar banco de dados: " + e.getMessage());
        }
    }
}