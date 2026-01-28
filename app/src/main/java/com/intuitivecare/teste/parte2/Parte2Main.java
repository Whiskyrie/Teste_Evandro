package com.intuitivecare.teste.parte2;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intuitivecare.teste.parte2.application.TransformacaoValidacaoService;

/**
 * Main class para executar a Parte 2: Transformação e Validação de Dados.
 */
public class Parte2Main {
    
    private static final Logger logger = LoggerFactory.getLogger(Parte2Main.class);
    
    public static void main(String[] args) {
        logger.info("=== PARTE 2: TRANSFORMAÇÃO E VALIDAÇÃO DE DADOS ===");
        
        try {
            // Diretórios
            Path diretorioTrabalho = Paths.get("dados/parte2");
            Path csvConsolidado = Paths.get("dados/parte1/consolidado_despesas.csv");
            Path csvCadastro = Paths.get("dados/parte1/cadastro/Relatorio_cadop.csv");
            
            // Validar que arquivos existem
            if (!csvConsolidado.toFile().exists()) {
                logger.error("Arquivo consolidado não encontrado: {}", csvConsolidado);
                logger.info("Execute primeiro a Parte 1 para gerar os dados consolidados");
                System.exit(1);
            }
            
            if (!csvCadastro.toFile().exists()) {
                logger.error("Arquivo de cadastro não encontrado: {}", csvCadastro);
                System.exit(1);
            }
            
            // Executar transformação
            TransformacaoValidacaoService service = new TransformacaoValidacaoService(diretorioTrabalho);
            service.executarTransformacao(csvConsolidado, csvCadastro);
            
            logger.info("=== PARTE 2 CONCLUÍDA COM SUCESSO ===");
            logger.info("Arquivos gerados:");
            logger.info("  - dados/parte2/despesas_enriquecidas.csv");
            logger.info("  - dados/parte2/despesas_agregadas.csv");
            
        } catch (IOException e) {
            logger.error("Erro na execução da Parte 2", e);
            System.exit(1);
        }
    }
}
