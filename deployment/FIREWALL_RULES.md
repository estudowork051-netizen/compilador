# Regras de Firewall e Portas de Rede — RemoteDesk

Este documento detalha todas as portas, protocolos e regras de firewall necessárias para implantar a infraestrutura de sinalização e relé WebRTC do RemoteDesk em um servidor VPS público (Oracle Cloud, DigitalOcean, Hetzner, AWS, etc.).

---

## 1. Tabela de Portas e Protocolos

| Porta / Faixa | Protocolo | Direção | Serviço / Componente | Finalidade |
|---|---|---|---|---|
| **80** | TCP | Entrada (Inbound) | Caddy / Nginx | Redirecionamento HTTP para HTTPS e validação de certificados SSL Let's Encrypt via ACME. |
| **443** | TCP | Entrada (Inbound) | Caddy / Nginx | Acesso público seguro HTTPS e WebSockets WSS (`/ws/signaling`). |
| **3478** | UDP | Entrada (Inbound) | Coturn (STUN/TURN) | Resolução de candidatos ICE STUN e tráfego de relé WebRTC UDP. |
| **3478** | TCP | Entrada (Inbound) | Coturn (TURN) | Fallback de relé TURN sobre TCP quando a rede móvel bloquear UDP. |
| **5349** | TCP | Entrada (Inbound) | Coturn (TURNS) | Conexão TURN segura criptografada sobre TLS. |
| **49152–49200** | UDP | Entrada (Inbound) | Coturn (Relay Range) | Faixa dinâmica para retransmissão de quadros de vídeo e áudio P2P. |
| **5050** *(Local)* | TCP | Local (Host PC) | Windows Agent | Comunicação direta exclusiva na rede local (LAN). **Não expor à internet.** |

---

## 2. Comandos para Configuração no Linux (UFW)

Se você utiliza Ubuntu/Debian no VPS com `ufw` ativo:

```bash
# Permitir acesso web seguro e sinalização
sudo ufw allow 80/tcp comment 'RemoteDesk HTTP / ACME'
sudo ufw allow 443/tcp comment 'RemoteDesk HTTPS / WSS'

# Permitir STUN e TURN padrão
sudo ufw allow 3478/udp comment 'RemoteDesk STUN/TURN UDP'
sudo ufw allow 3478/tcp comment 'RemoteDesk TURN TCP'
sudo ufw allow 5349/tcp comment 'RemoteDesk TURNS TLS'

# Permitir faixa de relé WebRTC
sudo ufw allow 49152:49200/udp comment 'RemoteDesk Coturn Relay Range'

# Recarregar regras
sudo ufw reload
sudo ufw status verbose
```

---

## 3. Configuração em Provedores de Nuvem

### Oracle Cloud Infrastructure (OCI)
1. Acesse **Networking** > **Virtual Cloud Networks** > **Sua VCN** > **Security Lists**.
2. Adicione as **Ingress Rules**:
   - `0.0.0.0/0`, TCP, Portas de Destino: `80,443,3478,5349`
   - `0.0.0.0/0`, UDP, Portas de Destino: `3478,49152-49200`
3. No terminal da instância Linux do Oracle Cloud, libere o iptables do sistema:
   ```bash
   sudo iptables -I INPUT 6 -m state --state NEW -p tcp -m multiport --dports 80,443,3478,5349 -j ACCEPT
   sudo iptables -I INPUT 6 -m state --state NEW -p udp --dport 3478 -j ACCEPT
   sudo iptables -I INPUT 6 -m state --state NEW -p udp --match multiport --dports 49152:49200 -j ACCEPT
   sudo netfilter-persistent save
   ```

### AWS (EC2 Security Groups)
- Crie regras de entrada para o Security Group associado à instância EC2 correspondendo à tabela acima.

---

## 4. Como o RemoteDesk Atravessa CGNAT e Redes Móveis (4G/5G)

1. Quando o celular Android está na rua (4G/5G) e o PC está em casa, ambos conectam-se ao servidor público por WSS (`wss://seu-dominio.com/ws/signaling`).
2. O servidor fornece credenciais efêmeras de TURN (`use-auth-secret`).
3. O Android e o Windows Agent realizam a coleta de candidatos ICE (Host, Server Reflexive via STUN e Relay via TURN).
4. Caso o roteador da residência ou a operadora 4G/5G utilize NAT Simétrico ou CGNAT (impedindo conexão P2P direta), a conexão transita automaticamente pelo relé TURN na porta 3478/UDP (ou 3478/TCP), garantindo 100% de conectividade.
