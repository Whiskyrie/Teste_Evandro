package com.intuitivecare.teste.parte1.infrastructure;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intuitivecare.teste.parte1.domain.TrimestreANS;

/**
 * Cliente HTTP para interagir com a API de Dados Abertos da ANS.
 * Responsabilidade: download de arquivos e descoberta de estrutura de diretórios.
 */
public class ANSHttpClient {
    
    private static final Logger logger = LoggerFactory.getLogger(ANSHttpClient.class);
    private static final String BASE_URL = "https://dadosabertos.ans.gov.br/FTP/PDA/demonstracoes_contabeis";
    private static final Duration REQUEST_TIMEOUT = Duration.ofMinutes(5);
    
    private final HttpClient httpClient;
    
    public ANSHttpClient() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    }
    
    /**
     * Identifica os últimos N trimestres disponíveis no servidor da ANS.
     */
    public List<TrimestreANS> identificarUltimosTrimestreDisponiveis(int quantidade) throws IOException, InterruptedException {
        logger.info("Identificando últimos {} trimestres disponíveis...", quantidade);
        
        List<TrimestreANS> trimestresDisponiveis = new ArrayList<>();
        int anoAtual = 2025;
        
        // Busca nos últimos 2 anos
        for (int ano = anoAtual; ano >= anoAtual - 1 && trimestresDisponiveis.size() < quantidade * 2; ano--) {
            // Testa cada trimestre do ano (do 4º para o 1º)
            for (int trimestre = 4; trimestre >= 1; trimestre--) {
                String nomeArquivo = String.format("%dT%d.zip", trimestre, ano);
                String url = String.format("%s/%d/%s", BASE_URL, ano, nomeArquivo);
                
                try {
                    if (arquivoExiste(url)) {
                        trimestresDisponiveis.add(new TrimestreANS(ano, trimestre));
                        logger.info("Trimestre disponível: Q{}/{}", trimestre, ano);
                    }
                } catch (IOException | InterruptedException e) {
                    logger.debug("Trimestre Q{}/{} não encontrado", trimestre, ano);
                }
            }
        }
        
        // Ordena por ano e trimestre (mais recentes primeiro)
        trimestresDisponiveis.sort((t1, t2) -> {
            int cmpAno = Integer.compare(t2.ano(), t1.ano());
            if (cmpAno != 0) return cmpAno;
            return Integer.compare(t2.trimestre(), t1.trimestre());
        });
        
        // Retorna apenas os N mais recentes
        List<TrimestreANS> resultado = trimestresDisponiveis.stream()
            .limit(quantidade)
            .toList();
        
        logger.info("Identificados {} trimestres disponíveis: {}", resultado.size(), resultado);
        return resultado;
    }
    
    /**
     * Lista arquivos ZIP disponíveis em um trimestre específico.
     * Estrutura da ANS: /demonstracoes_contabeis/YYYY/QT20YY.zip
     */
    public List<String> listarArquivosZip(int ano, int trimestre) throws IOException, InterruptedException {
        List<String> arquivos = new ArrayList<>();
        
        // Formato direto: https://dadosabertos.ans.gov.br/FTP/PDA/demonstracoes_contabeis/2024/3T2024.zip
        String nomeArquivo = String.format("%dT%d.zip", trimestre, ano);
        String url = String.format("%s/%d/%s", BASE_URL, ano, nomeArquivo);
        
        // Verifica se o arquivo existe
        try {
            if (arquivoExiste(url)) {
                logger.info("Arquivo ZIP encontrado: {}", url);
                arquivos.add(url);
            } else {
                logger.warn("Arquivo não encontrado: {}", url);
            }
        } catch (IOException | InterruptedException e) {
            logger.warn("Erro ao verificar arquivo {}: {}", url, e.getMessage());
        }
        
        return arquivos;
    }
    
    /**
     * Verifica se um arquivo existe fazendo uma requisição HEAD.
     */
    private boolean arquivoExiste(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .method("HEAD", HttpRequest.BodyPublishers.noBody())
            .timeout(Duration.ofSeconds(10))
            .build();
        
        HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        return response.statusCode() == 200;
    }
    
    /**
     * Baixa um arquivo da URL especificada para o diretório de destino.
     */
    public Path baixarArquivo(String url, Path destinoDir) throws IOException, InterruptedException {
        logger.info("Baixando arquivo: {}", url);
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(REQUEST_TIMEOUT)
            .GET()
            .build();
        
        HttpResponse<InputStream> response = httpClient.send(
            request, 
            HttpResponse.BodyHandlers.ofInputStream()
        );
        
        if (response.statusCode() != 200) {
            throw new IOException("Erro ao baixar arquivo. Status: " + response.statusCode());
        }
        
        // Extrai nome do arquivo da URL
        String nomeArquivo = extrairNomeArquivo(url);
        Path destino = destinoDir.resolve(nomeArquivo);
        
        Files.createDirectories(destinoDir);
        
        try (InputStream in = response.body()) {
            Files.copy(in, destino, StandardCopyOption.REPLACE_EXISTING);
        }
        
        logger.info("Arquivo baixado com sucesso: {}", destino);
        return destino;
    }
    
    private String extrairNomeArquivo(String url) {
        String[] partes = url.split("/");
        return partes[partes.length - 1];
    }
}
