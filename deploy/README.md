# Deploy no VPS KVM da Hostinger

Backend em container (imagem do CI no GHCR) atrás do Caddy, que cuida do HTTPS e bloqueia `/actuator/*`.

## 1. Domínio e DNS (hPanel)

1. Em **Domínios**, compre ou selecione o domínio.
2. Em **DNS / Zona DNS**, apague registros `A` e `AAAA` antigos de `@` (a Hostinger cria um apontando para a página de estacionamento) e crie:

   | Tipo | Nome  | Valor       |
   |------|-------|-------------|
   | A    | `@`   | IP do VPS   |
   | A    | `app` | IP do VPS (só quando houver front) |

3. Confira a propagação antes de subir o Caddy: `dig +short seu-dominio.com` deve retornar o IP do VPS.

## 2. VPS

1. Ao contratar o **VPS KVM** (2 GB de RAM bastam), escolha o template **Ubuntu 24.04 com Docker** (ou Ubuntu puro e instale o Docker Engine + plugin compose).
2. No hPanel, crie as regras de firewall: liberar 22, 80 e 443; bloquear o resto.
3. Acesse como root uma vez e prepare o servidor:

   ```bash
   adduser deploy && usermod -aG sudo,docker deploy
   # copie sua chave pública para /home/deploy/.ssh/authorized_keys
   # em /etc/ssh/sshd_config: PasswordAuthentication no e PermitRootLogin no
   systemctl restart ssh
   ufw default deny incoming && ufw allow OpenSSH && ufw allow 80/tcp && ufw allow 443/tcp && ufw enable
   apt install -y unattended-upgrades
   ```

## 3. Arquivos no servidor

```bash
# da sua máquina
scp -r deploy/ deploy@IP_DO_VPS:/opt/shortener
# no servidor
cd /opt/shortener
cp .env.example .env && chmod 600 .env
nano .env    # DOMAIN, IMAGE_TAG, DATABASE_*
```

A imagem é publicada em `ghcr.io/<dono>/<repositório>` (hoje `marcuswittho/comulink`, pelo nome do repositório no GitHub). Pacotes do GHCR são privados por padrão. Faça login no servidor ou torne o pacote público:

```bash
docker login ghcr.io -u SEU_USUARIO   # senha: token (PAT) com read:packages
```

## 4. Subir

```bash
docker compose pull
docker compose up -d
docker compose ps          # app deve ficar "healthy"
docker compose logs -f app
```

## 5. Smoke test

```bash
curl -i -X POST https://seu-dominio.com/api/urls -H 'Content-Type: application/json' -d '{"originalUrl":"https://example.com"}'   # 201
curl -i https://seu-dominio.com/<shortCode>              # 302
curl -i https://seu-dominio.com/<codigo-inexistente>     # 404
curl -i https://seu-dominio.com/actuator/prometheus      # 404 (bloqueado)
```

## Deploy de nova versão e rollback

Mude `IMAGE_TAG` no `.env` e rode `docker compose pull && docker compose up -d`. Rollback: volte a tag anterior e rode o mesmo comando. Antes de mudanças de modelo, confirme que o schema é compatível com a versão anterior.

## Notas

- Só uma instância da aplicação: o rate limit é em memória, por instância.
- Backups do banco e teste de restauração continuam pendentes (ver `PRODUCAO.md`, seção 3).
- Front: descomente o bloco `app.{$DOMAIN}` no `Caddyfile`, monte o build em `/srv/front` no serviço `caddy` e crie o registro `A` `app`.
