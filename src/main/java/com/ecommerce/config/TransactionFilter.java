package com.ecommerce.config;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

/**
 * Filtro para gerenciamento automático de transações por request
 * 
 * Este filtro é aplicado como "after" handler para gerenciar
 * o ciclo de vida das transações JPA adequadamente.
 */
public class TransactionFilter {
    
    /**
     * Before handler - cria EntityManager e inicia transação
     */
    public static class BeforeHandler implements Handler {
        @Override
        public void handle(Context ctx) throws Exception {
            // Criar EntityManager para este request
            EntityManager em = DatabaseConfig.createEntityManager();
            DatabaseConfig.setRequestEntityManager(em);
            
            // Iniciar transação para requests que modificam dados
            if (isModifyingRequest(ctx)) {
                EntityTransaction transaction = em.getTransaction();
                transaction.begin();
                System.out.println("🔄 Transação iniciada para: " + ctx.method() + " " + ctx.path());
            }
        }
        
        private boolean isModifyingRequest(Context ctx) {
            String method = ctx.method().toString();
            return "POST".equals(method) || "PUT".equals(method) || 
                   "PATCH".equals(method) || "DELETE".equals(method);
        }
    }
    
    /**
     * After handler - faz commit/rollback e limpa recursos
     */
    public static class AfterHandler implements Handler {
        @Override
        public void handle(Context ctx) throws Exception {
            EntityManager em = null;
            EntityTransaction transaction = null;
            
            try {
                // Obter EntityManager do request
                try {
                    em = DatabaseConfig.getEntityManager();
                } catch (IllegalStateException ex) {
                    // Se não tiver EntityManager ativo, não há o que fazer
                    return;
                }
                if (em != null && em.isOpen()) {
                    transaction = em.getTransaction();
                    
                    // Se tiver transação ativa, fazer commit
                    if (transaction != null && transaction.isActive()) {
                        // Verificar se houve erro no contexto
                        if (ctx.status().getCode() >= 400) {
                            // Se houve erro HTTP, fazer rollback
                            transaction.rollback();
                            System.out.println("❌ Rollback realizado devido ao status HTTP: " + ctx.status().getCode());
                        } else {
                            // Sucesso - fazer commit
                            transaction.commit();
                            System.out.println("✅ Transação commitada para: " + ctx.method() + " " + ctx.path());
                        }
                    }
                }
            } catch (Exception e) {
                // Em caso de erro no commit/rollback
                System.err.println("❌ Erro ao finalizar transação: " + e.getMessage());
                if (transaction != null && transaction.isActive()) {
                    try {
                        transaction.rollback();
                    } catch (Exception rollbackEx) {
                        System.err.println("❌ Erro ao fazer rollback: " + rollbackEx.getMessage());
                    }
                }
            } finally {
                // Limpar recursos
                DatabaseConfig.clearRequestEntityManager();
                if (em != null && em.isOpen()) {
                    em.close();
                }
            }
        }
    }
    
    /**
     * Handler para capturar exceções e garantir rollback
     */
    public static class ExceptionHandler implements io.javalin.http.ExceptionHandler<Exception> {
        @Override
        public void handle(Exception e, Context ctx) {
            EntityManager em = null;
            EntityTransaction transaction = null;
            
            try {
                try {
                    em = DatabaseConfig.getEntityManager();
                } catch (IllegalStateException ex) {
                    // Se não tiver EntityManager ativo, não há o que fazer
                    return;
                }
                if (em != null && em.isOpen()) {
                    transaction = em.getTransaction();
                    if (transaction != null && transaction.isActive()) {
                        transaction.rollback();
                        System.out.println("❌ Rollback realizado devido a exceção: " + e.getMessage());
                    }
                }
            } catch (Exception rollbackEx) {
                System.err.println("❌ Erro ao fazer rollback após exceção: " + rollbackEx.getMessage());
            } finally {
                // Limpar recursos
                DatabaseConfig.clearRequestEntityManager();
                if (em != null && em.isOpen()) {
                    em.close();
                }
            }
            
            // Re-lançar a exceção para o Javalin tratar
            throw new RuntimeException(e);
        }
    }
    
}