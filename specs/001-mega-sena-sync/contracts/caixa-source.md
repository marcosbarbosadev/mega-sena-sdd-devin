# Contrato da Fonte Externa (Caixa — Portal de Loterias)

**Feature**: 001-mega-sena-sync | **Date**: 2026-05-31

Contrato de consumo da fonte oficial. Este é um contrato **externo** (não controlamos a
API); o cliente deve ser defensivo (validar antes de mapear) conforme FR-010.

## Requisição

- **Último concurso**:
  `GET https://servicebus2.caixa.gov.br/portaldeloterias/api/megasena`
- **Concurso específico**:
  `GET https://servicebus2.caixa.gov.br/portaldeloterias/api/megasena/{numero}`
- **Headers**:
  - `User-Agent: <navegador>` (obrigatório — a fonte recusa requisições sem ele)
  - `Accept: application/json`
- **TLS**: cadeia completa do domínio `servicebus2.caixa.gov.br` (não desabilitar
  verificação).
- **Timeout**: conexão e leitura explícitos (configuráveis).
- **Base URL configurável**: permite apontar para dublê (WireMock) em teste e para um
  fallback (ex.: BrasilAPI) se habilitado.

## Resposta (campos consumidos)

A fonte retorna muitos campos; consumimos apenas os necessários ao escopo:

| Campo na fonte | Uso | Mapeia para |
|----------------|-----|-------------|
| `numero` | número do concurso | `Concurso.numero` |
| `dataApuracao` (`dd/MM/yyyy`) | data do sorteio | `Concurso.dataSorteio` |
| `listaDezenas` (array de strings `"01".."60"`) | dezenas sorteadas | `Concurso.dezenas` (parse para inteiros) |
| `listaRateioPremio[].valorPremio` da faixa "Sena" | valor do prêmio | `Concurso.valorPremio` |

Campos ignorados (fora de escopo): `acumulado`, `valorAcumuladoProximoConcurso`,
`valorEstimadoProximoConcurso`, `dataProximoConcurso`, ganhadores por faixa/UF, etc.

### Exemplo (resumido) de payload

```json
{
  "numero": 2700,
  "dataApuracao": "29/05/2024",
  "listaDezenas": ["04", "17", "23", "38", "51", "60"],
  "listaRateioPremio": [
    { "descricaoFaixa": "Sena", "valorPremio": 52000000.00 }
  ]
}
```

## Regras de validação ao mapear (FR-010)

Rejeitar e **registrar** (sem corromper dados existentes) quando:

- `numero` ausente ou ≤ 0;
- `dataApuracao` ausente ou em formato inválido;
- `listaDezenas` não contém **exatamente 6** dezenas distintas entre 1 e 60;
- não há faixa "Sena" com `valorPremio` ≥ 0.

## Modos de falha esperados (FR-007/FR-009)

| Cenário | Tratamento |
|---------|-----------|
| Timeout / 5xx / indisponível | Retry com backoff (Resilience4j); ao esgotar, registra `SyncRun` FALHA/PARCIAL e mantém o cache local servindo leituras. |
| Payload malformado/incompleto | Concurso rejeitado e registrado; não sobrescreve dados válidos já armazenados. |
| Concurso ainda não sorteado | Fonte não retorna resultado válido → nada é criado. |
| Concurso já armazenado e idêntico | Upsert idempotente — nenhuma alteração. |
