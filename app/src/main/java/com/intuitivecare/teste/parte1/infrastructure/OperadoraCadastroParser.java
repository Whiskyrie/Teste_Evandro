package com.intuitivecare.teste.parte1.infrastructure;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intuitivecare.teste.parte1.domain.OperadoraCadastro;

/**
 * Parser para arquivos de dados cadastrais de operadoras.
 */
public class OperadoraCadastroParser {
    
    private static final Logger logger = LoggerFactory.getLogger(OperadoraCadastroParser.class);
    
    /**
     * Carrega dados cadastrais de operadoras em um mapa indexado por REG_ANS.
     */
    public Map<String, OperadoraCadastro> carregarCadastro(Path arquivo) throws IOException {
        logger.info("Carregando dados cadastrais de: {}", arquivo);
        
        Map<String, OperadoraCadastro> cadastro = new HashMap<>();
        CSVFormat format = detectarFormato(arquivo);
        
        try (BufferedReader reader = Files.newBufferedReader(arquivo, StandardCharsets.UTF_8);
             CSVParser parser = new CSVParser(reader, format)) {
            
            Map<String, Integer> headers = parser.getHeaderMap();
            logger.debug("Headers detectados: {}", headers.keySet());
            
            for (CSVRecord record : parser) {
                try {
                    OperadoraCadastro operadora = parsearRegistro(record, headers);
                    if (operadora != null) {
                        cadastro.put(operadora.registroANS(), operadora);
                    }
                } catch (Exception e) {
                    logger.warn("Erro ao processar linha {}: {}", record.getRecordNumber(), e.getMessage());
                }
            }
        }
        
        logger.info("Carregados {} registros cadastrais", cadastro.size());
        return cadastro;
    }
    
    private CSVFormat detectarFormato(Path arquivo) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(arquivo, StandardCharsets.UTF_8)) {
            String primeiraLinha = reader.readLine();
            
            if (primeiraLinha == null) {
                throw new IOException("Arquivo vazio: " + arquivo);
            }
            
            char delimitador = ';';
            if (primeiraLinha.contains(",") && !primeiraLinha.contains(";")) {
                delimitador = ',';
            } else if (primeiraLinha.contains("\t")) {
                delimitador = '\t';
            }
            
            return CSVFormat.DEFAULT.builder()
                .setDelimiter(delimitador)
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .setQuote('"')
                .setIgnoreSurroundingSpaces(true)
                .build();
        }
    }
    
    private OperadoraCadastro parsearRegistro(CSVRecord record, Map<String, Integer> headers) {
        String regANS = buscarValorColuna(record, headers, "registro_operadora", "registro_ans", "reg_ans", "cd_operadora");
        String cnpj = buscarValorColuna(record, headers, "cnpj", "nr_cnpj", "cd_cnpj");
        String razaoSocial = buscarValorColuna(record, headers, "razao_social", "nm_razao_social", "razaosocial");
        String modalidade = buscarValorColuna(record, headers, "modalidade", "nm_modalidade", "cd_modalidade");
        String uf = buscarValorColuna(record, headers, "uf", "sg_uf", "uf_operadora");
        
        if (regANS == null) {
            return null;
        }
        
        // Limpa CNPJ
        if (cnpj != null) {
            cnpj = cnpj.replaceAll("[^0-9]", "");
            if (cnpj.length() != 14) {
                cnpj = null; // CNPJ inválido
            }
        }
        
        return new OperadoraCadastro(
            regANS.trim(),
            cnpj,
            razaoSocial != null ? razaoSocial.trim() : "NÃO INFORMADA",
            modalidade != null ? modalidade.trim() : "NÃO INFORMADA",
            uf != null ? uf.trim() : "NÃO INFORMADA"
        );
    }
    
    private String buscarValorColuna(CSVRecord record, Map<String, Integer> headers, String... nomesPossiveis) {
        for (String nome : nomesPossiveis) {
            for (Map.Entry<String, Integer> entry : headers.entrySet()) {
                if (entry.getKey().toLowerCase().contains(nome.toLowerCase())) {
                    String valor = record.get(entry.getValue());
                    if (valor != null && !valor.trim().isEmpty()) {
                        return valor;
                    }
                }
            }
        }
        return null;
    }
}
