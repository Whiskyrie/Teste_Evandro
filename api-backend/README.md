# API Backend - FastAPI

Backend REST API desenvolvido em FastAPI para consulta de operadoras de planos de saúde e suas despesas.

## 🚀 Instalação e Execução

### 1. Criar ambiente virtual

```bash
cd api-backend
python3 -m venv venv
source venv/bin/activate  # Linux/macOS
# ou
venv\Scripts\activate     # Windows
```

### 2. Instalar dependências

```bash
pip install -r requirements.txt
```

### 3. Configurar banco de dados

Certifique-se de que o PostgreSQL está rodando e o banco `teste_intuitive_care` foi criado (Parte 3).

**Opcional**: Configurar via variável de ambiente:

```bash
export DATABASE_URL="postgresql://postgres:senha@localhost:5432/teste_intuitive_care"
```

### 4. Executar servidor

```bash
# Método 1: Script Python
python run.py

# Método 2: Uvicorn direto
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

Servidor rodando em: **http://localhost:8000**

## 📚 Documentação da API

- **Swagger UI**: http://localhost:8000/docs
- **ReDoc**: http://localhost:8000/redoc
- **OpenAPI JSON**: http://localhost:8000/openapi.json

## 🔌 Endpoints

### Operadoras

- `GET /api/operadoras` - Lista todas as operadoras (paginado)
  - Query params: `page`, `limit`, `search`, `uf`
- `GET /api/operadoras/{cnpj}` - Detalhes de uma operadora
- `GET /api/operadoras/{cnpj}/despesas` - Histórico de despesas

### Estatísticas

- `GET /api/estatisticas` - Estatísticas agregadas do sistema

### Health Check

- `GET /` - Health check
- `GET /api/health` - Health check alternativo

## 🧪 Testar a API

### cURL

```bash
# Listar operadoras (página 1)
curl http://localhost:8000/api/operadoras

# Listar operadoras (página 2, 10 itens)
curl "http://localhost:8000/api/operadoras?page=2&limit=10"

# Buscar por razão social
curl "http://localhost:8000/api/operadoras?search=BRADESCO"

# Filtrar por UF
curl "http://localhost:8000/api/operadoras?uf=SP"

# Detalhes de operadora
curl http://localhost:8000/api/operadoras/27652773000143

# Despesas de operadora
curl http://localhost:8000/api/operadoras/27652773000143/despesas

# Estatísticas gerais
curl http://localhost:8000/api/estatisticas
```

### Python (requests)

```python
import requests

# Listar operadoras
response = requests.get("http://localhost:8000/api/operadoras", params={"page": 1, "limit": 20})
data = response.json()
print(f"Total: {data['total']} operadoras")
print(f"Página {data['page']} de {data['total_pages']}")

# Estatísticas
stats = requests.get("http://localhost:8000/api/estatisticas").json()
print(f"Total de despesas: R$ {stats['total_despesas']}")
```

## 📁 Estrutura do Projeto

```
api-backend/
├── app/
│   ├── main.py                 # Aplicação FastAPI
│   ├── database.py             # Conexão e sessão do banco
│   ├── models/
│   │   ├── database_models.py  # SQLAlchemy ORM models
│   │   └── schemas.py          # Pydantic schemas
│   └── routes/
│       ├── operadoras.py       # Rotas de operadoras
│       └── estatisticas.py     # Rotas de estatísticas
├── requirements.txt            # Dependências Python
├── run.py                      # Script para rodar servidor
└── README.md                   # Este arquivo
```

## 🧠 Trade-offs Técnicos Implementados

### 4.2.1. Framework: FastAPI
- ✅ **Escolhido**: FastAPI
- **Justificativa**: Performance, validação automática, documentação OpenAPI, async nativo

### 4.2.2. Paginação: Offset-based
- ✅ **Escolhido**: Offset-based (page + limit)
- **Justificativa**: Simples, ideal para dados relativamente estáticos (732 operadoras)

### 4.2.3. Estatísticas: Cálculo on-demand
- ✅ **Escolhido**: Calcular em tempo real (sem cache)
- **Justificativa**: Volume pequeno, dados atualizados raramente, query otimizada

### 4.2.4. Estrutura de Resposta: Dados + Metadados
- ✅ **Escolhido**: `{data: [...], total: X, page: Y, ...}`
- **Justificativa**: Frontend precisa de metadados para UI de paginação

## 🔒 Segurança

**MVP sem autenticação**. Para produção, implementar:
- JWT tokens ou OAuth2
- Rate limiting
- HTTPS obrigatório
- CORS restrito ao domínio do frontend

## 📝 Notas de Desenvolvimento

- Auto-reload ativado (desenvolvimento)
- CORS liberado para todas as origens (desenvolvimento)
- Logs estruturados com uvicorn
- Validação automática via Pydantic
- Documentação automática via OpenAPI
