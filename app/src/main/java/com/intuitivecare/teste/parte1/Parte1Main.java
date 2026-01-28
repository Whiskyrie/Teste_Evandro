package com.intuitivecare.teste.parte1;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intuitivecare.teste.parte1.application.IntegracaoANSService;
import com.intuitivecare.teste.parte1.domain.TrimestreANS;
import com.intuitivecare.teste.parte1.infrastructure.ANSHttpClient;

/**
 * Main class para executar a Parte 1: Integração com API da ANS.
 */
public class Parte1Main {
    
    private static final Logger logger = LoggerFactory.getLogger(Parte1Main.class);
    
    public static void main(String[] args) {
        logger.info("=== PARTE 1: INTEGRAÇÃO COM API DA ANS ===");
        
        try {
            // Define diretório de trabalho
            Path diretorioTrabalho = Paths.get("dados/parte1");
            
            // Cria cliente HTTP para identificar trimestres disponíveis
            ANSHttpClient httpClient = new ANSHttpClient();
            
            // Identifica últimos 3 trimestres disponíveis dinamicamente no servidor
            logger.info("Identificando últimos trimestres disponíveis no servidor da ANS...");
            List<TrimestreANS> trimestres = httpClient.identificarUltimosTrimestreDisponiveis(3);
            
            if (trimestres.isEmpty()) {
                logger.error("ERRO: Nenhum trimestre disponível encontrado no servidor da ANS");
                System.exit(1);
            }
            
            logger.info("Trimestres selecionados para processamento:");
            trimestres.forEach(t -> logger.info("  - {}", t));
            
            // Executa integração
            IntegracaoANSService service = new IntegracaoANSService(diretorioTrabalho);
            service.executarIntegracao(trimestres);
            
            logger.info("=== PARTE 1 CONCLUÍDA COM SUCESSO ===");
            
        } catch (IOException | InterruptedException e) {
            logger.error("Erro na execução da Parte 1", e);
            System.exit(1);
        }
    }
}
