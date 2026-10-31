# MoneyGuard API

API do **MoneyGuard**, um app de controle financeiro pessoal: rendas, gastos, categorias, metas e insights por período.

## Stack
- Java 21, Spring Boot 4
- Spring Security (JWT HS256, stateless)
- PostgreSQL + Flyway
- OpenAPI / Swagger UI em `/swagger-ui.html`

## Privacidade dos dados
Valores financeiros e textos livres (descrições, notas) são cifrados pela aplicação com **AES-256-GCM** antes de chegar ao banco (colunas `*_enc`). Quem consulta o banco direto vê só bytes. Datas, tipos e chaves ficam em claro para os filtros por período funcionarem.

## Variáveis de ambiente
| Variável | Descrição |
|---|---|
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` | Conexão com o PostgreSQL (schema `moneyguard`) |
| `JWT_SECRET` | Base64, mínimo 32 bytes |
| `JWT_TTL` | Validade do token (ISO-8601, padrão `PT12H`) |
| `CRYPTO_KEY` | Base64 de exatamente 32 bytes (AES-256). **Se perder, os dados cifrados não voltam.** |
| `CORS_ORIGINS` | Origens permitidas, separadas por vírgula |

Gerar uma chave: `openssl rand -base64 32`

## Rodando local
```bash
mvn test              # testes (Postgres embarcado, sem Docker)
mvn spring-boot:run   # precisa das variáveis acima
```

## Endpoints atuais
| Método | Rota | Auth |
|---|---|---|
| POST | `/api/auth/register` | não |
| POST | `/api/auth/login` | não |
| GET | `/api/auth/me` | Bearer |
| GET | `/actuator/health` | não |
