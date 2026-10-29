# Guia de Implantação em Produção — RemoteDesk / DoHsktoP

Este manual orienta a instalação e inicialização da infraestrutura do RemoteDesk em um VPS público para viabilizar conexões remotas seguras pela Internet, mesmo quando o celular Android estiver na rede móvel 4G/5G e o PC em uma rede doméstica com CGNAT.

---

## 1. Pré-Requisitos

1. **Um Servidor VPS** com IP público dedicado (ex: Ubuntu 22.04 ou 24.04 LTS).
   - Mínimo: 1 vCPU, 1 GB de RAM, 20 GB de disco.
   - Provedores suportados: Oracle Cloud (Always Free), Hetzner, DigitalOcean, Linode, AWS.
2. **Um Domínio ou Subdomínio DNS** (ex: `remotedesk.meudominio.com`).
   - Crie uma entrada DNS tipo `A` apontando o subdomínio para o IP público do seu VPS.
3. **Docker e Docker Compose** instalados no VPS:
   ```bash
   curl -fsSL https://get.docker.com | sh
   sudo usermod -aG docker $USER
   ```

---

## 2. Passo a Passo de Instalação no VPS

### Passo 2.1 — Clonar o Repositório no VPS
```bash
git clone https://github.com/SEU_USUARIO/RemoteDesk.git
cd RemoteDesk/deployment
```

### Passo 2.2 — Configurar Variáveis de Ambiente
Copie o modelo de ambiente e edite os valores:
```bash
cp .env.example .env
nano .env
```

Preencha os seguintes parâmetros:
```ini
DOMAIN_NAME=remotedesk.meudominio.com
PUBLIC_IP_OR_DOMAIN=203.0.113.10
ACME_EMAIL=seu-email@gmail.com
TURN_SECRET=coloque_aqui_uma_chave_aleatoria_longa
SIGNALING_PORT=5000
```
> *Dica para gerar uma chave aleatória segura:* `openssl rand -hex 32`

### Passo 2.3 — Configurar o Firewall do VPS
Execute as regras detalhadas em `FIREWALL_RULES.md`:
```bash
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw allow 3478/udp
sudo ufw allow 3478/tcp
sudo ufw allow 5349/tcp
sudo ufw allow 49152:49200/udp
sudo ufw reload
```

### Passo 2.4 — Iniciar os Serviços
Execute o script de automação:
```bash
./deploy.sh
```
O Docker construirá o servidor de sinalização em .NET 8, inicializará o servidor Coturn (TURN/STUN) e o proxy Caddy com provisionamento automático de certificado SSL/TLS gratuito Let's Encrypt.

---

## 3. Validação da Infraestrutura

Para verificar se todos os serviços estão saudáveis:
```bash
./verify_deployment.sh
```
Ou teste diretamente pelo navegador / terminal do seu computador:
```bash
curl -i https://remotedesk.meudominio.com/health
```
Resposta esperada:
```json
{
  "status": "Healthy",
  "service": "RemoteDesk Signaling Server",
  "version": "1.0.0",
  "stage": "8/12",
  "activeHosts": 0,
  "activeClients": 0,
  "timestamp": "2026-10-25T..."
}
```

---

## 4. Conectando os Dispositivos ao Servidor Público

### No Computador Windows (Host Agent):
Abra o arquivo `windows-agent/src/RemoteDesk.Agent/appsettings.json` (ou configure a variável de ambiente):
```json
{
  "RemoteDeskAgent": {
    "SignalingServerUrl": "https://remotedesk.meudominio.com"
  }
}
```
Inicie o agente:
```powershell
dotnet run --project windows-agent/src/RemoteDesk.Agent/RemoteDesk.Agent.csproj
```
O console exibirá:
```
======================================================================
  RemoteDesk Windows Host Agent v1.0.0
======================================================================
Dispositivo Host:           MEU-PC (ID: D4E5F6)
PIN DE ACESSO / PAREAMENTO: 492 813  <=== DIGITE ESTE PIN NO APP ANDROID
Servidor de Sinalização:    https://remotedesk.meudominio.com
[SINALIZAÇÃO] Conectado ao servidor de sinalização com sucesso. Registrando Host...
[SINALIZAÇÃO] Registro de Host confirmado pelo servidor de sinalização.
```

### No Celular Android:
1. Abra o aplicativo DoHsktoP / RemoteDesk.
2. Expanda **"Configurações Avançadas de Servidor e Rede"**.
3. No campo **Servidor de Sinalização**, insira: `https://remotedesk.meudominio.com`.
4. No campo **PIN de Pareamento / Acesso**, digite o PIN de 6 dígitos exibido no console do Windows Agent (ex: `492 813`).
5. Toque em **Conectar ao Computador**.
6. O aplicativo se conecta através do servidor de sinalização, valida o PIN, emite e salva o token persistente no Android Keystore.
7. Nas próximas vezes, toque em **Conectar ao Computador** e o acesso ocorrerá automaticamente com 1 clique de qualquer lugar do mundo!
