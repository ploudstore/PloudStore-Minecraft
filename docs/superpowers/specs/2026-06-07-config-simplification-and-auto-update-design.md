# Design: Config Simplification + Auto-Update System

**Date:** 2026-06-07
**Repo:** ploudstore/PloudStore-Minecraft

---

## Objetivo

1. Simplificar o `config.yml` para só expor o que o server admin precisa de configurar: `secret-key` e `messages`.
2. Adicionar um sistema de auto-update que verifica o GitHub no startup, descarrega o novo JAR automaticamente e notifica consola e admins.

---

## Parte 1 — Simplificação da Config

### O que muda

O `config.yml` passa a ter apenas:

```yaml
secret-key: "your-secret-key-here"

messages:
  not-enough-slots: "&c[Loja] Precisa de {slots} slot(s) livre(s) no inventario para receber a sua compra. Tem apenas {free}."
```

As opções removidas do config tornam-se constantes internas no código:

| Opção antiga              | Valor hardcoded |
|---------------------------|-----------------|
| `api.base-url`            | `https://command.ploudstore.com` |
| `api.fallback-next-check-seconds` | `60` |
| `api.http-timeout-seconds` | `10` |
| `api.http-max-retries`    | `3` |

### Ficheiros afetados

- `src/main/resources/config.yml` — remover as opções acima
- `PloudStorePlugin.java` (`startCommandProcessor`) — deixar de ler essas chaves do config, usar constantes diretamente
- `ApiClient.java` — pode manter a assinatura atual (as constantes são passadas no construtor), ou receber-as como constantes de uma classe `PluginConstants`

---

## Parte 2 — Sistema de Auto-Update

### Arquitetura

Nova classe: `org.ploudstore.ploudStorePlugin.updater.UpdateChecker`

Responsabilidades únicas: verificar versão, descarregar JAR, guardar no update folder.

### Fluxo

```
onEnable()
  └─ UpdateChecker.checkAsync()
       ├─ GET https://api.github.com/repos/ploudstore/PloudStore-Minecraft/releases/latest
       ├─ Extrair tag_name (ex: "v1.2.0") → strip "v" → "1.2.0"
       ├─ Comparar com plugin.getDescription().getVersion() ("1.0.0")
       ├─ Se igual ou inferior → sem ação
       └─ Se superior →
            ├─ Fazer download do primeiro asset .jar
            ├─ Guardar em plugins/update/<jar-atual>.jar
            ├─ Log console: "Update v1.0.0 → v1.2.0 descarregado. Será aplicado no próximo restart."
            └─ Guardar flag updateAvailable=true + versão nova em memória
```

### Notificação a admins

No `PlayerJoinListener` existente: se `updateAvailable == true` e o jogador tem permissão `ploudstore.admin`, envia mensagem:
```
[PloudStore] Nova versão v1.2.0 disponível! Reinicia o servidor para aplicar o update.
```

### Localização do update folder

Usar `Bukkit.getUpdateFolderFile()` — método nativo do Bukkit que devolve a pasta `plugins/update/`, criando-a se necessário. O ficheiro de destino tem o mesmo nome do JAR em execução, obtido via:
```java
getClass().getProtectionDomain().getCodeSource().getLocation().toURI()
```

### Comparação de versões

Comparação semântica simples: split por `.`, comparar inteiros major/minor/patch. Suficiente para versões no formato `X.Y.Z`.

### Tratamento de falhas

Qualquer falha (timeout, JSON inválido, download falhado, sem assets .jar) regista `WARNING` no console e o plugin continua a funcionar normalmente. O update não é bloqueante.

### Classe nova

```
src/main/java/org/ploudstore/ploudStorePlugin/updater/UpdateChecker.java
```

### Dependências

Sem novas dependências Maven. Usa `HttpClient` (Java 11+, já presente), `Gson` (já incluído via shade), e `Bukkit.getUpdateFolderFile()` (API Paper).

---

## Ficheiros a modificar/criar

| Ficheiro | Ação |
|----------|------|
| `config.yml` | Remover 4 opções, manter só `secret-key` e `messages` |
| `PloudStorePlugin.java` | Usar constantes em vez de config; chamar `UpdateChecker.checkAsync()` no `onEnable()` |
| `ApiClient.java` | Constantes hardcoded em vez de recebidas do config (ou classe `PluginConstants`) |
| `PlayerJoinListener.java` | Adicionar notificação de update a admins |
| `updater/UpdateChecker.java` | Criar de raiz |

---

## Fora do scope

- Opção para desligar auto-update (sempre ativo)
- Checks periódicos (só no startup)
- Rollback de versão
