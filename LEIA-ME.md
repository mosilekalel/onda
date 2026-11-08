# Mesh Livre — 0.1.0 alpha
Criado para Ludvicx • 8 de novembro de 2026

Projeto Android nativo, Java, para experimentar mensagens sem internet com retransmissão entre celulares. Nome provisório. Este ZIP contém código-fonte, não um APK pronto. É um protótipo experimental, não um serviço de comunicação de emergência.

## O que está implementado
- Chat coletivo em português, apelido e identidade local persistentes.
- Bluetooth Classic RFCOMM seguro com pareamento do Android.
- Descoberta de aparelhos, visibilidade temporária e conexão manual.
- Até quatro vizinhos diretos por aparelho no código; a capacidade real depende do hardware.
- Retransmissão por flooding, UUID por mensagem, deduplicação e limite de oito saltos.
- Fila em memória de até 500 mensagens por dez minutos, reapresentada ao conectar novos vizinhos. Mensagens antigas expiram por relógio monotônico local. Não há garantia de entrega.
- Pacotes limitados a 8 KiB; mensagens limitadas a 1.000 caracteres.
- Sem servidor, conta, SIM obrigatório, dependência Google Play Services ou permissão INTERNET.
- Workflow para gerar APK de teste pelo GitHub Actions.

## Comece pelo celular: gerar APK no GitHub
É necessário internet apenas para enviar o projeto, baixar dependências e gerar/baixar o APK. Depois de instalado, o chat usa Bluetooth.

1. Extraia este ZIP no gerenciador de arquivos do celular.
2. Crie um repositório no GitHub, preferencialmente privado durante os testes.
3. Envie **o conteúdo da pasta MeshLivre**, não o ZIP nem uma pasta MeshLivre extra. A raiz precisa conter `settings.gradle`, `build.gradle`, `app/`, `tests/` e `.github/`.
4. Garanta que o arquivo oculto `.github/workflows/android.yml` foi enviado. Se o gerenciador ocultar essa pasta, crie o arquivo pelo editor do GitHub, copiando o conteúdo do ZIP.
5. Faça commit na branch main. Abra **Actions → Gerar APK Android**. Se necessário, habilite Actions no repositório.
6. Execute **Run workflow** (ou aguarde a execução iniciada pelo commit).
7. Depois do resultado verde, abra a execução e baixe **MeshLivre-APK-debug** em Artifacts. Você deve estar conectado ao GitHub para baixar.
8. Extraia o artefato e instale `app-debug.apk` em todos os celulares. O Android pode pedir autorização para instalar a partir do navegador ou gerenciador de arquivos.

Se a execução ficar vermelha, abra a etapa que falhou e copie o erro. O workflow está preparado, mas não foi executado nesta entrega. Políticas e cotas do GitHub podem limitar sua execução. APKs debug de execuções diferentes podem ter assinaturas diferentes; nesse caso desinstale a versão antiga antes de instalar, perdendo a identidade local.

## Alternativa: computador com Android Studio
Use JDK 17, SDK Android 35, Android Gradle Plugin 8.7.3 e Gradle 8.9. Essa combinação foi escolhida por compatibilidade documentada, não por ser a mais recente.

Este pacote não inclui o binário Gradle Wrapper. Instale Gradle 8.9 e, na raiz, rode:

```sh
gradle wrapper --gradle-version 8.9
```

Abra a pasta no Android Studio e sincronize. Instale SDK 35/Build Tools quando solicitado e selecione JDK 17 para o Gradle. O Studio cria `local.properties` com o caminho do SDK. Pelo terminal, com SDK configurado:

```sh
./gradlew assembleDebug lintDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`. No Windows use `gradlew.bat`. O workflow do GitHub instala Gradle diretamente e não depende do Wrapper.

## Teste real com três celulares A, B e C
1. Instale o mesmo APK nos três aparelhos Android 8.0 ou superior.
2. Desative Wi-Fi e dados móveis; mantenha Bluetooth ligado. No modo avião, reative Bluetooth manualmente.
3. Abra o app e escolha apelidos diferentes: A, B e C.
4. Toque em **Iniciar** em todos e conceda as permissões. No Android 8–11, a descoberta também pode exigir Localização ligada.
5. Em B, toque **Ficar visível** e aceite o pedido do Android.
6. Em A, toque **Buscar**, selecione B e aceite o pareamento nos dois aparelhos. Espere aparecer um vizinho conectado.
7. Em C, busque e conecte a B da mesma forma. B deve mostrar dois vizinhos; A e C, um cada. Não conecte A diretamente a C.
8. Envie uma mensagem inédita em A. C deve recebê-la com **2 saltos**. Responda de C e confira em A.
9. Toque **Parar** em B. Envie outra mensagem inédita em A e confirme que ela não chega a C enquanto a ponte está desligada. As mensagens já recebidas permanecem na tela.
10. Reinicie B e reconecte os dois lados manualmente. Mensagens ainda presentes no cache de A podem ser repassadas. Não há reconexão automática.

Mantenha os apps abertos e visíveis durante o teste. A tela fica acesa enquanto a rede está iniciada. O app não implementa serviço de segundo plano: bloquear a tela, trocar de app, girar a tela ou o sistema encerrar o processo pode interromper a rede. Não faça buscas simultâneas desnecessárias enquanto conversa.

## Limites e privacidade
- Isso é uma rede mesh na camada do aplicativo sobre conexões Bluetooth Classic; não é o padrão Bluetooth Mesh.
- Bluetooth já usa rádio. O app não transforma o celular em transmissor LoRa, rádio FM ou enlace de quilômetros. O alcance depende de paredes, interferência e modelos dos aparelhos.
- Não há acesso automático a celulares de outras pessoas. Os aparelhos participantes precisam do app, permissões e conexão consentida.
- Esta versão é uma sala pública para os participantes conectados. Os intermediários conseguem ler mensagens. O pareamento protege o enlace Bluetooth, mas **não existe criptografia ponta a ponta nem autenticação criptográfica de remetentes**. Apelidos e IDs não comprovam identidade. Não use para informações confidenciais.
- Sem mensagens privadas, confirmação de entrega/leitura, anexos, chamadas ou iPhone.
- A etiqueta de envio significa registro na fila local, não recebimento por outra pessoa. Sem vizinhos, o texto fica temporariamente em cache.
- Histórico e cache são voláteis; fechando o processo, são perdidos. Somente apelido e ID persistem. A tela exibe no máximo 200 mensagens.
- Cache e deduplicação são limitados. Após expiração/evicção, um pacote reenviado pode aparecer novamente; saltos continuam limitados. Não há proteção completa contra spam ou participantes maliciosos.
- Descoberta lista também dispositivos que não têm o app. Eles não vão aceitar o protocolo.
- Quatro conexões é um teto do código, não uma promessa do rádio. Multi-hop precisa de teste em modelos reais.

## Arquitetura
`MainActivity.java`: interface, permissões, lista e ciclo de vida.
`MeshRadio.java`: sockets RFCOMM, aceitação de vizinhos, filas de escrita e fechamento.
`Packet.java`: protocolo binário versionado e validação de tamanho/campos.
`Router.java`: cache limitado, deduplicação e replay.
`tests/ProtocolTest.java`: testes de protocolo e repasse lógico, sem Bluetooth.

Cada socket tem leitura e escrita fora da thread da interface. A descoberta é cancelada antes de conectar. A escuta reabre após aceitar um cliente para permitir outros enlaces. O UUID do serviço deve ser igual em todos os aparelhos. Frames começam por tamanho de 32 bits, seguido de magic/version, UUID da mensagem, UUID do remetente, apelido, texto, horário e saltos. Não se usa desserialização de objetos Java.

## Validação desta entrega
Executados em Java 17: codificação/decodificação Unicode, repasse lógico A → B → C, rejeição de duplicatas/loop, TTL, replay, cache de 500 entradas, rejeição de pacote truncado, grande demais e com bytes extras. Resultado: PASS.

**Não executados:** compilação Android completa, Android Lint, instalação, interface em aparelho e testes de rádio. Este ambiente não tem Android SDK. O workflow inclui compilação e lint para a próxima etapa. Testes unitários do protocolo não comprovam interoperabilidade Bluetooth.

Para executar os testes com JDK instalado:
```sh
bash tests/run.sh
```

## Próximos marcos
1. Compilar e instalar o APK; corrigir eventuais problemas de build/modelos.
2. Validar A–B–C, reconexão e consumo de bateria em aparelhos reais.
3. Serviço em primeiro plano com notificação e reconexão controlada.
4. Identidades autenticadas, criptografia ponta a ponta e confirmação de recebimento.
5. Armazenamento persistente e testes de carga antes de expandir a rede.

## Referências técnicas
- https://developer.android.com/develop/connectivity/bluetooth/connect-bluetooth-devices
- https://developer.android.com/develop/connectivity/bluetooth/bt-permissions
- https://developer.android.com/build/releases/agp-8-7-0-release-notes
