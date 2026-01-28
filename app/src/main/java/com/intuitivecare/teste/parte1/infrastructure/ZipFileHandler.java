package com.intuitivecare.teste.parte1.infrastructure;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manipulador de arquivos ZIP.
 * Responsabilidade: extração de arquivos ZIP e identificação de arquivos relevantes.
 */
public class ZipFileHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(ZipFileHandler.class);
    
    /**
     * Extrai um arquivo ZIP para o diretório de destino.
     * Retorna lista de arquivos extraídos.
     */
    public List<Path> extrairZip(Path arquivoZip, Path destinoDir) throws IOException {
        logger.info("Extraindo arquivo ZIP: {}", arquivoZip);
        
        List<Path> arquivosExtraidos = new ArrayList<>();
        Files.createDirectories(destinoDir);
        
        try (ZipFile zipFile = ZipFile.builder().setPath(arquivoZip).get()) {
            Enumeration<ZipArchiveEntry> entries = zipFile.getEntries();
            
            while (entries.hasMoreElements()) {
                ZipArchiveEntry entry = entries.nextElement();
                
                if (entry.isDirectory()) {
                    continue;
                }
                
                Path destino = destinoDir.resolve(entry.getName());
                Files.createDirectories(destino.getParent());
                
                try (InputStream in = zipFile.getInputStream(entry)) {
                    Files.copy(in, destino, StandardCopyOption.REPLACE_EXISTING);
                    arquivosExtraidos.add(destino);
                }
            }
        }
        
        logger.info("Extraídos {} arquivos de: {}", arquivosExtraidos.size(), arquivoZip);
        return arquivosExtraidos;
    }
    
    /**
     * Identifica se um arquivo é de despesas com eventos/sinistros.
     * Para demonstrações contábeis, processa todos os CSVs.
     */
    public boolean isDespesasEventosSinistros(Path arquivo) {
        String nomeArquivo = arquivo.getFileName().toString().toLowerCase();
        
        // Aceita todos os arquivos CSV/TXT como potencialmente relevantes
        // O parser identificará se há dados de despesas dentro
        return nomeArquivo.endsWith(".csv") || nomeArquivo.endsWith(".txt");
    }
    
    /**
     * Filtra lista de arquivos para manter apenas os relevantes (despesas).
     */
    public List<Path> filtrarArquivosDespesas(List<Path> arquivos) {
        List<Path> arquivosFiltrados = new ArrayList<>();
        
        logger.info("Analisando {} arquivos extraídos:", arquivos.size());
        for (Path arquivo : arquivos) {
            logger.info("  - Arquivo: {}", arquivo.getFileName());
            if (isDespesasEventosSinistros(arquivo)) {
                arquivosFiltrados.add(arquivo);
                logger.info("    ✓ SELECIONADO como arquivo de despesas");
            } else {
                logger.info("    ✗ Ignorado (não corresponde aos critérios)");
            }
        }
        
        logger.info("Filtrados {} arquivos de despesas de um total de {}", 
            arquivosFiltrados.size(), arquivos.size());
        
        return arquivosFiltrados;
    }
}
